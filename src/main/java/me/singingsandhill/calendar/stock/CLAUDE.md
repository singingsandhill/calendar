# Stock Module

Gap & Pullback trading bot for Korean stocks via Korea Investment Securities API.

## Trading Flow

```
08:30  PreMarket    -> Collect prev day data
09:20  Screening    -> Floor filter + composite score ranking (top N) + email alert
09:20  Trading      -> Every 5 seconds: risk check -> state update -> enter if ready
11:20  Final Exit   -> Force close all remaining positions
```

## State Machine

```
WATCHING      -> Price >= Open x 1.015         -> HIGH_FORMED
HIGH_FORMED   -> Price <= High x 0.985         -> PULLBACK
PULLBACK      -> Price >= PullbackLow x 1.003  -> ENTRY_READY
ENTRY_READY   -> Buy order executed             -> ENTERED
ENTERED       -> All exits completed            -> EXITED

PULLBACK      -> Price < High x 0.970          -> FILTERED_OUT (too deep)
```

## Exit Rules

| Type | Condition | Action |
|------|-----------|--------|
| Stop Loss | Price <= Entry x 0.985 (-1.5%) | Sell 100% |
| TP1 | Price >= Entry x 1.015 (+1.5%) | Sell 50% (fee-adjusted profit check) |
| TP2 | Price >= DayHigh | Sell 60% remaining (fee-adjusted profit check) |
| TP3 | Price >= DayHigh x 1.01 (+1% above day high) | Sell remaining (fee-adjusted profit check) |
| Trailing | Price <= TrailingHigh x 0.992 (-0.8%) | Sell remaining |
| Time Exit | Time >= 11:20 | Sell 100% |

Time-decay take profit: minimum profit threshold decreases linearly from 0.5% (09:10) to 0.1% (15:15), making TP triggers easier to hit later in the session.
