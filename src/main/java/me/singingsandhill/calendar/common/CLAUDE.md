# Common Package

> 결정 근거: [`docs/adr/common/`](../../../../../../../docs/adr/common/) — SEO 7개,
> i18n 3개, error-handling 1개, security 1개 ADR.

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
| `/`, `/start`, `/api/**`, `/h2-console/**`, static assets, `/runners/**`, `/insights/**`, `/use-cases/**`, `/tools`, `/tools/**`, `/stock/**`, `/api/stock/**`, `/*`, `/*/*/*` | permitAll |
| 그 외 | authenticated |

CSRF: `/h2-console/**`, `/api/**`, runner admin 변경 엔드포인트 비활성. 폼 로그인 페이지:
`/runners/admin/login` → 로그아웃 후 `/runners`.

관련 ADR: [`common/security/0001`](../../../../../../../docs/adr/common/security/0001-runner-admin-only-form-login.md).

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
