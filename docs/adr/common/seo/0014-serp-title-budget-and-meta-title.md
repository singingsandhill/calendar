# ADR-0014: SERP 제목 예산 — ko 35자 / en 60자 가드 + 기사 `metaTitle` 분리

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-09-26 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / 검색 결과 표시 |
| 관련 커밋 | `docs/guides/git-commit.md` 참조 |
| 관련 문서 | [SEO 종합 점검 2026-09-24](../../../audit/seo-comprehensive-audit-2026-09-24.md) N10, §12 R2 |

## Context — 무엇이 문제였나

색인 페이지의 `<title>` 길이에 기준이 없었다. 설명(description)은 120~160자 가드
(`SeoServiceI18nTest.indexablePages_descriptionLengthInRange`)가 있지만 제목은 아무도 보지 않았다.

- 09-24 점검: 60자를 넘는 en 제목 4건(홈 65, insights 78, 기사 2편 78·79).
- 09-26 재검증: guides 기사 8편 추가로 **ko 12건 / en 12건**(26페이지 기준)으로 늘었다.
  새 기사 en 제목은 90~100자, ko 는 49~56자다.
- Google 은 제목을 약 600px 에서 자른다. 긴 제목은 검색 결과에서 핵심어 앞뒤가 잘려 보인다.

기사는 사정이 하나 더 있다. `guides.article.<slug>.title` 은 h1·Article `headline`·BreadcrumbList 이름·허브 카드에
같이 쓰이고, 본문 첫머리의 긴 제목은 내용을 잘 설명한다. 제목을 줄이면 이 모든 곳이 같이 짧아진다.

## Decision — 무엇을 골랐나

1. **제목 예산** — 브랜드 접미 `" | DateDate"` 포함, **ko 35자 / en 60자**.
   - 한글은 라틴 글자의 약 2배 폭이라 문자 수 상한을 로케일별로 둔다(≈600px 환산).
   - 가드: `SerpTitleBudgetTest.indexablePages_titleWithinSerpBudget` — 색인 26페이지 × ko/en 전수.
2. **기사 `metaTitle`** — 선택 키 `guides.article.<slug>.metaTitle` 이 있으면 `<title>`·og:title 에 쓰고,
   없으면 기존대로 `title` 을 쓴다(`SeoService.getGuideArticleSeo`, `mOrEmpty` 폴백).
   h1·headline·breadcrumb·허브 카드는 긴 `title` 그대로다. 12편 전부에 ko/en 쌍으로 둔다(카탈로그 패리티).
3. **기사 외 페이지** — 제목 키가 곧 SERP 제목이라 값을 직접 줄인다.
   - insights: ko/en 모두 `X | DateDate` 형식으로 정리 (브랜드가 문구 중간에 오던 형식 제거).
   - 홈 en: `DateDate - Free Group Scheduling with One Link`.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 제목 기준 없음 (현상 유지) | 변경 0 | 기사 추가 때마다 초과가 늘어난다 — 8편 추가로 4건 → 24건 |
| `title` 자체를 줄임 | 새 키 0 | h1·headline·허브 카드까지 짧아져 본문 첫머리의 설명력이 줄어든다 |
| 픽셀 폭 측정 가드 | 가장 정확 | 폰트·렌더링 의존 — 테스트 환경에서 재현 불안정. 문자 수 근사로 충분 |
| **(선택) 문자 수 예산 + 기사 `metaTitle` 선택 키** | SERP 만 짧게, 본문은 그대로 | — |

## Consequences — 영향

- **긍정:**
  - 색인 26페이지 × ko/en 전부 예산 안 (ko 최대 35, en 최대 60).
  - 기사 추가 시 제목이 길면 테스트가 바로 잡는다.
- **부정/트레이드오프:**
  - 기사 추가 비용이 키 2개(ko/en `metaTitle`) 늘었다.
  - 문자 수는 픽셀 폭의 근사라 경계값 근처는 실제 절단과 다를 수 있다.
  - Google 은 제목을 자체 재작성하기도 해서, 예산을 지켜도 표시가 보장되지는 않는다.
- **후속:**
  - 배포 후 GSC 실적 보고서에서 기사 CTR 변화를 관찰.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java` (`getGuideArticleSeo`)
  - `src/main/resources/messages*.properties` (`guides.article.<slug>.metaTitle`, `seo.insights.title`, `seo.home.title`)
  - `src/test/java/me/singingsandhill/calendar/datedate/application/service/SerpTitleBudgetTest.java`
- 관련 ADR: [0011 /guides 에디토리얼 허브](0011-guides-editorial-hub.md) (기사 키 네임스페이스)
