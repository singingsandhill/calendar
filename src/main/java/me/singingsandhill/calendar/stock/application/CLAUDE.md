# Stock Application Layer

> 결정 근거: [`docs/adr/stock/`](../../../../../../../../docs/adr/stock/) — 알고리즘
> (시간/매직넘버 외부화, UniverseBuilder, 진입 검증, TP 비순차화), 인프라 (Semaphore /
> Lock / ThreadPool), 모드 (PAPER/BACKTEST + Clock 빈), 관측성 (TradeEvents).

## 유니버스 (UniverseBuilder)

`pinned` (관심 종목) ∪ **KIS 거래량순위 상위 N** (`rank-api-top`, FHPST01710000) 으로 그날
유니버스를 스냅샷 캐시. 거래량순위가 실패/0건이면 정적 `fallback-codes` 로 폴백(무회귀).
pre-market(08:30) 에 초기 빌드하되, **08:30 엔 당일 거래량이 없어 rank 가 0건이므로
스크리닝(09:20) 시점에 `refreshIfDegraded()` 가 rank=0 스냅샷을 감지해 1회 재시도**
([ADR-0006](../../../../../../../../docs/adr/stock/algorithm/0006-universe-rank-retry-at-screening.md)).
rank 성공 스냅샷은 그대로 유지(거래일 1회 스냅샷 정합성). 빈 유니버스 시 `SCREENING_SKIPPED`
이벤트 + 조기 리턴.
[ADR-0002](../../../../../../../../docs/adr/stock/algorithm/0002-universe-builder-snapshot.md)
(스냅샷) ·
[ADR-0005](../../../../../../../../docs/adr/stock/algorithm/0005-dynamic-universe-volume-rank.md)
(거래량순위 동적 소스).

## Screening (ScreeningService)

**Score-based mode** (default, `scoring.enabled=true`):

1. Floor filters (hard cut): min gap%, max gap 15%, min trade strength, min market cap 500억
0. **거래 가능 상태 가드** — VI/거래정지/관리/정리매매/투자경고 배제 (`isTradable()`)
1'. **선정 규칙** — 총점 ≥ `min-score-threshold`(40) **AND** 신호 점수(갭+체결강도) ≥
   `signal-min-score`(25). `min-candidates` 강제 선정은 제거 — 조건 미달이면 0건이 정상
   ([ADR 0008](../../../../../../../../docs/adr/stock/algorithm/0008-screening-selection-and-cost-model.md)).
2. Composite score = weighted sum of 5 normalized factors:
   - Gap score (bell curve, center=4%, sigma=3)
   - Strength score (linear, 95~130)
   - Trade value score (log scale, 5억~500억)
   - Spread score (inverse, 0~0.5%)
   - Market cap score (log scale, 500억~10조)
3. Sort by score descending → 위 선정 규칙 통과분만 최대 `max-watchlist-size` 까지

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
