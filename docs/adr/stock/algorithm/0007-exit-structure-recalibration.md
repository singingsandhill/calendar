# ADR-0007: 청산 구조 재보정 — 풀백저가 손절 + 고정 앵커 TP3 + 트레일링 정상화

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 리스크 |
| 관련 ADR | [stock/algorithm/0004](0004-tp-independent-triggers.md) (TP 비순차화 — 앵커 정의 갱신), [trading/strategy/0005](../../trading/strategy/0005-exit-risk-reward-recalibration.md) (크립토 R:R 재보정 선례), [stock/modes/0002](../../stock/modes/0002-paper-default-mode.md) |
| 관련 이슈 | 주식 봇 로직 리뷰 [`docs/audit/stock-trading-logic-review-2026-07-24.md`](../../../audit/stock-trading-logic-review-2026-07-24.md) §4 (P1-2/3/4) |

## Context — 무엇이 문제였나

리뷰에서 청산 체인을 실제로 추적한 결과, **승자 트레이드의 이익 실현 경로가 구조적으로
붕괴**돼 있었다.

1. **TP3 도달 불가** — `shouldTp3` 가 매 틱 갱신되는 당일고가(`stck_hgpr`)를 앵커로 써서
   5초 폴링 간격에 +10% 점프를 요구했다. KRX VI(±3% 급변 시 단일가) 하에서 수학적으로 불가능.
2. **트레일링 미작동** — 활성화 조건이 `tp1Executed` 뿐인데, 파라미터상 TP2(전고점 회복,
   진입가 +0.8~5%)가 TP1(+5%)보다 거의 항상 먼저 발동해 TP1 이 실행되지 않는다. 활성화되더라도
   `trailingStopPrice` 를 신고가 갱신 블록에서만 세팅해, 활성화 직후 하락하면 스탑가가 null 인
   채 영원히 미발동.
3. **R:R 역전** — 결과적으로 잔여 물량의 보호·익절 수단이 -5% 손절과 11:20 시간청산뿐이라,
   승리 혼합익 ≈ +1.1% vs 패배 -5.26%(비용 포함) → **손익분기 승률 ~83%**.

## Decision — 무엇을 골랐나

**(1) 손절 앵커를 진입 근거에 묶는다** (사용자 선택):

```
SL = max(풀백저가 × (1 - pullback-stop-buffer-percent), 진입가 × (1 - max-stop-loss-percent))
```

- `risk.pullback-stop-buffer-percent: 1.0`, `risk.max-stop-loss-percent: 2.0` 신설.
- 진입가 = 풀백저가 × 1.002(bounce) 이므로 통상 진입가 대비 **약 -1.2%**. 진입 논리("풀백저가
  반등")가 깨지는 지점에서 끊는다.
- 체결가가 풀백저가에서 크게 밀린 경우(슬리피지·드리프트)에는 캡(-2%)이 손실을 제한한다.
- 풀백저가가 없거나 비정상(진입가 이상)이면 캡만 적용 — 손절가는 항상 진입가 아래.
- 레거시 `risk.stop-loss-percent`(5.0)는 계산에서 제외(키는 하위호환으로 보존).

**(2) TP3 앵커를 진입가 고정으로** (사용자 선택): `TP3 = 진입가 × (1 + tp3-percent)`.
세 단계가 모두 고정 앵커(진입가·전고점)가 되어 도달 가능해진다.

**(3) 트레일링 정상화**: 활성화 조건을 `tp1 || tp2 || tp3`(부분익절 발생)로 넓히고,
**활성화 시점에 스탑가를 즉시** `max(현재가 × (1 - trailing%), 손익분기가)` 로 세팅한다.

## Rationale

| 대안 | 기각 이유 |
|---|---|
| 고정 -2% 손절 | 단순하나 풀백 깊이와 무관 — 진입 논리와 무관한 지점에서 끊긴다 |
| 현행 -5% 유지 + TP 조정 | R:R 역전(손절 폭이 익절 폭의 4배 이상)이 남는다 |
| TP3 삭제 | 상승 여력 포착을 트레일링에만 의존 — 앵커 수정이 더 작은 변경으로 같은 효과 |
| 진입 시점 고가 스냅샷 +10% | 2시간 창에서 도달 빈도가 지나치게 낮다 |

## Consequences

- **손절 빈도 증가 예상**: -1.2% 손절은 노이즈에 더 자주 걸린다. 승률은 낮아지되 손실 크기가
  1/4 로 줄어 기대값이 개선되는 트레이드오프 — **PAPER 실측(리뷰 P2-5)으로 검증 후 LIVE**.
- TP2 이후 잔여 물량이 트레일링(-3.8%, 손익분기 하한)으로 실제 보호된다.
- CLAUDE.md 의 Exit Rules 표를 갱신(Stop Loss·TP3 행). ADR-0004 의 TP 비순차화 결정은 유지되며
  앵커 정의만 본 ADR 로 갱신된다.
- 회귀 가드: `StockPositionStopLossTest`(4), `StockPositionTakeProfitTest`(TP3 앵커·트레일링 4).

## References

- `stock/domain/position/StockPosition.java` (`resolveStopLossPrice`, `shouldTp3`, `updateTrailingStop`)
- `stock/application/service/StockPositionService.java` (진입 시 손절가 산정)
- `application.yaml:255-265` (risk 블록)
