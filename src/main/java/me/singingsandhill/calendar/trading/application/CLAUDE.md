# Trading Application Layer

> 결정 근거: [`docs/adr/trading/strategy/`](../../../../../../../../docs/adr/trading/strategy/).

## Signal Logic

- BUY: score >= 40 AND RSI < 70 AND StochK < 85 AND 3+ indicators agree
- SELL: score <= -40 AND RSI > 30 AND StochK > 15 AND 3+ indicators agree
- Otherwise: HOLD
- MA convergence (|MA5-MA20|/MA20 < 0.2%): MA cross score suppressed to 0

## Score Components (weights, sum = ±128)

MA Cross: ±25 (이벤트) 또는 MA State ±5 (둘 중 하나만) | MA Trend: ±8 | RSI Divergence: ±20 | RSI Level: ±15 | Stoch Divergence: ±15 | Stoch Level: ±15 | Volume Divergence: ±20 | RSI Trend: ±10

총 합산 범위: **±128** = 25+8+20+15+15+15+20+10 ([ADR-0001](../../../../../../../../docs/adr/trading/strategy/0001-multi-indicator-consensus.md), 모멘텀 가중 하향 [ADR-0010](../../../../../../../../docs/adr/trading/strategy/0010-momentum-weight-reduction.md): MA Trend ±15→±8, MA State ±10→±5).
횡보장에서 MA 수렴 시(|MA5-MA20|/MA20 < 0.2%) 크로스 점수는 0 으로 억제됨
([ADR](../../../../../../../../docs/adr/trading/strategy/0002-ma-convergence-suppression.md)).

## Divergence Types

- Bullish: Price Lower Low + Indicator Higher Low
- Bearish: Price Higher High + Indicator Lower High
- Detects on: RSI, Stochastic, Volume

## Rebalancing Safety

- Cooldown: 8h between rebalances
- Min order: 5,000 KRW (skip smaller)
- Slippage: 0.5% buffer on market orders
- MA60 data insufficient: skip rebalancing
- **회계 정합 (P1-3):** 리밸런스 매수 → 추적 `Position`(SL/TP) 생성; 매도 → OPEN 포지션
  FIFO 청산, 포지션별 수수료차감 PnL ≥ `min-sell-pnl-pct`(0%) 인 것만, 목표량 도달 시 중단.
  청산은 `RiskManagementService.closePosition(…, REBALANCE)` 재사용. 추적 Position ↔ 실잔고
  정합 + 전 코인 SL/TP 보호 — [ADR risk/0002](../../../../../../../../docs/adr/trading/risk/0002-rebalance-position-accounting.md). ADR-0003 의 적자 청산 방지 의도를 포지션별로 정련.
- 강한 신호 매도는 평가손익 ≥ -2% 일 때만 (작은 손실 시 강한 신호로 매도 안 함).
- 트레일링 스탑은 진입가 + 왕복 수수료(0.5%) *아래로* 내려가지 않게 floor.

## Circuit Breaker (P0-2)

`TradingCircuitBreaker` — 연속 손실 `maxConsecutiveLosses`(기본 3) 또는 당일 실현손익
≤ `maxDailyLossPct`(기본 -5%) 시 신규 BUY 차단(리스크 청산은 허용). 청산 결과는
`executeSell`/`closePosition` 에서 집계, 진입 차단은 `executeBuy` 진입부에서 판정.
근거: [ADR](../../../../../../../../docs/adr/trading/risk/0001-circuit-breaker-daily-and-consecutive-loss.md).
