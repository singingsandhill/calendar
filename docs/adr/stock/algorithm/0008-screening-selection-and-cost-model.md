# ADR-0008: 선정 규칙 강화(강제 선정 제거 + 신호 게이트) · 비용 모델 정정 · 거래정지 가드

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 리스크 |
| 관련 ADR | [stock/algorithm/0007](0007-exit-structure-recalibration.md) (청산 구조), [stock/algorithm/0006](0006-universe-rank-retry-at-screening.md) (유니버스), [trading/strategy/0004](../../trading/strategy/0004-fee-cost-model-and-net-margin-threshold.md) (크립토 비용모델 선례) |
| 관련 이슈 | 주식 봇 로직 리뷰 §4·§5·§6 (P2-1/2/3) |

## Context — 무엇이 문제였나

1. **강제 선정** — 선정 루프가 `selected.size() < minCandidates || score >= threshold` 라서
   점수 미달 종목도 상위 3개는 무조건 선정됐다. 엣지가 없는 날에도 매일 진입을 시도하는
   구조 = 비용만 지출.
2. **점수 왜곡** — 가중치 합 100 중 유동성 팩터(거래대금 20 + 스프레드 15 + 시총 10 = **45**)
   만으로 총점 임계(40)를 넘길 수 있어, 갭·체결강도 *신호가 거의 없는* 종목이 통과 가능했다.
3. **비용 모델 오차** — `sellTaxRate` 가 0.23%(구 세율)로 고정돼 있었고, `slippageBuffer`(0.2%)는
   정의만 되고 **사용처가 0곳**이었다. 모든 주문이 시장가인데 익절 게이트가 슬리피지를
   무시하면 명목상 익절이 실제로는 순손실이 될 수 있다.
4. **거래정지·VI 무방비** — VI(±3% 급변 시 단일가)·거래정지·관리종목·정리매매·투자경고를
   판별하는 코드가 전무했고, 갭 상한 15% 허용과 결합하면 위험한 조합이었다.

## Decision — 무엇을 골랐나

**(1) 선정 규칙** (사용자 선택 — 단계적):
- `min-candidates` 강제 선정 **제거**. 조건에 맞는 종목이 없으면 그날 선정 0건이 정상.
- **신호 게이트 신설**: `gapScore + strengthScore >= scoring.signal-min-score`(기본 25).
  총점 임계와 **둘 다** 통과해야 선정된다.
- 갭 floor 0.5% 는 **유지** — PAPER 실측 표본을 확보하고, 실측 후 상향 여부를 결정한다.
- 선정 로직은 `ScreeningService.selectCandidates()` 로 분리해 단위 테스트 가능.

**(2) 비용 모델**:
- `sellTaxRate` 0.23% → **0.20%**. 2026-01-01 이후 양도분 기준 코스피 = 증권거래세 0.05% +
  농특세 0.15%, 코스닥·K-OTC = 0.20%(농특세 없음) → 두 시장 모두 0.20%.
  (리뷰 §4 의 "0.15%" 권고는 2025년 세율이었다 — 본 ADR 이 정정.)
- `getEffectiveExitCostRate()` = 왕복 수수료·세금 + 슬리피지. **익절 게이트는 순익에서
  슬리피지를 차감**한 뒤 최소수익 임계와 비교하고, 트레일링 손익분기 하한도 이 값을 쓴다.

**(3) 거래정지·VI 가드**: `KisQuoteResponse` 에 상태 필드 4종
(`iscd_stat_cls_code`, `temp_stop_yn`, `mrkt_warn_cls_code`, `sltr_yn`)을 추가하고
`isTradable()` 로 판정 — 스크리닝 Floor 0번 필터 + **진입 직전 재확인**(09:20 통과 후에도
VI 가 걸릴 수 있음). 배제 대상: 임시정지(VI 포함)·거래정지(58)·관리(51)·투자위험/경고
(52/53, 시장경고 02/03)·단기과열(59)·정리매매. 투자주의(01/54)는 허용.

**필드 부재는 "거래 가능"으로 본다** — 응답 스펙 차이로 전 종목이 걸러지는 회귀를 막기
위함이며, 전 종목에서 상태 필드가 비면 WARN 으로 필드명 오류를 드러낸다(cttr 계측과 동일 패턴).

## Rationale

| 대안 | 기각 이유 |
|---|---|
| 갭 floor 0.5% → 2.0% 동시 상향 | 정확도는 오르나 선정 0건 날이 급증해 PAPER 실측 표본 확보가 어려움 — 실측 후 재검토 |
| 신호 게이트 없이 총점 임계만 상향 | 유동성 팩터가 임계를 밀어올리는 구조적 왜곡이 그대로 남음 |
| 시장 구분(KOSPI/KOSDAQ)별 세율 분기 | 2026년 기준 양쪽 모두 0.20% 라 분기 이득 없음 |
| VI 전용 API(국내주식 VI발동종목) 추가 조회 | 틱당 API 호출 증가. 시세 응답의 상태 필드로 충분 |

## Consequences

- **선정 빈도 감소** — "매일 3건" 이 사라지고 신호 있는 날에만 진입한다. 표본이 줄어드는
  만큼 P2-5 실측 기간을 넉넉히 잡아야 한다.
- 익절 게이트가 슬리피지만큼 높아져 한계 익절이 보류된다(순손실 거래 방지).
- 상태 필드명이 실제 KIS 응답과 다르면 가드가 무력화되지만 기존 동작과 동일(무회귀) —
  다음 거래일 로그의 WARN 으로 즉시 판별 가능.
- 회귀 가드: `ScreeningSelectionTest`(4), `StockCostModelTest`(4),
  `StockRiskServiceSlippageGateTest`(2), `KisQuoteTradabilityTest`(7),
  `StockPositionServiceTest.openPosition_refusesWhenStockIsHaltedAtEntryTime`.

## References

- `stock/application/service/ScreeningService.java` (`selectCandidates`, Floor 0 가드)
- `stock/infrastructure/config/StockProperties.java` (`signalMinScore`, `sellTaxRate`, `getEffectiveExitCostRate`)
- `stock/infrastructure/api/dto/KisQuoteResponse.java` (`isTradable`)
- 세율 출처: [2026 달라지는 것 — 증권거래세율 인상](https://news.nate.com/view/20251231n05425),
  [헤럴드경제](https://biz.heraldcorp.com/article/10627001)
