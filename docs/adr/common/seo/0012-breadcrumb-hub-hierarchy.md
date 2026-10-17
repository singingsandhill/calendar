# ADR-0012: BreadcrumbList 허브 계층 복원 — 활용 사례·모임 노하우 상세는 3단계 + 가시 breadcrumb

| 항목 | 값 |
|---|---|
| 상태 | Accepted — [0008](0008-breadcrumb-item-on-every-listitem.md) 의 "2단계 고정" 결정을 대체 (전 항목 `item` 규칙은 유지) |
| 날짜 | 2026-09-24 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / 내비게이션 |
| 관련 커밋 | `docs/guides/git-commit.md` 참조 |
| 관련 문서 | [SEO 종합 점검 2026-09-24](../../../audit/seo-comprehensive-audit-2026-09-24.md) N1·N2 |

## Context — 무엇이 문제였나

ADR-0008 은 BreadcrumbList 를 **홈 → 현재 페이지 2단계로 고정**했다. 당시 중간 크럼브가 가리킬
`/use-cases`·`/tools` 허브 페이지가 존재하지 않아, 중간 항목에 URL 을 넣으면 404 를 가리키게 됐기
때문이다. 0008 은 허브 신설을 기각이 아니라 **보류**로 남기고 "허브를 만들면 3단계 복원 + 이 ADR
Superseded 검토" 를 후속 항목으로 적어 두었다.

그 전제가 사라졌다.

- `/use-cases` 허브 인덱스 (2026-09-08, AdSense 3차 진단 §4-4)
- `/guides` 모임 노하우 허브 + 기사 4편 (2026-09-10~12, [ADR-0011](0011-guides-editorial-hub.md))

그런데 2026-09-24 점검에서 두 가지가 함께 드러났다.

1. **구조화 데이터가 실제 계층을 말하지 않는다.** 활용 사례 5편·기사 4편의 BreadcrumbList 가 여전히
   `홈 → 페이지` 라, 검색 결과 breadcrumb 에 허브 그룹이 드러나지 않는다.
2. **허브가 고아다.** `/use-cases`·`/guides`(그리고 `/faq`)로 들어오는 `<a>` 링크가 사이트 전체에
   0개였다 — 라이브 38 URL 크롤로 실측. 상세 페이지에는 가시 breadcrumb 도 없어서, 허브는 사이트맵으로만
   발견 가능했다. 사용자·AdSense 리뷰어의 탐색 경로에서 허브가 빠져 있다.

## Decision — 무엇을 골랐나

허브가 있는 두 콘텐츠 계열의 상세 페이지는 **홈 → 허브 → 페이지 3단계** 로 JSON-LD 와 화면을 함께
맞춘다.

- `SeoService.breadcrumbJsonLd` 를 "홈 + (name, path) 크럼 목록" 루프 직렬화로 일반화한다.
  **모든 `ListItem` 에 `item` 을 채우는 0008 의 불변식은 이 한 메서드에 그대로 남는다.**
  2단계 호출부 8곳은 오버로드로 변경 없이 유지한다.
- `getUseCaseSeo(slug)` → `홈 → 활용 사례(/use-cases) → 슬러그`,
  `getGuideArticleSeo(slug)` → `홈 → 모임 노하우(/guides) → 기사`. 허브 이름은 기존
  `seo.breadcrumb.useCases` / `seo.breadcrumb.guides` 키.
- 같은 계층의 **가시 breadcrumb** 를 `use-cases/detail.html` 과 기사 템플릿 4개에 넣는다
  (`faq.html` 과 같은 `nav.breadcrumb` 마크업·기존 CSS). 허브 링크는 `localeLinks.href` 로 로케일 유지.
  현재 항목 라벨은 짧은 `navLabel`, JSON-LD 의 마지막 `name` 은 기존대로 전체 제목.
- 허브 페이지 자신(`/use-cases`, `/guides`)과 나머지 2단계 페이지는 그대로다. date-diff 는 `/tools` 가
  308 리다이렉트라 허브가 아니므로 2단계 유지 (0008 의 화면/데이터 불일치 기록은 그대로 유효).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 2단계 유지 | 변경 0 | 0008 이 복원을 막았던 유일한 이유(허브 부재)가 사라졌다. 허브 그룹 신호를 계속 버린다 |
| JSON-LD 만 3단계 | 코드 1곳 | 화면에 없는 계층을 구조화 데이터로만 주장 — 0008 이 date-diff 에서 이미 "일치가 바람직" 으로 기록한 불일치를 새로 9페이지에 만든다. 허브 고아 문제도 그대로 |
| 헬퍼를 3단계 전용으로 복제 | 단순 | 불변식(전 항목 `item`)이 두 곳으로 갈라진다 — 0008 사고의 원인이 7곳 복붙이었다 |
| **(선택) 헬퍼 일반화 + 가시 breadcrumb** | 구조화 데이터·화면·내부 링크가 한 계층으로 수렴. 신규 메시지 키 0 | — |

## Consequences — 영향

- **긍정:**
  - 활용 사례·기사 9페이지 × ko/en 의 BreadcrumbList 가 허브를 포함한다 — 검색 결과 breadcrumb 에
    그룹 표기가 돌아온다 (데스크톱).
  - 상세 페이지마다 허브로 가는 본문 내 링크가 생겨 허브가 고아에서 벗어난다
    (`/faq` 는 푸터, `/guides` 는 헤더 내비로 별도 해소 — 점검 보고서 N1).
  - 회귀 가드: `SeoServiceI18nTest.breadcrumbList_useCaseAndGuideArticlesIncludeHub`(3단계·허브 이름/URL),
    `UseCaseLocaleRenderingTest.detailBreadcrumbLinksHub`·`GuidesLocaleRenderingTest.articleBreadcrumbLinksHub`
    (가시 breadcrumb·로케일 링크), `InternalLinkingTest`(사이트맵 전 URL 의 내부 유입 링크 ≥1).
    기존 `breadcrumbList_everyItemHasUrl` 은 3단계에서도 그대로 통과한다.
- **부정/트레이드오프:**
  - 기사 템플릿이 기사별 전용이라(ADR-0011) breadcrumb 마크업이 4개 파일에 반복된다. 0011 이 적어 둔
    "공통 마크업 변경 시 4개 파일" 비용의 첫 사례 — 기사 10편을 넘으면 fragment 추출 재검토.
- **후속:**
  - 배포 후 Rich Results Test 로 use-case 1편·기사 1편의 BreadcrumbList 3항목 인식을 확인.
  - 새 허브(예: `/tools` 인덱스)를 만들면 같은 헬퍼에 크럼 하나를 더하는 것으로 확장한다.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java`
    (`breadcrumbJsonLd`, `getUseCaseSeo`, `getGuideArticleSeo`)
  - `src/main/resources/templates/use-cases/detail.html`, `src/main/resources/templates/guides/<slug>.html`
  - `src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java`
  - `src/test/java/me/singingsandhill/calendar/common/presentation/InternalLinkingTest.java`
- 관련 ADR: [0008 BreadcrumbList 2단계 + 전 항목 item](0008-breadcrumb-item-on-every-listitem.md) (부분 대체),
  [0011 /guides 에디토리얼 허브](0011-guides-editorial-hub.md)
- 관련 docs: [SEO 종합 점검 2026-09-24](../../../audit/seo-comprehensive-audit-2026-09-24.md)
