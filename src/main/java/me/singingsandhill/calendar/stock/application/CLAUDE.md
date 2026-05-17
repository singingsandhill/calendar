# Stock Application Layer

> 결정 근거: [`docs/adr/stock/`](../../../../../../../../docs/adr/stock/) — 알고리즘
> (시간/매직넘버 외부화, UniverseBuilder, 진입 검증, TP 비순차화), 인프라 (Semaphore /
> Lock / ThreadPool), 모드 (PAPER/BACKTEST + Clock 빈), 관측성 (TradeEvents).

## 유니버스 (UniverseBuilder)

`pinned` (관심 종목) ∪ `fallback-codes` (안정 후보) 합집합으로 그날 유니버스를
스냅샷으로 캐시. 거래일 변경 시 재빌드. 빈 유니버스 시 `SCREENING_SKIPPED` 이벤트 +
조기 리턴.
[ADR](../../../../../../../../docs/adr/stock/algorithm/0002-universe-builder-snapshot.md).

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

- Trade strength >= configured `entryMinStrength` (null/0 시 FAIL — 데이터 부족 ≠ 통과)
- Order imbalance (bid/ask) >= configured `entryMinImbalance` (orderbook null 시 FAIL)
- Pullback duration: configured min~max minutes

진입 시도(통과/거절) 모두 `EntryAttempt` 도메인으로 영속화 (`stock_entry_attempts`
테이블) — `rejectReason` 라벨링으로 사후 분석.
[ADR](../../../../../../../../docs/adr/stock/algorithm/0003-entry-validation-strictness.md).

## TP1·TP2·TP3 비순차화 (StockRiskService)

TP1·TP2·TP3 는 *독립 트리거* — 선행 의존 X. `checkTakeProfitLevels` 가 TP3 → TP2 → TP1
순서로 평가, 가장 강한 트리거 즉시 발동. `tryFireTp` 헬퍼로 중복 코드 제거.
[ADR](../../../../../../../../docs/adr/stock/algorithm/0004-tp-independent-triggers.md).
