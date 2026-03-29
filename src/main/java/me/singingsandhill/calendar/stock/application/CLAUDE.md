# Stock Application Layer

## Screening (ScreeningService)

**Score-based mode** (default, `scoring.enabled=true`):

1. Floor filters (hard cut): min gap%, max gap 15%, min trade strength, min market cap 500억
2. Composite score = weighted sum of 5 normalized factors:
   - Gap score (bell curve, center=4%, sigma=3)
   - Strength score (linear, 95~130)
   - Trade value score (log scale, 5억~500억)
   - Spread score (inverse, 0~0.5%)
   - Market cap score (log scale, 500억~10조)
3. Sort by score descending → select top N (minCandidates guaranteed)

**Legacy mode** (`scoring.enabled=false`): sequential hard-cut filters (gap, market cap, trade value, strength, spread).

## Entry Validation (PullbackDetectionService)

3 conditions checked; **soft validation** (`softEntryValidation=true`): 2/3 sufficient.

- Trade strength >= configured `entryMinStrength`
- Order imbalance (bid/ask) >= configured `entryMinImbalance`
- Pullback duration: configured min~max minutes
