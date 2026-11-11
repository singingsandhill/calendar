# ADR-0005 (trading/observability): 오프라인 리플레이 하네스 — 운영 DB 사본 + 공개 캔들, 보정 게이트 통과 시에만 대안 비교

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | trading (코인) |
| 관심사 | 관측성 / 분석 |
| 관련 ADR | [strategy/0011 출구 틱 하한·시간청산 모드](../strategy/0011-tick-aware-exits-and-time-exit-modes.md)·[risk/0007 일봉 레짐·리밸런싱](../risk/0007-daily-regime-filter-and-rebalance-guards.md) 의 값 결정 수단, [infrastructure/0005 캔들 보관](../infrastructure/0005-candle-retention-for-analysis.md), [observability/0001 신호 품질 분석](0001-signal-quality-analytics-page.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 6) |

## Context — 무엇이 문제였나

- 작업 4(레짐 필터·리밸런싱)와 5(출구 규칙)는 결론이 안 난 값을 **기본 = 현재 동작** 인 설정 키로만 추가했다. 값을 정할 수단이
  없었다 — `Bot.Mode.BACKTEST` 는 PAPER 와 같아 과거 시세로 돌릴 수 없고, 신호 품질 분석(observability/0001)은 신호의 전방수익만 본다.
- 운영 DB 에 직접 접속하지 않는다(작업 원칙 6). 데이터는 내려받은 H2 사본으로만 쓴다.
- 운영 DB 의 1분봉은 보관 90일(infrastructure/0005, 그 전에는 7일)이라 7월 초 구간이 비어 있을 수 있다. 반면 2026-10-22 실측으로
  빗썸 공개 API 는 1년 전(2025-10-01) 1분봉도 준다(체결 없는 분은 건너뜀) — infrastructure/0005 의 "삭제 구간은 복구할 수 없다" 전제는 과했다.
- 운영 호스트 TZ 는 "추정 UTC, 단정 금지"(서버 이관 런북)라 DB 의 `opened_at` 등 LocalDateTime 이 KST 인지 알 수 없다. 1분봉
  `candle_date_time` 만 빗썸 KST 값이다.

## Decision — 무엇을 골랐나

`src/test/java/.../trading/replay` 의 오프라인 하네스를 `@Tag("replay")` 로 기본 `test` 에서 빼고, Gradle `replay` 태스크로만 돌린다:
`./gradlew replay -Dreplay.db=<scheduledb 사본>` (`-Dreplay.*` 를 테스트 JVM 으로 전달). 보고서는 `docs/data-analysis/coin-exit-replay-<날짜>.md`(A),
`coin-regime-replay-<날짜>.md`(B·C).

- **입력:** DB 사본은 읽기 전용(`ACCESS_MODE_DATA=r;IFEXISTS=TRUE`). DB 시각 - KST 는 1분봉 `created_at - candle_date_time` 중앙값을
  시간 단위로 반올림해 추정(`-Dreplay.db-zone` 으로 지정 가능). PAPER 판별은 백필 SQL 과 같은 규칙(연결 체결 `uuid LIKE 'PAPER-%'`,
  `mode` 컬럼이 있으면 LIVE·NULL 만). 캔들은 빗썸 공개 API 를 `build/replay` 에 캐시(1분봉은 KST 날짜별 CSV, 일봉 `ada-daily.csv`).
- **운영과 같은 부품:** 손익률 `Position.calculateUnrealizedPnlPctWithFee`, 틱 하한 `ExitThresholds`, 호가 `BithumbTickSize`, 레짐
  `RegimeCalculator`. CLOSE 모델(매분 종가 한 번)은 `RiskManagementService.checkPositionRisk` 와 같은 순서이고
  `ExitReplayParityTest`(20 경로)가 운영 서비스와 출구 사유·분을 대조한다 — 깨지면 리플레이로 고른 값이 운영에서 다르게 동작한다.
- **A 출구:** 실제 진입(시각·가격·수량, 2026-07-01~)을 고정하고 분 단위로 다시 판정. HIGH_LOW 모델(봉 고가·저가, 같은 봉의 손절·익절은
  손절 먼저, 봉 고가로 올린 스탑은 다음 봉부터)과 CLOSE 모델. 체결가는 판정 손익률이 임계를 넘는 첫 호가(시가 갭이면 시가). 리스크
  판정 뒤 같은 분의 DB 신호로 신호 매도(`executeTradeBySignal` SELL 분기 — 최소 보유·슬리피지 버퍼·강한 신호 손실 한도), 모델 밖 실제
  출구(리밸런싱·수동)는 그 시각·가격으로. 손익은 모델 수수료 0.04% 양쪽(`replay.fee`), 판정은 운영 taker-fee-rate 0.25%(`replay.decision-fee`).
