# ADR-0001: Bot.Mode {LIVE, PAPER, BACKTEST} + Clock 빈(Asia/Seoul)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 모드 / 테스트 가능성 |
| 관련 커밋 | `docs/git_commit.md` Commit 6 (PR-6) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

두 가지 분리된 통증이 같은 PR 에서 풀어야 했다.

1. **백테스트/페이퍼 트레이딩이 불가능** — `KoreaInvestmentApiClient` 가 모든 주문을
   실제 KIS 호출로 연결. 알고리즘 변경을 검증하려면 실거래 자금이 필요.
2. **시간 의존 코드의 테스트 어려움** — `StockRiskService.calculateTimeDecayThreshold`
   가 `LocalTime.now()` 직접 호출. 09:10 / 12:13 / 15:15 시점별 다른 임계값을 검증하려면
   `Mockito.mockStatic(LocalTime.class)` 또는 시계 조작 — 둘 다 깨지기 쉬움.

## Decision — 무엇을 골랐나

봇 모드 enum 도입과 시계 추상화.

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — `KoreaInvestmentApiClient` 의 모든 주문
  진입부에 모드 가드. PAPER/BACKTEST 는 `simulateOrder()` 로 인메모리 체결.
- **`KisOrderResponse.simulated()`** 팩토리 — 시뮬레이션 응답 생성 (체결가/수량/시각
  주입).
- **`Clock` 빈 (Asia/Seoul)** — Spring 빈으로 등록. `StockRiskService` 등 시간 의존
  코드는 `LocalTime.now(clock)` 사용.
- **단위 테스트 4종 / 20 케이스 추가:**
  - `StockStateMachineTest` — WATCHING/HIGH_FORMED/PULLBACK 전이.
  - `StockPositionTakeProfitTest` — TP1·TP2·TP3 독립 트리거 + 손절.
  - `StockRiskServiceTimeDecayTest` — 09:10 / 12:13 / 15:15 / 09:00 임계값을 `Clock.fixed`
    로 결정성 검증.
  - `UniverseBuilderTest` — 합집합 / 중복 제거 / 캐싱 / 거래일 변경 재빌드.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 별도 Backtest 모듈 분리 | 격리 강함 | 코드 두 벌 유지 — 알고리즘 변경 시 양쪽 동시 갱신 |
| KIS 모의투자 서버 호출 | 외부 신호 충실 | 시뮬레이션 시점 빠르게 반복 불가, 비용/네트워크 부담 |
| `LocalTime.now()` 직접 호출 + 테스트 우회 | 단순 | 시간 의존 테스트 깨지기 쉬움, mockStatic 의존 |
| **(선택) 모드 enum + Clock 빈** | 운영 코드 한 벌, 결정성 테스트 가능 | — |

`Clock.fixed(Instant, ZoneId)` 는 표준 라이브러리. 모의 라이브러리 의존 없이 시간을
고정할 수 있어 가장 단순한 선택.

## Consequences — 영향

- **긍정:**
  - PAPER 모드로 자금 없이 알고리즘 검증.
  - BACKTEST 모드로 과거 데이터 시뮬레이션 (별도 데이터 fixture 필요).
  - 시간 의존 코드의 단위 테스트가 결정성 가짐.
- **부정:**
  - 신규 시간 의존 코드 작성 시 `LocalTime.now()` 직접 호출 금지 — `Clock` 주입 강제.
    리뷰 가드 필요.
  - PAPER/BACKTEST 분기는 `KoreaInvestmentApiClient` 진입부에 모두 추가 — 새 주문
    유형 추가 시 모드 가드 누락 위험.
- **후속:**
  - ADR-0003 (ThreadPoolTaskScheduler) 와 같은 빈 컨텍스트에서 Clock 빈도 함께
    등록되어 일관된 시간 사용.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KoreaInvestmentApiClient.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisOrderResponse.java`
  - `src/test/java/me/singingsandhill/calendar/stock/domain/StockStateMachineTest.java`
  - `src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionTakeProfitTest.java`
  - `src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceTimeDecayTest.java`
  - `src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java`
- 관련 docs: `docs/stock-bot.md` (시간 감소 임계값)
- 관련 커밋: `docs/git_commit.md` Commit 6
