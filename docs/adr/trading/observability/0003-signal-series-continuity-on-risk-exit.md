# ADR-0003: 리스크 청산 틱에도 신호를 기록한다 — 관측성은 주문 경로를 게이팅하지 않는다

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-06 |
| 도메인 | trading |
| 관심사 | 관측성 |
| 관련 커밋 | (배치 커밋 시 기입) |
| 관련 ADR | [trading/observability/0001](0001-signal-quality-analytics-page.md), [trading/risk/0001](../risk/0001-circuit-breaker-daily-and-consecutive-loss.md) |

## Context — 무엇이 문제였나

`TradingBotService.executeTradeLoop()` 의 단계 순서는 이랬다.

```
0. 미결 주문 스윕   (try/catch 로 감쌈 — "실패해도 리스크 체크를 막지 않는다")
1. 캔들 갱신
2. 리스크 체크 (손절/익절)  → CloseReason 이 있으면 여기서 return
3. 신호 생성                 ← 2에서 return 하면 도달하지 않음
4~6. 강신호 매매 / 리밸런싱 / 신호 매매
```

즉 손절·익절·트레일링이 발동한 분에는 `trading_signals` 행이 **통째로 없었다.** 분석에 가장
정보량이 큰 순간 — 청산 직전의 지표 상태 — 이 1분 관측 시계열에서 정확히 빠져 있었다는 뜻이다.
[observability/0001](0001-signal-quality-analytics-page.md) 의 전방수익·구간 통계는 이 시계열이
균일하다는 전제 위에 서 있으므로, 구멍이 체계적으로 특정 사건과 겹치는 것은 단순 결측보다 나쁘다.

## Decision — 무엇을 골랐나

리스크 체크는 그대로 1순위로 두고, 조기 return 을 "신호는 기록하되 매매 단계는 건너뛴다" 로 바꾼다.

- `checkAndExecuteRiskRules` 호출 위치는 **그대로**. 신호 생성은 언제나 그 뒤다.
- 청산이 일어났으면 신호를 기록한 **뒤에** return 한다 → 단계 4~6 억제 효과는 기존과 동일.
- 신호 생성은 `try/catch` 로 감싼다. 예외를 삼키는 이유는 **없던 운영 경보를 만들지 않기 위해서**다 —
  삼키지 않으면 바깥 catch 가 `lastError` 와 `LOOP_ERROR` 이벤트를 남기는데, 이전에는 이 경로에서
  신호 생성 자체를 하지 않았으므로 그런 경보가 존재할 수 없었다.
- 신호 생성이 null 을 돌려주면(캔들 없음) **합성 행을 만들지 않는다.** 그 분은 진짜 결측이고,
  커버리지 패널이 센다.

행동 변화는 **"청산 틱에 `trading_signals` 행 1개가 더 생긴다"** 뿐이다. 매수·매도·리밸런싱
판단은 이전과 완전히 동일하게 억제된다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 신호 생성을 리스크 체크 **앞으로** 옮긴다 | 가장 단순한 diff | **안전 불변식 역전.** `generateSignal` 은 캔들 DB 조회 + `DivergenceService.detect` + `calculatePreviousMAs` 를 한다. 여기서 예외가 나거나 쿼리가 느리면 그 틱의 손절이 지연되거나 아예 실행되지 않는다. 관측성이 자본 보호를 게이팅해선 안 된다 |
| `RiskManagementService` 가 신호 행을 쓴다 | 루프를 안 건드림 | 리스크 서비스에 `SignalService` 의존을 새로 만들고, 관측 목적으로 리스크 애그리거트 안에서 신호 도메인을 쓰게 된다 |
| 청산 틱에 `RISK_EXIT` 합성 행을 넣는다 | 시계열이 빈틈없어 보인다 | 점수를 계산한 적 없는 행이 섞인다. 전방수익·국면 계산이 기대는 "균일 관측 시계열" 전제를 깨뜨린다 |
| 그냥 두고 분석 시점에 캔들로 복원한다 | 코드 변경 0 | 캔들은 정리되고(90일), 복원하려면 **그 순간의 설정**으로 다이버전스를 다시 돌려야 하는데 파라미터가 바뀐 뒤에는 재현 불가능 |
| **리스크 체크 유지 + 기록 후 return (선택)** | 불변식 보존, diff 최소, 행동 변화는 행 1개 | 루프에 try/catch 가 하나 늘어난다 |

**불변식을 테스트로 못박는다.** `TradingBotServiceLoopSignalRecordingTest` 의
`riskCheckRunsBeforeSignalGeneration` 이 `InOrder` 로 순서를 고정하므로, 나중에 누가 신호 생성을
위로 올리면 빌드가 깨진다. `riskExitTick_skipsRebalanceAndTrade` 는 단계 4~6 이 여전히
억제되는지를 `never()` 로 지킨다.

## Consequences — 영향

- **긍정:** 1분 관측 시계열의 구멍이 사건과 상관되지 않게 됐다. 청산 직전 지표 상태가 남아
  "무엇을 보고 손절까지 갔는가" 를 사후에 볼 수 있다.
- **부정:** 루프에 예외를 삼키는 지점이 하나 생겼다(ERROR 로그는 남는다). 청산 틱의
  `signal_time` 은 정상 틱보다 최대 3초쯤 늦게 찍힌다 — `closePosition` 이
  `extractExecutedPriceWithRetry` 에서 500+1000+1500ms 를 쓸 수 있기 때문. 90초 조인 창에는
  영향이 없지만, 분석의 결측 판정은 정각이 아니라 허용오차 기반이어야 한다(그렇게 돼 있다).
- **후속:** 남은 구조적 공백 두 가지는 이번 범위 밖이다 — ① 봇 정지·일시정지 구간,
  ② `executeTradeLoop` 의 `candleService.fetchAndSaveCandles()` 가 try/catch 없이 호출돼
  **캔들 조회 실패가 그 틱의 리스크 체크까지 통째로 건너뛰게 만드는 문제**. ②는 관측성이 아니라
  자본 보호 사안이며 별도 결정으로 다뤄야 한다.
  → **②는 [risk/0006](../risk/0006-candle-sync-failure-does-not-gate-risk-check.md) 으로 닫혔다**
  (2026-08-08). 캔들 실패 시 리스크 체크는 실행하고 4~6단계만 억제한다. 그 결과 이 ADR 이 만든
  신호 행에 한 가지 성격이 추가됐다 — 캔들 실패 틱의 행은 직전 확정봉 기준이라 지표가 한 틱
  낡을 수 있고, `trading_signals` 만으로는 구분되지 않으므로 같은 시각의 `CANDLE_SYNC_FAILED`
  이벤트와 대조해야 한다. ① 은 여전히 미해결이다.

## References

- 관련 코드: `src/main/java/.../trading/application/service/TradingBotService.java` (`executeTradeLoop` 단계 2~3)
- 관련 테스트: `src/test/java/.../trading/application/service/TradingBotServiceLoopSignalRecordingTest.java`
