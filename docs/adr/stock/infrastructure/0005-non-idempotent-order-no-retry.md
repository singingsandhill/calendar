# ADR-0005: 비멱등 주문 POST 무재시도

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 인프라 / 주문 안정성 |
| 관련 ADR | [stock/infrastructure/0001](0001-kis-rate-limit-semaphore.md) (Semaphore — 유지), [trading/infrastructure/0002](../../trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md) (크립토 선영속화 — 후속 대응물) |
| 관련 이슈 | 주식 봇 로직 리뷰 (2026-07-24) §3-③ — 적대적 검증 유지 |

## Context — 무엇이 문제였나

주문 실행(`KisRestClient.executeOrder`, `/order-cash` POST)이 조회와 동일한
`executePostWithRetry` 를 타서 `Retry.backoff(3, 1s)` + 재시도 대상
(IOException/"prematurely closed"/429/5xx)이 적용됐다. **주문 POST 는 비멱등** —
KIS 가 주문을 접수했는데 응답만 유실(read timeout·게이트웨이 5xx)되면 재시도가
동일 시장가 주문을 최대 3회 중복 전송한다.

## Decision — 무엇을 골랐나

주문 경로를 **`executePostNoRetry`** 로 전환 — Semaphore 게이트·타임아웃은 유지하되
`retryWhen` 제거. 실패는 즉시 null 반환하고, 주문 접수 여부 확인은 당일주문조회
(TTTC8001R)의 몫으로 남긴다 (체결 확인/backfill 은 리뷰 P1-1 후속).

| 대안 | 장단점 | 채택/기각 |
|---|---|---|
| 재시도 유지 + 멱등키 | KIS order-cash 는 클라이언트 멱등키 미지원 | 기각 |
| 재시도 전 주문조회로 접수 확인 | 안전하나 구현 복잡 — P1-1(체결 backfill)과 함께 재검토 | 보류 |
| **(선택) 무재시도** | 중복 주문 원천 차단, 최소 변경. 일시 오류 시 해당 틱 주문 1회 유실(다음 틱 재평가) | 채택 |

## Consequences

- 일시적 네트워크 오류 시 그 틱의 진입/청산 주문이 유실될 수 있으나, 트레이딩 루프가
  5초 주기로 조건을 재평가하므로 재기회가 있다. 중복 실주문보다 압도적으로 싼 실패 모드.
- 조회(GET) 경로의 재시도는 그대로 유지.
- 회귀 가드: `KisRestClientOrderRetryTest` (MockWebServer — 주문 5xx 시 요청 1회,
  시세 5xx 시 재시도로 2회).

## References

- `stock/infrastructure/api/KisRestClient.java` (`executePostNoRetry`)
- `docs/audit/stock-trading-logic-review-2026-07-24.md` §3-③·§9(P0-4)
