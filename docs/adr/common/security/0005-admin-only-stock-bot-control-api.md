# ADR-0005: 주식 봇 제어 API 관리자 전용

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | common |
| 관심사 | 보안 |
| 관련 ADR | [common/security/0003](0003-admin-only-trading-control-api.md) (크립토 선례), [stock/modes/0002](../../stock/modes/0002-paper-default-mode.md) |
| 관련 이슈 | 주식 봇 로직 리뷰 (2026-07-24) §3-② — 적대적 검증에서 critical 상향 |

## Context — 무엇이 문제였나

`SecurityConfig` 의 `/api/stock/**` permitAll + `/api/**` CSRF 면제로
`StockBotApiController` 의 start/stop/pause/resume/emergency-close 가 **인터넷에서
무인증 POST 가능**했다. 크립토 모듈은 동일 문제를 ADR common/security/0003 으로
막았으나(`/api/trading/**` ADMIN) 주식 모듈만 누락됐고, 당시 `stock.bot.mode` 기본값이
LIVE 라 무인증 호출이 실계좌 주문·청산으로 직결될 수 있었다.

## Decision — 무엇을 골랐나

**`POST /api/stock/bot/**` 를 `ROLE_ADMIN` 전용으로 전환**한다 (`/api/**` permitAll 보다
먼저 선언). 미인증 진입점은 어드민 로그인(`defaultAuthenticationEntryPointFor`)으로 매핑.

**`GET /api/stock/bot/status` 는 permitAll 유지** — 공개 대시보드(`/stock`)의 상태 위젯이
5곳에서 사용 중이라, 전면 잠금은 공개 페이지 기능 회귀가 된다. 제어(뮤테이션)만 잠그면
이번 리뷰의 critical(무인증 제어)이 정확히 닫힌다.

## Rationale

| 대안 | 장단점 | 채택/기각 |
|---|---|---|
| `/api/stock/**` 전면 ADMIN (크립토 완전 동형) | 조회 API 의 계좌 정보 노출도 차단 | 기각(현 시점) — 공개 대시보드가 status/positions/monitoring 을 사용, 대시보드 공개 여부는 제품 결정 필요 |
| **(선택) POST 만 ADMIN** | 제어 차단 + 공개 페이지 무회귀 | 채택 |

CSRF: `/api/**` 면제는 유지 — 크립토 ADR-0003 과 동일하게 ROLE_ADMIN(세션) + 무자격증명
CORS 조합으로 교차출처 호출을 차단 (CSRF 토큰화는 공통 후속 과제).

## Consequences

- 대시보드의 제어 버튼은 관리자 세션에서만 동작 (비로그인 시 어드민 로그인 리다이렉트).
- **후속(P1):** 조회 API(`/api/stock/positions/**`, `/api/stock/monitoring/**`,
  `/api/stock/events`)와 `/stock` 대시보드 자체의 공개 범위 재검토 — 포지션 손익은
  계좌 정보다.
- 회귀 가드: `StockBotApiSecurityTest` (미인증 3xx / USER 403 / ADMIN ok / status 공개 유지).

## References

- `common/infrastructure/config/SecurityConfig.java` (POST 매처 + 진입점)
- `docs/audit/stock-trading-logic-review-2026-07-24.md` §3-②
