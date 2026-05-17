# ADR-0001: SeoMetadata record 를 SEO 단일 진실 공급원(SSOT) 으로

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2025-12-14 |
| 도메인 | common |
| 관심사 | SEO |
| 관련 커밋 | `cdb552e` |
| 관련 이슈 | #9 |

## Context — 무엇이 문제였나

MVP 출시 시점, SEO 메타(title, description, og:*, robots, canonical, JSON-LD) 가 각
컨트롤러에서 즉석 작성되고 있었다. 한 페이지의 메타가 두세 곳(컨트롤러 + 템플릿
인라인 + 정적 fragment) 에 분산되면 hreflang/og:locale/JSON-LD 같은 cross-cutting
요구가 추가될 때 *모든 페이지를 다 고쳐야* 하는 상황이 예고됐다.

## Decision — 무엇을 골랐나

`SeoMetadata` record + `SeoService` 도입 — 모든 SEO 메타가 한 곳에서 생성·주입됨.

- `SeoMetadata` record (title, description, og*, robots, canonical, hreflang, jsonLd)
  를 신설.
- `SeoService.buildMetadata(page, locale, ...)` 가 페이지별 정책을 한 곳에서 결정.
- 컨트롤러는 `model.addAttribute("seo", seoService.buildMetadata(...))` 로 주입,
  템플릿(`fragments/head.html`)은 `seo.*` 만 렌더링.
- `StaticResourceController` — robots/sitemap/manifest/favicon/ads.txt 같은 표준
  경로를 명시적 `@GetMapping` 으로 노출하여 라우팅 우선순위 매칭 사고 방지.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 컨트롤러 인라인 메타 | 단순 | hreflang/og:locale/JSON-LD 추가 시 회귀 ↑ |
| Thymeleaf fragment 변수만 사용 | 템플릿 친화 | 분기 로직(canonical 페이지별 다름)이 템플릿에 들어가 복잡 |
| **(선택) record + 서비스** | 도메인 분리, 테스트 가능, 확장 용이 | — |

이후 hreflang(ADR-0004), 사이트맵 lastmod(ADR-0003) 같은 결정이 *컨트롤러 수정 없이*
서비스 한 곳에서 끝난 것이 이 선택의 가치를 입증.

## Consequences — 영향

- **긍정:**
  - 후속 SEO 결정이 한 클래스 수정으로 모든 페이지에 적용.
  - SEO 단위 테스트 가능 (`SeoServiceI18nTest` 등).
- **부정:**
  - 신규 페이지 추가 시 컨트롤러에서 `seoService.buildMetadata(page=...)` 호출 강제 —
    누락 시 메타 없이 출력 (가드: `head.html` 의 `th:if`).
- **후속:**
  - ADR-0002 (HTTPS 통일), ADR-0003 (sitemap lastmod), ADR-0004 (hreflang) 모두 이
    SSOT 위에 쌓임.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/presentation/dto/SeoMetadata.java`
  - `src/main/java/me/singingsandhill/calendar/common/presentation/controller/StaticResourceController.java` (참고)
- 관련 docs: `docs/seo-evolution-playbook.md#21-foundation-9-무엇을-미리-갖추었는가`
- 관련 커밋: `git log -1 cdb552e`
