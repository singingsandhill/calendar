# 0003 — 트레이딩 봇 제어·실주문 API 는 관리자 전용

- Status: Accepted
- Date: 2026-07-06

## Context

코인 트레이딩(`trading`) 모듈의 봇 제어·주문 REST 엔드포인트가 전부 `/api/trading/**` 아래에
있다: 봇 시작/중지/일시정지/재개(`/api/trading/bot/*`), 수동 매수/매도(`/api/trading/bot/manual/*`),
긴급 청산(`/api/trading/bot/emergency-close`), 리밸런스 강제 실행(`/api/trading/rebalance/execute`),
그리고 **실계좌 시장가 매수를 실행하는** 검증 엔드포인트(`/api/trading/verify/test-order`,
컨트롤러 주석에 "실제 돈 사용!" 명시).

그러나 [ADR 0002](0002-cors-for-apps-in-toss-miniapp.md) 의 전제("`/api/**` 는 인증 없는 공개
엔드포인트, `permitAll` + CSRF 비활성")에 따라 `SecurityConfig` 가 `/api/**` 전체를 `permitAll`
로 두고 있어, 이 트레이딩 제어·주문 API 들이 **인증 없이 외부에서 호출 가능**했다. 또한 제어
대시보드 뷰 `/trading`(계좌·손익 데이터를 서버 렌더링)도 `/*` 포괄 `permitAll` 로 공개돼 있었다.

CORS 는 브라우저 교차출처 스크립트만 통제하며 직접 HTTP 호출(`curl` 등)은 막지 못하므로,
서버에 네트워크로 도달 가능한 누구나 실계좌 주문을 낼 수 있는 상태였다. 이는
[코인 트레이딩 운영 리뷰 2026-07-06](../../../audit/coin-trading-operational-review-2026-07-06.md)
의 **P0-1** 로 식별됐다. 어떤 ADR 에서도 이 노출을 승인한 바 없다 — ADR 0002 의 CORS 공개는
datedate/insights 등 공개 API 를 의도한 것이지 트레이딩 제어를 의도한 것이 아니다.

## Decision

- `SecurityConfig` 필터 체인에서 다음 경로를 `hasRole("ADMIN")` 로 보호한다. 매칭 우선순위상
  반드시 포괄 `permitAll` 규칙(`/api/**`, `/*`)보다 **먼저** 선언한다.
  - `/api/trading/**` — 봇 제어·수동주문·긴급청산·리밸런스·검증(실주문) API 전체
  - `/trading`, `/trading/**` — 트레이딩 제어 대시보드 뷰
- 인증은 기존 러너 어드민 폼 로그인([ADR 0001](0001-runner-admin-only-form-login.md), `ROLE_ADMIN`)
  을 재사용한다. 별도 사용자 저장소를 만들지 않는다.
- CSRF 는 이번 변경 범위에서 `/api/**` 비활성 정책을 유지한다. 대시보드 JS(`trading-*.js`)가
  동일 출처 `fetch` 로 CSRF 토큰 없이 POST 하기 때문이며, `ROLE_ADMIN`(세션 인증) + CORS
  `allowCredentials=false`(교차출처는 세션 쿠키 미전송) 조합이 무인증 노출이라는 핵심 위험을
  이미 제거한다. 완전한 CSRF 토큰화는 JS 5개 파일 수정을 수반하는 후속 과제로 분리한다.

## Consequences

- 트레이딩 제어·주문 API 와 대시보드는 러너 어드민으로 로그인한 세션에서만 접근 가능하다.
  미인증 요청은 로그인 페이지로 302 리다이렉트, 인증됐으나 비관리자면 403.
- 운영자 워크플로: `/runners/admin/login` 으로 로그인 후 `/trading` 접근 → 대시보드의 동일 출처
  `fetch` 가 세션 쿠키를 실어 `/api/trading/**` 를 호출하므로 JS 변경 없이 동작한다.
- ADR 0002 의 CORS(`/api/**`, `allowCredentials=false`)는 유효하다. 트레이딩 엔드포인트에도 CORS
  가 등록돼 있으나, 자격증명 미허용이라 교차출처 스크립트가 관리자 세션 쿠키를 전송할 수 없어
  `ROLE_ADMIN` 요구가 이를 무력화한다. (선택적 강화: 향후 `CorsConfig` 에서 `/api/trading/**` 를
  CORS 대상에서 제외 가능 — 필수는 아님.)
- 남은 후속 과제(별도): `/api/trading/**` CSRF 토큰화, 실거래 이중 안전장치(`trading-armed`
  플래그), 운영 모드 기본값 재검토 — 운영 리뷰 §5 참고.
- 회귀 가드: `TradingApiSecurityTest` — 미인증/비관리자의 `/api/trading/bot/emergency-close`
  차단(302/403, 서비스 미도달)과 관리자 접근 허용, 미인증 `/trading` 리다이렉트를 검증.
- 미니앱 회귀 없음: `CorsConfigTest`(비-트레이딩 `/api/**` preflight 공개) 여전히 통과.
