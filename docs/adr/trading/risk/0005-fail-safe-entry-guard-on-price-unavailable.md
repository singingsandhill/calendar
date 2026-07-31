# ADR-0005: 현재가 조회 실패 시 진입 가드는 우회가 아니라 차단 (fail-safe)

| 항목 | 값 |
|---|---|
| 상태 | Accepted ([risk/0003](0003-entry-and-time-risk-guards.md) 의 정련) |
| 날짜 | 2026-07-27 |
| 도메인 | trading |
| 관심사 | 리스크, 운영 안전 |
| 관련 커밋 | — |

## Context — 무엇이 문제였나

- P2-10/P2-12 진입 가드(물타기 차단·코인 노출상한, [risk/0003](0003-entry-and-time-risk-guards.md))는
  `executeBuy` 와 `entryRiskGuardsBlock` 에서 `if (currentPriceForGuard != null) { …가드… }`
  구조였다 — **현재가 조회가 실패하면 가드를 평가하지 않고 매수가 그대로 진행**(fail-open).
- javadoc 은 "조회 실패 시 보수적으로 통과시키지 않고 스킵"이라 서술해 실동작과 반대였다.
- 2026-07-27 운영 로그의 `PrematureCloseException`(stale 커넥션,
  [infrastructure/0004](../infrastructure/0004-reactor-netty-connection-pool-policy.md))이
  이 경로를 실제로 밟았다: orderbook 조회 실패 → `getCurrentPrice()` null → 손실 포지션
  보유 중에도 물타기 차단 없이, 노출상한 확인 없이 신규 매수 가능.

## Decision — 무엇을 골랐나

가드 입력(현재가)을 얻지 못하면 **그 틱의 신규 매수를 차단**한다.

- `executeBuy`: `getCurrentPrice()` null → WARN 로그 + 즉시 return (주문 미전송).
- `entryRiskGuardsBlock`: null → `return true`(차단). `manualBuy` 도 같은 가드를 타므로
  수동 매수도 함께 fail-safe (의도된 확장).
- javadoc 을 실동작과 일치하게 정정.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 현행 유지(fail-open) | 매수 기회 유실 없음 | 실자금 코드에서 가드 없는 매수 허용 — 우선순위 1(운영 안전) 위반. 문서와도 불일치 |
| 조회 1회 재시도 후 진행 | 기회 유실 최소화 | 재시도해도 실패하면 같은 문제. 복잡도만 증가 — 다음 틱(60초)이 곧 재평가 |
| **차단 (선택)** | 비용은 해당 틱 매수 1회 유실뿐 | — |

주문 무재시도가 "응답 유실 시 재전송하지 않는다"를 고른 것과 같은 실패 모드 선택이다
(stock [infrastructure/0005](../../stock/infrastructure/0005-non-idempotent-order-no-retry.md)):
불확실할 때는 돈이 나가지 않는 쪽으로.

## Consequences — 영향

- **긍정:** 물타기 차단·노출상한이 조회 실패 틱에도 무결. 서킷브레이커 취지(불확실 시 진입
  금지)와 일관.
- **부정:** 가격 조회가 실패한 틱의 진입 기회 유실 — [infrastructure/0004](../infrastructure/0004-reactor-netty-connection-pool-policy.md)
  적용 후엔 드문 이벤트이며, 발생 시 "Skipping BUY ... price unavailable" WARN 으로 관측 가능.
- **후속:** 없음.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java`
- 관련 테스트: `TradingBotServiceGuardFailSafeTest`
- 트리거: 2026-07-27 운영 로그 (PrematureCloseException → getCurrentPrice null 경로)
