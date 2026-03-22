# Stock Application Layer

## Screening Filters

1. Gap ratio: 2-7%
2. Market cap >= configured minimum
3. Trade value >= configured minimum
4. Trade strength >= 110
5. Spread <= configured maximum

## Entry Validation (PullbackDetectionService)

- Trade strength >= 105
- Order imbalance (bid/ask) > 1.2
- Pullback duration: 3-15 minutes
