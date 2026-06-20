# ADR-0009 (trading/strategy): 형성 중 현재봉 제외 (룩어헤드/리페인트 방지) — 기본 OFF

| 항목 | 값 |
|---|---|
| 상태 | Accepted (기본 비활성) |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 지표 품질 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-2) |

## Context — 무엇이 문제였나

`CandleScheduler` 는 "캔들 완성 후" 매분 :05초에 트레이딩 루프를 실행한다(주석 명시). 그러나
Bithumb 분봉 API 는 **newest-first** 이고, :05초 시점의 index 0 은 *현재 형성 중인 분봉*(약
5초치 데이터)일 가능성이 크다. 그렇다면 `IndicatorService.calculate` 와 `DivergenceService`
가 **미완성 봉의 OHLC** 로 MA/RSI/Stoch/다이버전스를 계산 → 잡음·리페인트(봉이 형성되며 값
변동) 위험.

**불확실성:** Bithumb 이 index 0 을 *형성봉* 으로 주는지, 아니면 :05초엔 이미 *직전 종료봉* 을
주는지 코드만으로 확정 불가. 만약 index 0 이 이미 종료봉이라면, 이를 제외하면 지표가 **1봉
지연**(오히려 악화)된다. 감사도 이 항목을 medium/일부 strawman 으로 강등.

## Decision — 무엇을 골랐나

메커니즘을 제공하되 **기본 비활성**(현 동작 보존).

- `Indicators.excludeFormingCandle`(기본 **false**). true 시 `calculate()` 와
  `DivergenceService.detect()` 가 **index 0 을 제외**하고 종료봉(`subList(1, …)`)으로 모든
  지표를 계산.
- `currentPrice` 는 **index 0 tradePrice(라이브)** 유지 — 신호의 가격 기준/실행가 참조용.
- `currentVolume` 은 제외 시 직전 종료봉 거래량 사용(형성봉의 부분 거래량 과소반영 회피).

## Rationale — 왜 기본 OFF 인가

index 0 의 의미가 환경(Bithumb 응답 타이밍·코인 유동성)에 따라 다를 수 있어, 검증 없는 ON 은
1봉 지연 리스크. "현 동작 보존 + 옵트인" 이 안전.

## 검증 절차 (ON 전)

1. 운영 로그에서 `calculate` 진입 시 `candles.get(0).getCandleDateTime()` 이 **현재 분과
   동일**(=형성봉)인지 확인. 동일하면 형성봉 → 제외가 옳음.
2. `excludeFormingCandle=true` + `TRADING_BOT_MODE=PAPER` 로 백테스트해 신호 빈도/품질 비교.
3. 양호하면 LIVE ON.

## Consequences — 영향

- **ON:** 미완성 봉 제외 → 봉 단위 결정성↑, 리페인트↓.
- **OFF(기본):** 현 동작 무변 — 회귀 없음.

## References

- 코드: `IndicatorService.calculate`, `DivergenceService.detect`,
  `TradingProperties.Indicators.excludeFormingCandle`
- 테스트: `IndicatorServiceFormingCandleTest`
