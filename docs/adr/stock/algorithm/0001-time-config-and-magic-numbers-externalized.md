# ADR-0001: tradingLoopStart 분리 / 휴일 차단 / 매직넘버 Scoring config 외부화

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 설정 |
| 관련 커밋 | `5a5be58` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

세 가지 결함이 한 PR 로 묶여 있었다.

1. **`screeningEnd` 의 의미 모호** — 9:20 스크리닝 종료 시각인지, 트레이딩 루프 진입
   가능 시각인지 불분명. 실제로 두 가지 다른 결정에서 동일 키를 읽고 있어 한 쪽을
   바꾸면 다른 쪽이 깨질 위험.
2. **휴일 무시** — `StockTradingScheduler` 가 평일 cron 만 보고, 한국 공휴일에도
   루프가 돌아 KIS API 를 호출. KIS 는 휴일 데이터를 부정확하게 응답.
3. **6개 매직 상수가 `ScreeningService` 에 하드코딩** — `GAP_CENTER=4.0`, `GAP_SIGMA=3.0`,
   `STRENGTH_MIN/MAX`, `FLOOR_MAX_GAP`, `FLOOR_MIN_MARKET_CAP`. 운영 중 바꾸려면
   재배포 필요. 또한 정규화에 사용되는 트레이드밸류·시총·스프레드 범위도 코드에 박혀
   있어 백테스트 시 동일 계수를 다르게 적용 불가능.

## Decision — 무엇을 골랐나

시간 설정과 운영 휴일을 분리하고, 알고리즘 계수를 `Scoring` config 으로 끌어올림.

- **`Trading.tradingLoopStart`** 신규 도입 — 트레이딩 루프 가드 전용. 기존
  `screeningEnd` 는 의미 모호로 더 이상 신규 코드에서 참조하지 않고 호환용으로만 보존.
- **`Trading.holidays`** (yyyy-MM-dd 리스트) + `isHoliday()` 헬퍼 → 주말 + 공휴일
  거래 차단. 공휴일은 매년 yaml 에 추가.
- **`Scoring` config 외부화** — 6개 매직 상수와 정규화 범위를 `application.yaml`
  의 `stock.scoring.*` 으로 이동. 백테스트/운영에서 다른 값 사용 가능.
- `application.yaml`: `trading-loop-start`, `holidays` 키 추가.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `screeningEnd` 의미 재정의 | 키 추가 없음 | 기존 로그/메트릭/문서가 옛 의미를 가정 — 무성격 회귀 위험 |
| 공휴일 캘린더 API 연동 | 자동 갱신 | 외부 의존성, 캐시·실패 시 동작 결정 추가 부담. 한국 공휴일은 연 1회 수동 갱신으로 충분 |
| `@Value` 로 6개 키 주입 | 간단 | 6개 매직 상수가 사실은 *같은 알고리즘 단위* — config 클래스로 묶는 편이 유지보수 비용 낮음 |
| **(선택) 새 키 + holidays 리스트 + Scoring config 클래스** | 의미 충돌 0, 운영 토글 가능 | — |

## Consequences — 영향

- **긍정:**
  - 백테스트와 라이브의 계수 분리 가능 → ADR-0001 (PAPER/BACKTEST 모드) 의 결정성
    검증을 보강.
  - 휴일 거래로 인한 KIS 부정확 응답 차단.
  - "9:20 인데 왜 루프가 안 돌지?" 같은 운영 혼란 사라짐.
- **부정:**
  - 매년 1회 `holidays` 갱신 필요 (자동화 미구현).
  - `screeningEnd` 가 deprecated 상태로 남아 신규 진입자가 혼동 가능 — 코드에 주석.
- **후속:**
  - ADR-0002 (UniverseBuilder) 가 `tradingLoopStart` 시각의 명확함을 전제로 pre-market
    08:30 빌드 결정.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/scheduler/StockTradingScheduler.java`
  - `src/main/resources/application.yaml`
- 관련 docs: `docs/stock-bot.md` (시간/구간 정책)
- 관련 커밋: `git log -1 5a5be58`
