# ADR-0002 (trading/risk): 리밸런싱 회계 정합 — Position 생성/청산 기반

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 리스크 / 회계 정합 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P1-3), [ADR risk/0003 적자 매매 방지](../strategy/0003-loss-prevention-guards.md) 정련 |

## Context — 무엇이 문제였나

리밸런싱이 `Position` 추적과 **완전히 분리**되어 회계·리스크가 붕괴되어 있었다.

1. **리밸런스 매수** 는 실주문 후 `Trade` 만 저장하고 **`Position` 을 만들지 않았다.**
   → 그 코인은 `RiskManagementService` 의 OPEN-Position 순회에서 **보이지 않아
   stop-loss/TP/트레일링이 전혀 적용되지 않는 "유령 코인"** 이 됨. 불(70%) 구간에서
   자본 대부분이 이 상태일 수 있었다.
2. **리밸런스 매도** 는 포지션과 무관하게 계산된 `sellVolume` 만큼 팔고 **OPEN
   Position 을 건드리지 않았다.** → 추적 Position 과 실제 코인 잔고가 **점점 벌어짐**
   (PnL·승률·리스크 판정이 모두 왜곡).
3. 매도 손실 가드가 **평균 손익률** 기반이라, 일부 포지션이 큰 손실이어도 평균이
   양수면 청산이 통과될 수 있었다.

## Decision — 무엇을 골랐나

리밸런싱을 **Position 생명주기 위에서** 수행한다 (단일 통합 포지션 풀).

- **매수 (`buyAndOpenPosition`):** 실주문 후 `Position.open(market, price, volume, SL, TP,
  fee)` 생성·저장. SL/TP 는 `RiskManagementService.calculate{StopLoss,TakeProfit}Price`.
  → 리밸런스 코인도 리스크 루프가 보호.
- **매도 (`sellByClosingProfitablePositions`):** OPEN 포지션을 **FIFO(오래된 것부터)**
  로 순회, 각 포지션의 **수수료 차감 PnL ≥ `min-sell-pnl-pct`** 일 때만
  `RiskManagementService.closePosition(p, price, CloseReason.REBALANCE)` 로 청산.
  목표 매도량 도달 시 중단. → 추적 Position 과 실잔고가 항상 정합, 적자 포지션은
  청산 안 함.
- `RebalanceService` 에 `RiskManagementService` 주입(순환 의존 없음 — Risk 는
  Rebalance 를 모름).

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 현행(Position 무시) | 회계 드리프트 + 유령 코인 무방비 — 본 ADR 의 문제 자체 |
| 단일 원장으로 Position 폐기 | 신호 트레이딩이 Position 에 깊게 의존 — 대규모 재작성 |
| **(선택) 리밸런스를 Position 위에서** | 신호/리밸런스가 한 풀 공유, 최소 변경으로 정합 회복 |

평균 손익 가드 → **포지션별** 가드로 정련(ADR risk/0003 의 "적자 청산 방지" 의도를
더 정확히 구현). 청산은 기존 `closePosition` 재사용 → Trade 생성·수수료·서킷브레이커
집계가 일관.

## Consequences — 영향

- **긍정:** 추적 Position ↔ 실잔고 정합, 전 코인 SL/TP/트레일링 보호, PnL/승률 지표
  신뢰 회복, 적자 포지션 리밸런스 청산 차단.
- **트레이드오프:** 리밸런스·신호 포지션이 **한 풀 공유** — 리밸런스 매도가 신호
  포지션을 닫을 수도, 그 반대도 가능(같은 코인·같은 리스크 관리라 수용). 원하면 후속
  으로 `PositionOrigin` 태그 분리.
- **한계:** 매도는 **whole-position** 단위 — 목표량을 정밀히 맞추지 않고 마지막
  포지션에서 오버슈트 가능(리밸런싱은 근사이므로 수용). 정밀 부분청산은 후속.
- 리밸런스 매수는 `maxPositions` 게이트를 우회(전략적 배분이므로 의도).

## References

- 코드: `application/service/RebalanceService.java`
  (`buyAndOpenPosition`/`sellByClosingProfitablePositions`),
  `RiskManagementService.closePosition`, `CloseReason.REBALANCE`
- 테스트: `RebalanceServiceAccountingTest`
- 정련 대상: [ADR risk/0003 적자 매매 방지 가드](../strategy/0003-loss-prevention-guards.md)
