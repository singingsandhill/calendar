# ADR-0002: 로케일 즉시 적용 — `setLocale` 에서 request attribute 캐시

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-19 |
| 도메인 | common |
| 관심사 | i18n |
| 관련 커밋 | `e8dde63` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

ADR-0001 (cookie-then-accept-language) 도입 후에도 사용자 신고: KO → EN 토글이
*첫 클릭에 적용되지 않고 새로고침 한 번 더 해야* 영문이 나타나는 버그.

원인: `LocaleChangeInterceptor` 가 `?lang=en` 을 보고 쿠키를 설정하지만, *같은 요청*
의 view 렌더링 단계에서 `LocaleResolver.resolveLocale(request)` 는 *기존 쿠키* 를
다시 읽어 KO 반환. 즉 한 요청 안에서 cookie set → cookie read 가 같은 요청 객체에
반영되지 않는 Spring 의 정상 동작.

## Decision — 무엇을 골랐나

`setLocale()` 에서 request attribute 에 새 로케일을 즉시 캐시.

- **`CookieThenAcceptLanguageLocaleResolver.setLocale(request, response, locale)`**:
  쿠키 설정 + `request.setAttribute("RESOLVED_LOCALE", locale)`.
- **`resolveLocale(request)`**: 먼저 request attribute 확인, 있으면 그 값 사용.
  없으면 cookie → header → ko 순서.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 토글 후 redirect 강제 | 단순 | URL 깜빡임, history.back() 시 두 단계 |
| 클라이언트 사이드 i18n (i18next 등) | 즉각 반응 | 서버 사이드 렌더링과 이중 관리 |
| **(선택) request attribute 캐시** | 같은 요청 안에서 일관, redirect 불필요 | — |

Spring 의 `SessionLocaleResolver` 도 비슷한 패턴 — 세션에 저장하고 같은 요청에서 즉시
반영. 우리는 stateless 정책이라 session 대신 request attribute.

## Consequences — 영향

- **긍정:**
  - 토글 한 번에 즉시 영문 페이지 렌더링.
  - 같은 요청 안에서 세 번 `resolveLocale` 호출되어도 일관 결과.
- **부정:**
  - request scope 캐시가 *해당 요청 종료* 까지만 — 다음 요청은 쿠키에서 다시 읽음
    (정상 동작).
- **후속:**
  - ADR-0003 (NumberFormat 그룹화) 가 같은 시기의 i18n 정합 작업.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/CookieThenAcceptLanguageLocaleResolver.java`
- 관련 docs: `docs/seo-evolution-playbook.md` (로케일 즉시 적용 단계)
- 관련 커밋: `git log -1 e8dde63`
