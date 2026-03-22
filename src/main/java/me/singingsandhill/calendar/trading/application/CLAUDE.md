# Trading Application Layer

## Signal Logic

- BUY: score >= 40 AND RSI < 70 AND StochK < 85
- SELL: score <= -40 AND RSI > 30 AND StochK > 15
- Otherwise: HOLD

## Score Components (weights)

MA Cross: +/-25 | MA Trend: +/-15 | RSI Divergence: +/-20 | RSI Level: +/-15 | Stoch Divergence: +/-15 | Stoch Level: +/-15 | Volume Divergence: +/-20 | RSI Trend: +/-10

## Divergence Types

- Bullish: Price Lower Low + Indicator Higher Low
- Bearish: Price Higher High + Indicator Lower High
- Detects on: RSI, Stochastic, Volume

## Rebalancing Safety

- Cooldown: 4h between rebalances
- Min order: 5,000 KRW (skip smaller)
- Slippage: 0.5% buffer on market orders
- MA60 data insufficient: skip rebalancing
