# ADR-0006 (trading/strategy): RSI 를 Wilder 평활(표준)로 전환

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 지표 품질 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-4) |

## Context — 무엇이 문제였나

`IndicatorService.calculateRSI` 가 **단순평균(Cutler) RSI** 를 사용했다 — 매 호출마다
*첫 period 개 변화의 단순평균* 만으로 RS 계산. 표준 RSI(Wilder)와 달리 과거 평균을
평활(smoothing)하지 않아:

- 더 **노이지**(특히 1분봉), 동일 데이터에서 표준 RSI 와 값이 다름.
- RSI 레벨(35/65)·RSI 다이버전스·RSI 추세 점수가 모두 이 비표준 값에 의존.

## Decision — 무엇을 골랐나

Wilder 평활 RSI 채택(표준).

- **시드:** 가장 오래된 `period` 개 변화의 단순평균으로 avgGain/avgLoss 초기화.
- **평활:** 이후(더 최신) 각 변화에 대해 `avg = (avg*(period-1) + x) / period` 재귀 적용.
- 캔들 DESC(최신순) 인덱싱에 맞춰 시간순(오래된→최신)으로 진행.
- 시그니처(`calculateRSI(candles, period)`)·호출부 불변. period+1 캔들이면 시드만
  (평활 0회) = 경계에서 기존과 동일.

## Consequences — 영향

- **긍정:** 표준 RSI 로 노이즈 감소, 다른 도구/문헌과 일치(해석성·재현성 ↑).
- **주의:** Wilder RSI 는 더 평활(덜 극단적) → 35/65 경계 도달 빈도가 달라져 **RSI 기반
  신호 빈도가 이동**. 임계(oversold 35/overbought 65)는 단순 RSI 기준으로 튜닝됐으므로
  **재튜닝 여지**. 기본 LIVE 이므로 **PAPER 백테스트로 검증 후 확정 권장**.
- `calculateRsiSeries`(다이버전스)·`calculateRsiTrend` 도 자동으로 Wilder 값 사용.

## References

- 코드: `application/service/IndicatorService.java` (`calculateRSI`)
- 테스트: `IndicatorServiceWilderRsiTest` (all-gain→100 / all-loss→0 / 혼합 Wilder 85.71)
