# Trading Module

Crypto algorithmic trading bot for Bithumb. Uses MA/RSI/Stochastic with divergence detection.

> 결정 근거: [`docs/adr/trading/strategy/`](../../../../../../../docs/adr/trading/strategy/) —
> 8지표 컨센서스(±135), MA 수렴 억제, 적자 매매 방지 가드.

## Trading Flow

```
Bithumb API -> Candles -> Indicators -> Divergences -> Signals -> Trade Execution
                                                           |
                                      Risk Management <- Position Tracking
```

## Risk Levels

- Stop-loss: -3%
- Take-profit: +15%
- Trailing stop: activates at +10%, trails -3%
- Fee rate: 0.25% (taker), min profit threshold: 0.6%

## Anti-Whipsaw (휩소 방지)

- Signal cooldown: 10 minutes between trades
- Min holding: 15 minutes before sell allowed
- MA convergence suppression: |MA5-MA20|/MA20 < 0.2% → MA cross score = 0
- Min agreeing indicators: 3 (out of 8 score components)

## Rebalancing

- Bullish (price > MA60): 70% coins / 30% KRW
- Bearish (price < MA60): 30% coins / 70% KRW
- Trigger: 10% deviation from target
- Cooldown: 8 hours between rebalances
- Min sell PnL: +3% (only rebalances by selling when position profit >= 3%)
- Safety details: see `application/CLAUDE.md`

## ATR-based Dynamic Order Ratio

- High volatility (ATR >= 3%): 15% order
- Low volatility (ATR <= 1%): 35% order
- Mid: linear interpolation
