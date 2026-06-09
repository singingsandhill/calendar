# ADR-0007 (trading/strategy): 다이버전스 피벗 강화 (k봉 + MIN_DISTANCE)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 지표 품질 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-1) |

## Context — 무엇이 문제였나

`DivergenceService` 의 로컬 극값(피크/밸리) 검출이 **3봉 피벗**(`i-1, i, i+1` 즉시 이웃만
비교)이고 `MIN_DISTANCE=3`(3봉) 이었다. 1분봉에서는 **단일봉 잡음 스파이크**도 피벗으로
잡혀 *가짜 다이버전스* 를 양산. 다이버전스는 ±135 스코어 중 RSI(±20)+Stoch(±15)+
Volume(±20) = 최대 **55점** 을 차지하므로, 피벗 품질이 신호 전체에 큰 영향.

## Decision — 무엇을 골랐나

- **k봉 피벗:** `PIVOT_STRENGTH=3` — 후보가 좌우 각 3봉 *모두* 보다 엄격히 낮아야(밸리)/
  높아야(피크) 피벗으로 인정. 단일봉 잡음 제거.
- **`MIN_DISTANCE` 3 → 5:** 비교하는 두 극값 간 최소 거리 확대.
- 검출 로직만 변경 — 스코어 가중치(±20/±15 등)는 불변.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 3봉 피벗 유지 | 1분봉 잡음 스파이크가 가짜 다이버전스 생성 |
| 다이버전스 스코어 비활성 | 추세전환 신호 자체는 유효 — 검출 품질만 문제 |
| **(선택) k봉 피벗 + 거리 확대** | 검출 품질만 개선, 가중치/구조 불변 — 최소 침습 |

캔들은 최신순(DESC) 이라 피벗은 좌측(더 최신) k봉이 확정돼야 인정 = **k봉 지연 확정**
(룩어헤드 아님 — `i+d` 는 더 오래된 과거 봉).

## Consequences — 영향

- **긍정:** 가짜 다이버전스 감소 → 거짓 신호 질량 ↓ (특히 1분봉).
- **주의:** 다이버전스 발생 빈도 감소 → 일부 실제 다이버전스도 늦게/덜 잡힘. 신호 동작
  변경이므로 **LIVE 전 PAPER 백테스트 권장**.
- 후속(P2-1b, 미적용): volumeDivergence 가중치 재검토 / RSI·Stoch 다이버전스 가중치
  하향은 스코어 가중치 변경(ADR-0001 갱신 필요) — 별도 백테스트 동반.

## References

- 코드: `application/service/DivergenceService.java`
  (`findLocalMinima`/`findLocalMaxima`, `PIVOT_STRENGTH`, `MIN_DISTANCE`)
- 테스트: `DivergenceServicePivotTest`
