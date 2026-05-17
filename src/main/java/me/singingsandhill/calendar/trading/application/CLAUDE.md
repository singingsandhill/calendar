# Trading Application Layer

> 결정 근거: [`docs/adr/trading/strategy/`](../../../../../../../../docs/adr/trading/strategy/).

## Signal Logic

- BUY: score >= 40 AND RSI < 70 AND StochK < 85 AND 3+ indicators agree
- SELL: score <= -40 AND RSI > 30 AND StochK > 15 AND 3+ indicators agree
- Otherwise: HOLD
- MA convergence (|MA5-MA20|/MA20 < 0.2%): MA cross score suppressed to 0

## Score Components (weights, sum = ±135)

MA Cross: ±25 (이벤트) 또는 MA State ±10 (둘 중 하나만) | MA Trend: ±15 | RSI Divergence: ±20 | RSI Level: ±15 | Stoch Divergence: ±15 | Stoch Level: ±15 | Volume Divergence: ±20 | RSI Trend: ±10

총 합산 범위: **±135** = 25+15+20+15+15+15+20+10 ([ADR](../../../../../../../../docs/adr/trading/strategy/0001-multi-indicator-consensus.md)).
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
- 리밸런싱 매도는 평균 P/L ≥ 0% 일 때만 실행 (적자 청산 방지) — [ADR](../../../../../../../../docs/adr/trading/strategy/0003-loss-prevention-guards.md).
- 강한 신호 매도는 평가손익 ≥ -2% 일 때만 (작은 손실 시 강한 신호로 매도 안 함).
- 트레일링 스탑은 진입가 + 왕복 수수료(0.5%) *아래로* 내려가지 않게 floor.
