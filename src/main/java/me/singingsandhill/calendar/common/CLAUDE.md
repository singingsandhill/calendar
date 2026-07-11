# Common Package

> 결정 근거: [`docs/adr/common/`](../../../../../../../docs/adr/common/) — SEO 7개,
> i18n 3개, error-handling 1개, security 4개 ADR.

## BusinessException Pattern

Abstract base: subclasses implement `getStatus()` (HttpStatus) and `getCode()` (String).

## Exception Handlers (Two-Layer)

**GlobalExceptionHandler** (`presentation/api/`) — applies to `@RestController` only → returns JSON `ErrorResponse`:

| Exception Type | HTTP Status |
|----------------|-------------|
| BusinessException | From exception's getStatus() |
| MethodArgumentNotValidException | 400 |
| IllegalArgumentException | 400 |
| MethodArgumentTypeMismatchException | 400 |
| NoResourceFoundException | 404 |
| Exception | 500 |

**MvcExceptionHandler** (`presentation/controller/`) — applies to `@Controller` only → renders Thymeleaf error templates:

| Exception Type | View |
|----------------|------|
| BusinessException (4xx status) | `error/4xx` |
| BusinessException (5xx status) | `error/5xx` |
| Exception | `error/5xx` |

## SecurityConfig Access Rules

`SecurityConfig.java` 의 `requestMatchers` 순서가 중요. 더 구체적인 ADMIN 규칙을
포괄 permitAll 규칙 뒤에 두면 ADMIN 보호가 무력화된다.

| Path Pattern | Access |
|--------------|--------|
| `/runners/admin/**` | ROLE_ADMIN (단, `/runners/admin/login` 은 permitAll) |
| `/api/trading/**`, `/trading`, `/trading/**` | ROLE_ADMIN (봇 제어·실주문·제어 대시보드, [ADR 0003](../../../../../../../docs/adr/common/security/0003-admin-only-trading-control-api.md)) |
| `/me`, `/recap`, `/recap/**`, `/api/me/**` | ROLE_USER (카카오 로그인, [ADR 0004](../../../../../../../docs/adr/common/security/0004-kakao-oauth2-login.md)) — 단 `/recap/share/**` 는 permitAll |
| `/login`, `/oauth2/**`, `/login/oauth2/**` | permitAll |
| `/`, `/start`, `/api/**`, `/h2-console/**`, static assets, `/runners/**`, `/insights/**`, `/use-cases/**`, `/tools`, `/tools/**`, `/stock/**`, `/api/stock/**`, `/*`, `/*/*/*` | permitAll |
| 그 외 | authenticated |

트레이딩 ROLE_ADMIN·카카오 ROLE_USER 규칙은 포괄 `permitAll`(`/api/**`, `/*`)보다 **먼저** 선언해야 함(첫 매칭 우선).
회귀 가드: `TradingApiSecurityTest`, `DatedateAuthSecurityTest`.

**진입점 분리:** 어드민 경로(`/runners/admin/**`, `/trading*`, `/api/trading/**`)는 `defaultAuthenticationEntryPointFor`
로 먼저 매핑하고, 그 외 전체는 마지막 `AnyRequestMatcher.INSTANCE` 매핑(카카오 `/login`)으로 처리한다.
`ExceptionHandlingConfigurer#authenticationEntryPoint(...)` 를 별도로 호출하면 앞서 등록한 매핑이 통째로
무시되므로 사용하지 않는다.

**로그아웃 2계열:** `POST /runners/admin/logout` → `/runners`, `POST /logout`(카카오 세션) → `/`.

CSRF: `/h2-console/**`, `/api/**`, runner admin 변경 엔드포인트 비활성. `/api/trading/**`·`/api/me/**` 는
`ROLE_ADMIN`/`ROLE_USER` + 무자격증명 CORS 로 무인증/교차출처 노출을 차단(CSRF 토큰화는 후속 과제). 폼 로그인 페이지:
`/runners/admin/login` → 로그아웃 후 `/runners`.

CORS: `CorsConfig` 가 `/api/**` 한정 `CorsConfigurationSource` 빈을 제공하고 `SecurityConfig` 가
`.cors()` 로 활성화. 앱인토스 미니앱(다른 origin)의 공개 API 호출 허용 (무자격증명, allowedOriginPatterns `*`).

관련 ADR: [`common/security/0001`](../../../../../../../docs/adr/common/security/0001-runner-admin-only-form-login.md),
[`common/security/0002`](../../../../../../../docs/adr/common/security/0002-cors-for-apps-in-toss-miniapp.md),
[`common/security/0004`](../../../../../../../docs/adr/common/security/0004-kakao-oauth2-login.md).

## i18n / SEO 메모

- `CookieThenAcceptLanguageLocaleResolver` — 쿠키 → Accept-Language → ko 폴백, 1년
  쿠키, SameSite=Lax. `setLocale` 에서 request attribute 캐시로 같은 요청 안에서 즉시
  반영 ([ADR](../../../../../../../docs/adr/common/i18n/0002-immediate-locale-application.md)).
- `SeoMetadata` 가 SSOT — 모든 페이지 메타·hreflang·JSON-LD 가 한 곳에서 결정
  ([ADR](../../../../../../../docs/adr/common/seo/0001-seo-metadata-as-ssot.md)).
- `SitemapService` — lastmod 는 `BuildProperties` (정적) 또는 `findLatestActivity()`
  (인사이트), ISO 8601 KST, XML escape 필수
  ([ADR](../../../../../../../docs/adr/common/seo/0003-trustworthy-sitemap-lastmod.md)).
- `IndexNowService` — `indexnow.enabled=true` 일 때만 매일 03:30 KST `SitemapService`
  URL 들을 `api.indexnow.org` 에 POST. 4xx/5xx/네트워크 예외는 모두 fail-soft (WARN 만).
  키 파일은 `static/1dfcb4404e1d4f6fae3423fd163f97b8.txt`, 같은 호스트로 서빙되어야 함.
