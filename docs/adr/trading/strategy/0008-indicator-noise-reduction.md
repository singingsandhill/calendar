# ADR-0008 (trading/strategy): 지표 잡음 감소 — Slow Stochastic + RSI 추세 최소 델타

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 지표 품질 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-5, P2-6) |

## Context — 무엇이 문제였나

1분봉에서 두 지표가 잡음을 그대로 점수화했다.

- **P2-5 Stochastic:** 스코어링(`stochLevel` 25/75, 매수/매도 가드)이 **raw fast %K** 를
  사용. `stochSlow=3` 은 설정에 있으나 **사용되지 않는 dead config**. fast %K 는 1분봉에서
  매우 noisy.
- **P2-6 RSI 추세:** `calculateRsiTrend` 가 **인접봉(1봉) RSI 차이의 부호** 로 ±1(±10점)을
  부여 → 1분봉에서 사실상 동전던지기, 항상 켜진 ±10 이 컨센서스 카운트를 부풀림.

## Decision — 무엇을 골랐나

- **P2-5:** `calculate()` 의 `stochK` 필드(스코어링 입력)를 **slow %K** (fast %K 의
  `stochSlow` SMA = `calculateStochasticD(candles, stochK, stochSlow)`)로 전환.
  dead config `stochSlow` 활용 + 평활.
- **P2-6:** `calculateRsiTrend` 를 `rsiTrendLookback`(3)봉 전 RSI 와 비교하도록 변경하고,
  **|delta| > `minRsiTrendDelta`(2점)** 일 때만 ±1, 그 외 0. 순수 결정은 `rsiTrend(current,
  past)` 로 분리(단위 테스트).
- 전부 설정값. 스코어 **가중치(±15 등)는 불변** — 어떤 값이 점수를 트리거하는지만 정련.

## Consequences — 영향

- **긍정:** stochLevel·rsiTrend 점수의 1분봉 잡음 감소 → 거짓 신호·항상-켜진 추세 점수
  축소. `stochSlow` dead config 해소.
- **주의:** slow %K 는 fast 보다 둔감 → 25/75 도달 빈도 변화. RSI 추세는 미세 변화에 0 →
  컨센서스 카운트 변화. **신호 빈도가 달라지므로 LIVE 전 PAPER 백테스트 권장**.
- 기본 LIVE 이므로 검증 후 적용.

## References

- 코드: `application/service/IndicatorService.java` (`calculate` stochK, `rsiTrend`,
  `calculateRsiTrend`), `TradingProperties.Indicators`(stochSlow/rsiTrendLookback/minRsiTrendDelta)
- 테스트: `IndicatorServiceSlowStochTest`, `IndicatorServiceRsiTrendTest`
