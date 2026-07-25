# ADR-0002: PAPER 기본 모드 — LIVE 는 명시적 opt-in

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 모드 / 안전 |
| 관련 ADR | [stock/modes/0001](0001-paper-backtest-mode-and-clock-bean.md) (모드 도입), [trading/modes/0001](../../trading/modes/0001-paper-mode-default-and-order-gate.md) (크립토 선례), [common/security/0005](../../common/security/0005-admin-only-stock-bot-control-api.md) |
| 관련 이슈 | 주식 봇 로직 리뷰 (2026-07-24) §3-②·§4 |

## Context — 무엇이 문제였나

`StockProperties.Bot.mode` 기본값이 **LIVE** 였다 — 설정 누락만으로 실계좌 주문이
나가는 방향의 기본값. 리뷰 결과 전략 기대값이 미검증(백테스트·실거래 0회)이고 청산
체인에 다수 결함(TP1 수량·TP3 도달불가·트레일링 미작동)이 있는 상태라, 유니버스
병목이 풀리는 즉시 검증 안 된 전략이 실계좌에서 돌 위험이 있었다. 크립토 모듈은
수익성 감사 후 같은 이유로 PAPER 기본 전환(ADR trading/modes/0001) 선례가 있다.

## Decision — 무엇을 골랐나

- `Bot.mode` 기본값 LIVE → **PAPER** (`setMode(null)` 폴백도 PAPER).
- `application.yaml` 에 `stock.bot.mode: ${STOCK_BOT_MODE:PAPER}` 노출 —
  **LIVE 는 서버 환경변수 `STOCK_BOT_MODE=LIVE` 로만 활성화** (명시적 opt-in).
- 주문 진입부 모드 가드(`KoreaInvestmentApiClient.isLiveMode`)는 기존 그대로.

## Consequences

- **운영 주의:** 기존 서버는 env 미설정 시 LIVE 였다 — 배포 후 실주문을 원하면
  `STOCK_BOT_MODE=LIVE` 를 명시해야 한다. 리뷰 P2-5(PAPER 2~4주 실측) 통과 전에는
  설정하지 않는 것을 권장.
- PAPER 체결 모델은 요청가 즉시 전량 체결 픽션(리뷰 §6)이라 낙관적 — LIVE 성과
  추정에는 한계가 있음을 인지하고 사용.
- 회귀 가드: `StockPropertiesModeTest` (기본 PAPER + null 폴백 PAPER).

## References

- `stock/infrastructure/config/StockProperties.java` (`Bot.mode`)
- `docs/audit/stock-trading-logic-review-2026-07-24.md` §3-②·§4·§9(P0-2)
