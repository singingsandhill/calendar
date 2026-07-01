# 0002 — apps-in-toss 미니앱을 위한 /api CORS 허용

- Status: Accepted
- Date: 2026-06-07

## Context

datedate 의 모바일 버전(앱인토스 WebView 미니앱, 별도 레포 `datedate-app`)이 별도 origin 에서
`https://datedate.site/api/**` 를 브라우저 `fetch` 로 호출한다. 기존엔 CORS 설정이 전혀 없어
교차출처(cross-origin) 호출이 브라우저 단계에서 차단됐다(preflight 응답에
`Access-Control-Allow-Origin` 헤더 없음).

`/api/**` 는 인증이 없는 공개 엔드포인트이며(`SecurityConfig` 에서 `permitAll`, CSRF 비활성),
쿠키/세션을 사용하지 않는다. owner 식별은 URL 경로의 공개 slug 로만 이루어진다.

## Decision

- `CorsConfig` 에 `/api/**` 한정 `CorsConfigurationSource` 빈을 추가한다.
- `SecurityConfig` 의 필터 체인에 `.cors(Customizer.withDefaults())` 를 활성화해 Spring Security 가
  위 빈을 사용하도록 한다.
- 설정: `allowCredentials=false`, 허용 메서드 `GET/POST/PATCH/DELETE/OPTIONS`, 허용 헤더
  `Authorization, Content-Type, Accept-Language`, `maxAge=3600`.
- origin: 운영 미니앱 WebView 의 정확한 origin 이 미확정이라 `allowedOriginPatterns("*")` 로
  폭넓게 허용한다. 쿠키/자격증명을 쓰지 않으므로 와일드카드 origin 의 위험이 낮다.

## Consequences

- 미니앱 WebView 에서 공개 API 를 직접 호출할 수 있다. (로컬 개발은 Vite dev 프록시로 처리하므로
  CORS 와 무관.)
- 쿠키 미사용이라 와일드카드 origin 이 자격증명 탈취로 이어지지 않는다.
- 향후 `/api/**` 에 인증(예: 토스 로그인 기반 JWT)이 추가되면, origin 을 명시 목록으로 좁히고
  `allowCredentials` 정책을 재검토해야 한다.
- 회귀 가드: `CorsConfigTest` 가 `/api/**` preflight 에 대해 `Access-Control-Allow-Origin` 응답을
  검증한다.
