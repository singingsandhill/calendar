# ADR-0002 (trading/modes): 기본 모드 PAPER 전환 — LIVE 는 명시적 opt-in

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-25 |
| 도메인 | trading (코인) |
| 관심사 | 모드 / 실행 안전 |
| 관련 | [0001](0001-paper-mode-default-and-order-gate.md) (기본값 결정을 본 ADR 이 대체), 운영 감사 `docs/audit/coin-trading-operational-review-2026-07-06.md` (P1-8), 수익성 검사 2026-07-25 |

## Context — 무엇이 문제였나

[0001] 은 모드 게이트를 도입하면서도 기존 운영 연속성을 위해 **기본값 LIVE** 를
유지했다(운영자 결정, 2026-05-30). 이후 상황이 바뀌었다:

1. 운영 감사(2026-07-06)가 P1-8 로 재지적: 설정 누락·오타가 **실주문 쪽으로 폴백**하는
   fail-dangerous 기본값. `setMode(null)` 도 LIVE 로 떨어졌다.
2. 수익성 검사(2026-07-25) 결론: 재보정된 R:R 파라미터의 +EV 가 어디에도 입증되지 않아
   **PAPER 실측이 LIVE 의 전제조건**이다. 즉 당분간의 정상 운영 모드 자체가 PAPER 다.
3. stock 봇은 이미 [stock/modes/0002](../../stock/modes/0002-paper-default-mode.md) 로
   PAPER 기본 전환을 마쳤다 — 두 봇의 안전 기본값이 어긋난 상태였다.

## Decision — 무엇을 골랐나

- `TradingProperties.Bot.mode` 기본값 **LIVE → PAPER**. `setMode(null)` 폴백도 PAPER.
- `application.yaml`: `mode: ${TRADING_BOT_MODE:PAPER}` — 실주문은 서버 환경변수
  `TRADING_BOT_MODE=LIVE` 로만 활성화.
- [0001] 의 모드 게이트·인메모리 체결 구조는 그대로 유지. **기본값 결정만 대체**한다.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| LIVE 기본 유지 (0001) | +EV 미입증 상태에서 fail-dangerous — 검사·감사 결론과 정면 충돌 |
| 이중 플래그 (mode=LIVE AND armed) | stock 과 다른 패턴 — 한 프로젝트에 안전장치 두 방식 |
| **(선택) PAPER 기본 + env opt-in** | stock/modes/0002 와 동일 패턴 — 오설정이 안전측으로 폴백 |

## Consequences — 영향

- **긍정:** 설정 누락·오타·null 이 전부 PAPER 로 폴백. PAPER 실측(수익성 검사 후속
  B-트랙 전제조건)이 기본 동작이 된다. 두 봇의 안전 모델 일치.
- **주의 (운영 영향):** 기존에 env 미설정으로 LIVE 운영하던 배포는 이 변경 후
  **자동으로 PAPER 가 된다.** 실주문을 계속하려면 배포 환경에 `TRADING_BOT_MODE=LIVE`
  를 명시해야 한다.
- P1-9(Java 기본값 ↔ yaml 불일치) 도 동시 해소 — Risk/Bot/Rebalancing 기본값을 yaml
  운영값과 정합.

## References

- 코드: `TradingProperties.java` (`Bot.mode`, `setMode`), `application.yaml` (`trading.bot.mode`)
- 미러: [stock/modes/0002](../../stock/modes/0002-paper-default-mode.md)
