# ADR-0004 (trading/strategy): 수수료 비용모델 일원화 — 매도 게이트 순수 net 마진

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 비용모델 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P1-8/P1-9) |

## Context — 무엇이 문제였나

매도 게이트(`TradingBotService.executeTradeBySignal` 의 약한 SELL 분기)는
`pnlPct >= minProfitThreshold` 로 판단하는데:

- `pnlPct = position.calculateUnrealizedPnlPctWithFee(conservativeExitPrice, feeRate)` —
  **이미 왕복 수수료(진입+청산) 차감 + 슬리피지(conservativeExitPrice) 반영** 된 net 값.
- 그런데 `minProfitThreshold = 0.006` 의 정의는 "왕복 수수료 0.5% + 마진 0.1%" 였다.

즉 **수수료가 이중계상**: PnL 에서 한 번, 임계값에서 또 한 번. 결과적으로 매도를
허용받으려면 **총가격 기준 약 +1.6% 이동**(슬리피지 0.5% + 왕복 수수료 0.5% + 0.6%)이
필요 → 의도(~1.1%)보다 훨씬 높은 허들. 약세 신호가 떠도 +1.6% 미만이면 청산 못 하고
홀드하다 −3% 손절까지 가는 경우 발생.

## Decision — 무엇을 골랐나

매도 게이트 임계를 **순수 net 마진** 으로 정정(감사 옵션 b).

- `minProfitThreshold: 0.006 → 0.001` (net 마진 0.1% — 원래 주석 의도 복원).
- PnL 은 fee+슬리피지 차감 유지(비용 정확). 임계는 마진만 → 이중계상 제거.
  매도 게이트 총가격 허들 ~1.6% → **~1.1%**.
- **수수료 단일 출처:** 모든 비용 계산(PnL, `simulateOrder`)이 `risk.takerFeeRate` 사용.
- **쿠폰(P1-9):** Bithumb 은 maker=taker=0.25% 라 limit 으로는 수수료를 못 줄인다(원
  finding "maker 로 절반" 정정 — limit 은 *슬리피지* 만 절감). 진짜 레버는 0.04% 쿠폰 →
  `taker-fee-rate: 0.0004` 로 설정하면 전 비용 계산이 자동 반영, 임계도 더 낮출 수 있음.
- `taker-fee-rate` / `min-profit-threshold` 를 yaml 에 명시(가시성·튜닝).

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| (a) PnL 을 gross 로 바꾸고 임계 = 전체 왕복비용+마진 | PnL 계산을 비용-부정확하게 만듦, 더 침습적 |
| 임계만 더 올려 "확실히 이익" | 이중계상 자체를 안 고침 — 허들만 더 높아짐 |
| **(선택) PnL net 유지 + 임계 = 순수 마진** | 비용 정확성 유지, 이중계상 제거, 최소 변경 |

## Consequences — 영향

- **긍정:** 약세 신호 시 작은 이익(+0.1% net)에 청산 가능 → 이익을 −3% 손절로
  되돌리는 위험 감소. 쿠폰 적용 시 순엣지 추가 개선.
- **트레이드오프:** 매도 문턱이 낮아져 약세 신호에 더 자주 청산(회전 ↑ 가능) — 단
  쿨다운/최소보유/신호 임계가 과회전을 억제. 기본 LIVE 이므로 PAPER 백테스트 권장.
- **한계 (후속):** 리스크 출구(stop/TP/trailing, `RiskManagementService`)는 현재가
  기준(슬리피지 미적용)이라 신호 출구(conservativeExitPrice)와 비대칭. 동일
  conservativeExitPrice 적용은 별도 검토.

## References

- 코드: `infrastructure/config/TradingProperties.Risk` (minProfitThreshold/takerFeeRate),
  `TradingBotService.executeTradeBySignal`,
  `domain/position/Position.calculateUnrealizedPnlPctWithFee`, `resources/application.yaml`
- 테스트: `PositionCostModelTest`
