# ADR-0006: 캔들 동기화 실패는 리스크 체크를 게이팅하지 않는다

| 항목 | 값 |
|---|---|
| 상태 | Accepted ([observability/0003](../observability/0003-signal-series-continuity-on-risk-exit.md) 이 범위 밖으로 남긴 후속 ② 를 닫음) |
| 날짜 | 2026-08-08 |
| 도메인 | trading |
| 관심사 | 리스크, 자본 보호 |
| 관련 커밋 | — |

## Context — 무엇이 문제였나

`executeTradeLoop` 은 여섯 단계다: 0 미결 주문 스윕 → 1 캔들 수집 → 2 리스크 체크 →
3 신호 생성 → 4~6 진입·리밸런싱·매매.

0 단계와 3 단계는 각각 `try/catch` 로 감싸여 있고, 0 단계 주석은 **"실패해도 리스크 체크를
막지 않는다"** 라고 명시한다. 그런데 그 사이 **1 단계 `candleService.fetchAndSaveCandles()`
한 줄만 맨몸 호출**이었다. 예외가 나면 바깥 `catch` 로 빠져 `lastError` + `LOOP_ERROR` 를
남기고 메서드가 끝나므로, **그 틱의 손절·익절이 통째로 실행되지 않는다.**

발생원은 가설이 아니라 이미 문서화된 두 가지다.

- Bithumb 캔들 API 장애 — [infrastructure/0004](../infrastructure/0004-reactor-netty-connection-pool-policy.md)
  가 다룬 `PrematureCloseException` 계열이 같은 WebClient 를 탄다.
- 캔들 동기화(5분 cron)와 트레이딩 루프(매분 cron)의 동시 insert 유니크 위반 —
  감사 P2-8. `spring.task.scheduling.pool.size=4` 라 두 잡이 실제로 겹칠 수 있다.

[observability/0003](../observability/0003-signal-series-continuity-on-risk-exit.md) 이
이 항목을 스스로 발견해 Consequences 후속 ② 로 적으면서 **"관측성이 아니라 자본 보호 사안이라
별도 결정으로 다뤄야 한다"** 며 범위 밖으로 남겼다. 이 ADR 이 그 결정이다.

## Decision — 무엇을 골랐나

캔들 수집 실패를 **두 갈래로 나눈다.**

| 단계 | 캔들 실패 시 | 근거 |
|---|---|---|
| 2. 리스크 체크 | **실행한다** | `RiskManagementService` 는 캔들을 전혀 참조하지 않는다 — `bithumbApiClient.getCurrentPrice()` 로 판정한다. 캔들이 낡아도 손절·익절이 오발동하지 않는다 |
| 3. 신호 생성 | 기존 그대로 (이미 `try/catch`) | 시계열 연속성은 observability/0003 의 결정 |
| 4~6. 진입·리밸런싱·매매 | **건너뛴다** | stale 캔들로 계산한 신호가 신규 리스크를 떠안게 하지 않는다 |

구현은 `candlesFresh` 플래그 하나다. `catch` 에서 `false` 로 내리고, 기존
`closeReason != null` 조기 return 바로 아래에 같은 형태의 조기 return 을 추가한다.

경보는 유지한다 — `lastError` 를 그대로 채워 `BotStatus` 로 대시보드에 노출하고, 이벤트
타입만 `LOOP_ERROR` 에서 `CANDLE_SYNC_FAILED` 로 나눈다. 루프가 실제로는 계속 돌았는데
`LOOP_ERROR` 로 적는 것은 사실과 다르기 때문이다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 현행 유지 | 변경 없음 | 자본 보호 경로가 관측성 코드의 실패에 게이팅된다. 우선순위 1(운영 안전) 위반이며 0단계 주석이 선언한 원칙과도 불일치 |
| `try/catch` 로 감싸기만 하고 4~6 도 그대로 진행 | diff 가 가장 작음 | **반대 방향 회귀.** 지금은 캔들 실패 시 틱 전체가 멈춰 매매 판단도 함께 중단되는데, 감싸기만 하면 stale 캔들로 계산한 신호가 신규 진입·리밸런싱을 태운다. 보호를 늘리려다 새 리스크를 만든다 |
| 캔들 실패 시 리스크 체크만 하고 신호 기록도 생략 | 오염된 신호 행을 안 남김 | observability/0003 이 확정한 시계열 연속성 결정을 되돌리게 된다. 그리고 "직전 확정봉까지는 정상"인 경우와 구분하지 못해 결측을 과도하게 만든다 |
| 캔들 수집을 리스크 체크 **뒤로** 이동 | 순서만 바꾸면 됨 | 3단계 신호 생성이 캔들을 입력으로 쓰므로 1→2→3 순서 자체는 의미가 있다. 또 루프 순서 재배치는 `riskCheckRunsBeforeSignalGeneration` 이 `InOrder` 로 못박은 불변식과 얽혀 회귀 표면이 넓다 |
| **두 갈래 분기 (선택)** | 자본 보호는 살리고 신규 리스크는 감수하지 않음 | — |

[risk/0005](0005-fail-safe-entry-guard-on-price-unavailable.md) 와 같은 실패 모드 선택이다 —
**불확실할 때는 돈이 나가지 않는 쪽으로.** 다만 방향이 하나 더 있다: 이미 들어가 있는 돈을
빼는 판단(손절)은 불확실해도 **막지 않는다.** 이 두 방향의 비대칭이 이 결정의 핵심이다.

## Consequences — 영향

- **긍정:** 캔들 API 장애·유니크 위반이 손절·익절을 막지 못한다. 감사 P2-8 이 실현돼도
  자본 보호는 유지된다.
- **긍정:** 캔들 실패가 `CANDLE_SYNC_FAILED` 로 분리 관측된다 — 이전에는 다른 루프 예외와
  섞여 `LOOP_ERROR` 한 종류였다.
- **부정:** 캔들이 실패한 틱은 진입·리밸런싱 기회를 잃는다. 이전에도 같은 틱에서 잃던 것이라
  실질 손실은 없고, 다음 틱(60초)이 곧 재평가한다.
- **부정:** 캔들 실패 틱에도 신호 행이 남는다 — 그 행은 직전 확정봉 기준이라 지표가 한 틱
  낡을 수 있다. `trading_signals` 만 보면 구분되지 않으므로, 분석 시 같은 시각의
  `CANDLE_SYNC_FAILED` 이벤트와 대조해야 한다.
- **후속:** 발생원 자체(감사 P2-8 동시 insert race, P1-2 캔들 동결)는 이 ADR 의 범위가 아니다.
  이 결정은 **증상이 자본 보호로 번지는 것을 끊을 뿐** 원인을 고치지 않는다.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java`
- 관련 테스트: `TradingBotServiceCandleFailureGuardTest` (5종)
- 선행 결정: [observability/0003](../observability/0003-signal-series-continuity-on-risk-exit.md) Consequences 후속 ②
- 같은 계열: [risk/0005](0005-fail-safe-entry-guard-on-price-unavailable.md) — 입력 미확보 시의 실패 모드 선택
- 발생원: `docs/audit/coin-trading-operational-review-2026-07-06.md` P2-8, P1-2
