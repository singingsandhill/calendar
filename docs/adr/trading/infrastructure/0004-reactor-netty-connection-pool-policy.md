# ADR-0004: Reactor Netty 커넥션 풀에 idle 폐기 정책 도입

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-27 |
| 도메인 | trading (stock 공유) |
| 관심사 | 인프라 |
| 관련 커밋 | — |

## Context — 무엇이 문제였나

- 운영 로그(2026-07-27)에 `reactor.netty...PrematureCloseException: Connection prematurely
  closed BEFORE response` WARN 이 매분 :05초(트레이딩 루프 시작) 부근에서 3~10분 간격으로
  밤새 반복됐다. 커넥션 ID 접미사(-4~-128)는 풀에서 여러 번 재사용된 커넥션임을 보여준다.
- `WebClientConfig` 는 `HttpClient.create()` — 기본 `ConnectionProvider` 사용으로
  `maxIdleTime` 무한, `evictInBackground` 없음. Bithumb 서버가 keep-alive idle 타임아웃으로
  이미 닫은 커넥션을 풀이 들고 있다가 재사용 → 요청 직후 FIN → `BEFORE response` 실패.
- 이 실패는 조용히 사라지지 않는다: 조회 결과가 null/빈 값이 되어 해당 틱의 손절/익절 체크
  스킵, SELL 스킵, (수정 전) 진입 가드 우회까지 이어졌다 — [risk/0005](../risk/0005-fail-safe-entry-guard-on-price-unavailable.md).
- 이 `WebClient.Builder` 빈은 싱글턴이라 stock 모듈(KisRestClient/KisAuthService)도 같은
  HttpClient·풀을 공유한다.

## Decision — 무엇을 골랐나

커스텀 `ConnectionProvider` 로 idle 커넥션을 서버보다 먼저 폐기한다.

- `maxIdleTime=10s` — 크립토 루프(매분)의 틱 간 커넥션은 확실히 폐기, stock 5초 루프는
  warm 재사용 유지. 서버측 keep-alive 보다 보수적으로 짧게가 이 문제의 정석.
- `maxLifeTime=5m`, `evictInBackground=30s` — LB/프록시 교체·장수 커넥션 리스크 상한.
- 기존 타임아웃 3종(connect 10s, response/read/write 30s)은 유지.
- **GET 재시도는 이번에 추가하지 않는다(보류)** — 원인(stale 재사용)은 풀 정책이 제거하고,
  잔여 실패는 fail-safe(틱 스킵 후 재평가)로 흡수. 풀 수정 후에도 재발하면 그때
  `BithumbPublicApi` 개별 GET 경로에 `PrematureCloseException` 한정 1회 재시도를 별건 결정으로.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| HttpClient 전역 retry | 구현 최소 | 주문 POST 까지 재시도가 물든다 — 비멱등 주문 무재시도 원칙([0002](0002-order-pre-persistence-and-tick-sweep.md), stock [0005](../../stock/infrastructure/0005-non-idempotent-order-no-retry.md)) 위반 위험 |
| `keepAlive(false)` | stale 원천 차단 | 매 요청 TLS 핸드셰이크 — 5초 stock 루프에 과도한 비용 |
| **idle 폐기 정책 (선택)** | 원인 제거 + 재사용 이점 유지 | — |
| GET 재시도 동시 도입 | 잔여 실패도 흡수 | 실제 발생이 확인되지 않은 상황에 방어 코드 금지 원칙 — 풀 수정 효과 확인 후 판단 |

## Consequences — 영향

- **긍정:** stale 재사용 실패 소멸 → 리스크 체크/SELL/스냅샷 누락 경로 자체가 드물어짐.
  root 로거를 오염시키던 netty WARN(stock-trading.log)도 함께 소멸.
- **부정:** 크립토 루프는 사실상 매 틱 새 커넥션(TLS 핸드셰이크 ~수십 ms) — 매분 1회 수준이라
  무시 가능. `ConnectionProvider` 는 설정값 introspection API 가 없어 단위 테스트로 값을
  단정할 수 없다 — 완치 판정은 운영 로그(`PrematureCloseException` 0건).
- **후속:** stock 모듈도 같은 풀을 쓰므로 KIS 호출 특성이 바뀌면 이 정책을 재검토.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/trading/infrastructure/config/WebClientConfig.java`
- 관련 테스트: `WebClientConfigTest` (구성 스모크)