- **보정 게이트:** 현행 설정(손절 -1.5%·TP +3%·트레일링 0.8%/+1.5%·maxHold 360·BREAKEVEN_CLOSE·틱 하한 없음) 리플레이가 같은
  포지션의 실제 결과(실제 청산가를 모델 수수료로 재계산)를 총손익 ±15%·손절 건수 ±10% 안에서 재현한 가격 모델로만 그리드(트레일링 4 ×
  활성화 2 × 시간청산 모드 3 × maxHold 3 × 틱 하한 2 = 144)를 돌린다. 하나도 통과하지 못하면 원인 후보를 적고 멈춘다. 상위 3 과 함께
  2026-09-01 이전으로 고른 설정의 이후 순위(분할 검증)를 낸다.
- **B 레짐 필터:** 실제 청산 포지션(2026-01-04~)을 진입일 D-1 종가까지의 레짐으로 나누고(미래 데이터 금지) BLOCK/SCALE × UNKNOWN
  정책 5종을 1/4~6/4 와 6/5~ 구간별로. 4-D 판정 = 두 구간 모두 BEAR 진입 기대값 < BULL 이고 필터 적용 시 총손익 증가·MDD 비악화.
- **C 리밸런싱:** 2024-10-01~ 일봉에서 4-C 규칙(엔진 ON)을 하루 한 번 시가 체결로 돌려 KRW 보유·단순 보유와 비교(총수익, MDD,
  총수익÷MDD, 매매 수). 그리드는 ma {10, 20, 60} × hysteresis {0, 2, 5}% × min-days {1, 3}.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 앱의 BACKTEST 모드 구현 | 운영 코드 경로 그대로 | 스케줄러·벽시계·외부 API 를 모두 가짜로 바꿔야 함, 작업 범위 밖 |
| 별도 스크립트(Python 등) | 빠른 실험 | 판정 규칙이 두 벌 — 고른 값이 운영과 다르게 동작. 패리티 테스트도 못 둔다 |
| DB 1분봉만 사용 | 봇이 본 그대로 | 보관 정책 때문에 7월 초가 비어 있을 수 있다. 공개 API 는 같은 원천의 확정봉 |
| **테스트 소스 하네스 + 공용 도메인 함수 + 패리티 테스트 (선택)** | 운영 판정 재사용, CI 는 엔진 단위 테스트만 | 실행에 DB 사본·네트워크 필요(기본 test 에서 제외) |

## Consequences — 영향

- **2026-10-23 실행 결과 (DB 사본 없음):** 6-A·6-B 는 건너뜀(사본을 받으면 위 명령 한 줄). 6-C 는 실행 —
  `docs/data-analysis/coin-regime-replay-2026-10-23.md`. 단순 보유 -42.7%(MDD 87.2%) 구간에서 18개 중 7개 설정이 4-D 문구(KRW 보유·단순
  보유를 총수익÷MDD 로 이김)를 채웠지만 MDD 가 모두 47~62% 다. KRW 보유는 수익·낙폭이 0 이라 "위험조정으로 이긴다" 가 총수익 > 0 과
  같아지는 퇴화 비교다. 기본값(`rebalancing.enabled: false`)은 바꾸지 않았다 — 재활성화는 운영자 결정이고, 그때도 필터와 동시 사용 금지.
- **근사(보고서에 명시):** 신호 쿨다운(전역 30분)·포지션 간 상호작용·봇 정지 구간·전략 재실행(새 진입)은 모델 밖. C 는 하루 한 번
  시가 체결이라 분 단위 운영 엔진과 다르다. B·C 는 뺀 진입이 풀어 준 자본의 파급을 반영하지 않는다.
- **유지 비용:** 운영 출구 판정을 바꾸면 `ExitReplayParityTest` 가 먼저 깨진다 — 엔진을 같이 고친다. 엔진 단위 테스트(8 클래스)는 기본
  `test` 에 포함된다.

## References

- 코드: `src/test/java/me/singingsandhill/calendar/trading/replay/` — `ExitReplayEngine`, `RegimeReplay`, `RebalanceReplay`,
  `ReplayDb`, `BithumbPublicCandles`, `ExitReplayRunner`, `RegimeReplayRunner`, 진입점 `CoinExitReplayTest`·`CoinRegimeReplayTest`
- 빌드: `build.gradle` 의 `test { excludeTags 'replay' }`, `tasks.register('replay', Test)`
- 테스트: `ExitReplayEngineTest`, `ExitReplayParityTest`, `RegimeReplayTest`, `RebalanceReplayTest`, `ReplayStatsTest`, `ReplayDbTest`,
  `BithumbPublicCandlesTest`, `ReplayReportTest`
