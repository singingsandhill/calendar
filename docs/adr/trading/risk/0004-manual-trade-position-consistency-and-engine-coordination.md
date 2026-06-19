# ADR-0004 (trading/risk): 수동 매매 Position 정합 + 엔진 핑퐁 방지 + 쿨다운 상향

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 리스크 / 회계 정합 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (#3, P2-11, P2-9) |

## Context — 무엇이 문제였나

1. **수동 매매 회계 붕괴(#3):** `manualBuy` 는 `Trade` 만 저장하고 **Position 미생성** →
   그 코인은 SL/TP/트레일링 보호 없음 + 리스크 루프에 비가시. `manualSell` 은 OPEN
   Position 을 닫지 않아 **추적 Position ↔ 실잔고 드리프트** → 이미 판 코인을 리스크 루프가
   유령 포지션으로 재매도 시도.
2. **엔진 핑퐁(P2-11):** 신호 매매는 `lastTradeTime`(신호 쿨다운)만 갱신하고
   `lastRebalanceTime`(리밸런스 쿨다운)은 갱신 안 함 → 신호 매매 직후 틱에 리밸런스가
   발화 가능(틱 간 핑퐁, 매 레그 taker 수수료).
3. **과회전(P2-9):** 1분봉에서 신호 쿨다운 10분/최소 보유 15분이 너무 짧음.

## Decision — 무엇을 골랐나

- **#3 manualBuy:** `extractExecutedPriceWithRetry` 실체결가로 추적 `Position`(SL/TP) 생성.
  → 수동 매수 코인도 리스크 루프가 보호. (영속화는 `TransactionTemplate`.)
- **#3 manualSell:** 실체결가 사용 + `reconcilePositionsAfterManualSell` — OPEN 포지션을
  FIFO 로 **청산 기록**(추가 주문 없이, 실제 매도는 manual 주문으로 이미 체결). soldVolume
  소진까지 오래된 포지션부터 닫아 추적 ↔ 실잔고 정합.
- **P2-11:** `RebalanceService.markRebalanceCooldown()` 추가, 신호 매매(`executeBuy`/
  `executeSell`) 성공 시 호출 → 리밸런스 쿨다운도 갱신. (리밸런스 실행 시 신호 쿨다운
  갱신은 기존부터 존재 → 양방향 대칭.)
- **P2-9:** `signal-cooldown-minutes` 10→30, `min-holding-minutes` 15→30 (config).

## Consequences — 영향

- **긍정:** 수동 매매도 리스크 보호 + 잔고 정합(유령 포지션 재매도 제거), 신호↔리밸런스
  핑퐁 차단, 1분봉 과회전·수수료 churn 감소.
- **한계:** manualSell 정합은 **whole-position** 단위(soldVolume 이 포지션 경계와 안 맞으면
  근사). 쿨다운 30분은 감사 권장(≥60)보다 보수적 — PAPER 백테스트로 조정.
- 신호/리스크 동작 변경 → LIVE 전 PAPER 백테스트 권장.

## References

- 코드: `TradingBotService.manualBuy`/`manualSell`/`reconcilePositionsAfterManualSell`,
  `RebalanceService.markRebalanceCooldown`, `resources/application.yaml`(쿨다운)
- 테스트: `TradingBotServiceManualTest`, `RebalanceServiceCooldownTest`
