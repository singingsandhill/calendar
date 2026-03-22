# Stock Module

Gap & Pullback trading bot for Korean stocks via Korea Investment Securities API.

## Trading Flow

```
08:30  PreMarket    -> Collect prev day data
09:00  Screening    -> Find 2-7% gap stocks (max 10)
09:10  Trading      -> Every 5 seconds: risk check -> state update -> enter if ready
11:20  Final Exit   -> Close all remaining positions
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
| Stop Loss | Price <= Entry x 0.985 | Sell 100% |
| TP1 | Price >= Entry x 1.015 | Sell 50% |
| TP2 | Price >= High | Sell 60% remaining |
| TP3 | Price >= High x 1.01 | Sell remaining |
| Trailing | Price <= TrailingHigh x 0.992 | Sell remaining |
| Time Exit | Time >= 11:20 | Sell 100% |
