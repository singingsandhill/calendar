# ADR-0008: BreadcrumbList 는 2단계 고정 + 모든 `ListItem` 에 `item` URL

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-30 |
| 도메인 | common |
| 관심사 | SEO |
| 관련 커밋 | `docs/guides/git-commit.md` 참조 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

Google Search Console 이 **"'item' 입력란이 누락되었습니다 (경로: 'itemListElement')"** 오류를
보고했다. 최초 감지 2026-05-20, 영향 6개 URL, 최종 크롤링 2026-07-24~28. 해당 항목은 리치 결과에서
제외된다.

```
/use-cases/friend-meetup   /use-cases/team-meeting   /use-cases/travel-planning
/use-cases/study-group     /use-cases/club-activity   /tools/date-diff
```

Google BreadcrumbList 규격은 **마지막을 제외한 모든 `ListItem` 에 `item` 을 요구**한다 (마지막
항목의 `item` 은 선택). `SeoService` 의 breadcrumb 텍스트 블록은 7곳에 복붙돼 있었고, 전부
**position 1 에만 `item` 을 넣는 형태**였다. 그래서 결과가 계층 깊이에 따라 갈렸다.

- **2단계** (`/guide`, `/faq`, `/about`, `/privacy`, `/terms`, `/insights/trends`) —
  `item` 없는 크럼브가 *마지막* 이라 규격상 유효 → 오류 없음.
- **3단계** (`getUseCaseSeo`, `getDateDiffSeo`) — position 2 (`활용 사례` / `도구`) 가 `item` 없이
  *중간* 에 위치 → **오류**. 3단계인 메서드가 정확히 이 둘이고, GSC 가 신고한 6개 URL 과 일치한다.

중간 크럼브에 URL 을 넣어 고치는 길은 막혀 있었다. **`/use-cases`·`/tools` 허브 페이지가 존재하지
않는다** — `UseCaseController` 는 `@GetMapping("/{slug}")` 뿐이고, `HomeController` 는
`/tools/date-diff` 만 매핑하며, 두 템플릿 디렉토리에 index 도 없고 `SitemapService` 에도 없다.
URL 을 채우면 404 를 가리키는 breadcrumb 가 된다.

## Decision — 무엇을 골랐나

breadcrumb 을 **홈 → 현재 페이지 2단계로 고정**하고, **마지막 항목까지 포함해 모든 `ListItem` 에
`item` 절대 URL 을 채운다.**

- 3단계였던 `getUseCaseSeo` / `getDateDiffSeo` 의 중간 크럼브(`활용 사례` / `도구`) 제거.
- 7곳에 복붙돼 있던 breadcrumb 텍스트 블록을 `SeoService.breadcrumbJsonLd(leafName, leafPath)`
  헬퍼로 단일화 — 불변식이 한 곳에만 존재한다.
- `item` URL 은 `baseUrl + path` (ko 정규 URL). 같은 JSON-LD 안의 `"url"` 필드와 동일 규약이라
  `?lang=en` 페이지에서도 같은 엔티티를 가리킨다.
- 마지막 항목의 `item` 도 채운다 — 규격상 선택이지만, 계층이 하나 늘어나는 순간 기존 마지막 항목이
  중간이 되어 곧바로 같은 오류가 된다. 이번 사고가 정확히 그 형태였다.
- 참조가 사라진 메시지 키 `seo.breadcrumb.useCases` / `seo.breadcrumb.tools` 를 ko/en 양쪽에서 제거.

크럼브가 1개뿐인 `getHomeSeo` 와 `RunnerController` 는 이미 `item` 을 갖고 있어 변경 없음.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 중간 크럼브에 `/use-cases`·`/tools` URL 부여 | 3단계 계층 유지, 최소 diff | **해당 페이지가 없다.** breadcrumb 이 404 를 가리켜 오류를 품질 문제로 바꿔치기할 뿐 |
| `/use-cases`·`/tools` 허브 페이지 신설 후 URL 부여 | 계층 유지 + 색인 가능한 토픽 랜딩 페이지로 SEO 이득 | 컨트롤러·템플릿·sitemap·메시지·테스트가 따라붙는 신규 기능. 버그 수정 범위를 크게 벗어남 |
| 중간 크럼브가 자기 페이지 URL 을 가리키게 | 파일 하나만 수정 | 같은 URL 이 두 position 에 중복 — 계층 의미가 거짓이 됨 |
| **(선택) 2단계 축소 + 전 항목 `item`** | 이미 유효하게 동작하던 6개 페이지와 같은 형태로 수렴. 신규 라우트·404 위험 없음 | — |

허브 페이지 신설은 기각이 아니라 **보류**다. 만들 가치가 있는 페이지지만 이번 결정과 독립적이며,
만들 때 이 ADR 의 2단계 제약을 3단계로 되돌리면 된다 (헬퍼 한 곳만 수정).

## Consequences — 영향

- **긍정:**
  - GSC 리치 결과 오류 6건 해소. 배포 후 "수정 확인" 요청으로 재크롤링.
  - breadcrumb 형태가 전 페이지 1종으로 수렴 — 새 페이지가 같은 버그를 상속할 수 없다.
  - 회귀 가드: `SeoServiceI18nTest.breadcrumbList_everyItemHasUrl` — ko/en 양쪽, `UseCaseSlugs.ALL`
    전수로 모든 `ListItem` 의 `item` 존재·절대 URL·`position` 연속성을 검증한다. 기존
    `allJsonLd_validJsonBothLocales` 는 `readTree` 파싱만 해서 이 버그를 통과시켰다.
- **부정:**
  - `/tools/date-diff` 의 *화면* breadcrumb (`templates/tools/date-diff.html`) 은 여전히
    `홈 / 도구 / 날짜 계산기` 3단계를 표시해 구조화 데이터와 어긋난다. Google 권장사항상 일치가
    바람직하나 오류는 아니다 — UI 변경은 별도 판단으로 남긴다.
  - 검색 결과 breadcrumb 에서 `활용 사례` / `도구` 그룹 표기가 사라진다.
- **후속:**
  - `/use-cases`·`/tools` 허브 페이지를 만들면 3단계 복원 + 이 ADR Superseded 검토.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java`
    (`breadcrumbJsonLd`, 호출부 7곳)
  - `src/main/resources/templates/fragments/head.html` (`th:utext` 로 `seo.jsonLd()` 주입)
  - `src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java`
- 관련 ADR: [0001 SeoMetadata SSOT](0001-seo-metadata-as-ssot.md),
  [0007 콘텐츠 페이지 확장](0007-content-pages-for-adsense.md) (JSON-LD 로 BreadcrumbList 도입)
- 관련 docs: `docs/seo/evolution-playbook.md` §5 JSON-LD 진화
