# ADR-0003 (trading/risk): 진입·시간 리스크 가드 (maxHold 출구 / 물타기 차단 / 노출 상한)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 리스크 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-8/P2-10/P2-12) |

## Context — 무엇이 문제였나

포지션 수준 리스크 가드 3종이 없었다.

1. **정체 포지션(P2-8):** TP/트레일링/손절 어디에도 안 걸린 포지션이 무한정 자본을
   점유 → 자본 회전·기회비용 손실.
2. **물타기(P2-10):** 손실 중인 포지션이 있어도 신규 매수가 추가 진입 → 하락장에서
   노출 2배·이중 손절.
3. **노출 무제한(P2-12):** `executeBuy` 가 *available KRW* 만 보고 사이징 → 코인 비중이
   의도(불 타깃 70%)를 넘어 무한 누적 가능.

## Decision — 무엇을 골랐나

설정 가능한 가드 3종(0/false 로 비활성):

- **P2-8 시간 청산:** `Bot.maxHoldMinutes`(기본 360). `RiskManagementService.checkPositionRisk`
  에서 보유 시간 초과 **AND 수수료차감 PnL ≥ 0%(손익분기)** 이면 `CloseReason.TIME_EXIT`
  청산. 적자 포지션은 손절(-1.5%)이 처리 — 시간 청산으로 손실 강제 안 함.
- **P2-10 물타기 차단:** `Bot.blockAveragingDown`(기본 true). `executeBuy` 진입 전 OPEN
  포지션 중 현재가 기준 손실인 것이 있으면 신규 매수 차단(이익 중일 때만 가산 = 승자
  피라미딩 허용).
- **P2-12 노출 상한:** `Bot.maxCoinExposurePct`(기본 0.8). `executeBuy` 진입 전 코인가치/
  총자본 ≥ 상한이면 매수 스킵.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| maxPositions=1 (물타기 원천 차단) | 승자 가산(피라미딩)까지 막음 — 너무 둔탁 |
| 시간 청산을 손익 무관 강제 | 적자 강제 확정 → 손절 정책과 충돌. 손익분기 이상만으로 한정 |
| 노출 상한 없이 리밸런싱에만 의존 | 리밸런싱은 8h 쿨다운 — 신호 매수가 그 사이 과집중 가능 |

## Consequences — 영향

- **긍정:** 정체 자본 회수→회전↑, 하락장 물타기/과집중 차단, 코인 비중 상한 보장.
- **비용:** `executeBuy` 에 `getCurrentPrice`/`getCoinBalance` 호출 추가(매수 신호 시에만).
- **상호작용:** 물타기 차단으로 `maxPositions=2` 의 2차 진입은 *1차가 이익 중일 때만* 발생.
  시간 청산은 stop/TP/trailing 다음 우선순위(그것들이 먼저 트리거).
- 신호/리스크 동작 변경이므로 **LIVE 전 PAPER 백테스트 권장**.

## References

- 코드: `RiskManagementService.shouldTimeExit`/`checkPositionRisk`,
  `TradingBotService.blocksAveragingDown`/`exceedsExposureCap`/`executeBuy`,
  `TradingProperties.Bot`(maxHoldMinutes/blockAveragingDown/maxCoinExposurePct),
  `CloseReason.TIME_EXIT`
- 테스트: `RiskManagementServiceTimeExitTest`, `TradingBotServiceEntryGuardTest`
