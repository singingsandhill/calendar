# ADR-0004: 사이트 크롬(헤더·푸터) 단일 fragment — 오버레이는 홈 한정, 푸터 1벌 재설계

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-09-27 |
| 도메인 | datedate |
| 관심사 | 프론트엔드 / UX |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 193 (구현은 179·180·181 에 흡수) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`fragments/header.html` 과 `fragments/footer.html` 에 같은 역할의 fragment 가 두 벌씩 있었다
(`header`/`header-minimal`, `footer`/`footer-minimal`). 두 헤더는 링크 클래스(`nav-link`/`nav-link-animated`)를
빼면 4줄만 달랐는데, 어느 벌을 쓸지가 페이지 성격이 아니라 **페이지가 만들어진 시점**으로 정해졌다.
예를 들어 색인·광고 콘텐츠 페이지인 `/insights/trends` 가 서비스용 벌을 썼다. 그 결과:

- **중복 비용:** nav 항목을 추가할 때마다 두 곳을 고쳤다(`92400e0`, `faf5150`, Commit 181). 계획 문서들도
  "header·header-minimal 둘 다"를 매번 적었고, `InternalLinkingTest` 도 두 변형을 각각 검사해야 했다.
- **고정 헤더 밑 브레드크럼 겹침:** `header-minimal` 은 `position: fixed` 투명 nav 인데, 본문 오프셋 보정은 홈 히어로
  (`.hero-fullscreen` padding-top 8rem)에만 있었다. 콘텐츠 페이지는 `padding-top: 2rem` 뿐이라 브레드크럼(y≈32px)이
  nav 띠(≈72px) 안에 그려졌다. 운영에서는 `/faq`·date-diff 가 이미 겹쳐 있었고, Commit 179(가시 breadcrumb,
  ADR common/seo/0012)가 이를 guides 12편과 use-case 상세로 확산시켰다. 데스크톱에서는 nav 링크와, 520px 폭에서는
  로고와 겹쳤다.
- **실제로 고정되지 않는 sticky:** 기본 헤더의 `.navbar { position: sticky }` 는 부모 `<header>` 높이가 nav 와 같아
  고정 범위가 0이었다. 스크롤하면 nav 가 사라졌다.
- **요소 선택자 누수:** 스코프 없는 `footer {}` 규칙이 owner 대시보드 카드의 `<footer class="schedule-card-actions">`
  에 margin·배경을 흘렸다.
- **푸터 품질:** 중앙정렬 칼럼 5개의 링크 수가 4/4/2/5/12개로 들쭉날쭉했고, 헤더 정렬선과도 어긋났다. 영문 섹션 제목은
  전부 대문자 변환됐고 위아래 여백은 6rem+2rem 이었다. 모바일(≤480)에서는 1열에 링크 27개가 쌓였다.
  Commit 180/181 이 저작권·문의 줄을 `.footer-sections` flex 안으로 옮기면서, 이 두 줄이 6·7번째 "칼럼"으로 렌더되기도 했다.

## Decision — 무엇을 골랐나

헤더·푸터를 각 1벌로 합치고, 모양 분기는 "홈 히어로 오버레이" 하나만 남긴다.

- **헤더:** fragment 는 `header` 하나다. 홈만 `<html th:with="navOverlay=true">` 로 투명 fixed(`navbar-minimal`)를 쓰고,
  나머지 전 페이지는 흰색 sticky(`navbar`)를 쓴다. sticky 는 `<header class="site-header">` 에 둔다.
  링크 클래스는 `nav-link` 하나이며, hover·모바일 규칙은 기존 다수 페이지(홈+콘텐츠 22개)의 모습(spring 밑줄,
  280px 메뉴, 1.125rem)을 기준으로 한다.
- **푸터:** fragment 는 `footer` 하나(`site-footer`)다.
  - 좌측 정렬이고, 컨테이너(1200px·동일 여백)를 헤더 `.navbar-content` 와 맞춘다.
  - 브랜드(워드마크·태그라인·문의) + 서비스·도움말·활용 사례·모임 노하우 4섹션으로 구성한다. 기사 12편은 2열이다(태블릿 3열).
  - 정책 링크는 구분선 아래 하단 줄로 옮긴다. 섹션 제목은 sentence case `<h2>` 다.
  - 모바일은 2열 그리드이며, 링크 행 높이는 ≥32px 이다.
  - 활용 사례·기사 링크는 전부 계속 노출한다. `UseCaseNavAdvice`·`GuideNavAdvice` 의 고아 페이지 방지는 그대로다.
- **FAQ 토글:** 인라인 스크립트 14벌을 `static/js/faq-toggle.js` 하나로 합친다. `/faq` 만 쓰던 `hidden` 속성 방식도
  다수 방식(`.faq-item.open` + `aria-expanded`)으로 맞춘다.
- **가드:** `SiteChromeConsistencyTest` 가 DateDate 대표 13페이지에서 다음을 고정한다.
  단일 헤더·푸터, 오버레이 홈 한정, 푸터 구간 안에서 연락처·정책 각 1회, 전 활용 사례·기사 링크.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. 두 벌 유지 + 콘텐츠 페이지에 오프셋 CSS 보정 | 겉모습 변화 최소 | 겹침만 가린다. 페이지 래퍼 클래스마다 보정 규칙이 필요해 새 페이지에서 또 빠진다. 중복 비용·sticky 불능·푸터 편차는 그대로 |
| B. 마크업 1벌 + 두 모양 파라미터 유지 | 중복 제거 | fixed 오버레이를 콘텐츠 페이지에 남기면 오프셋 보정이 여전히 필요하다(A 의 문제 승계) |
| **C. 1벌 + 오버레이는 홈 한정 (선택)** | 겹침이 구조적으로 사라지고, 호출부가 한 가지다 | 서비스 페이지도 스크롤 중 헤더가 계속 보인다(모바일 약 60px 상시 점유) — 사용자 결정으로 수용 |
| 푸터: 서비스 페이지만 compact 모드 | 작업 흐름 페이지가 가벼움 | 모드 2개가 남아 같은 편차가 재발한다. 사용자 결정으로 단일 푸터 |

## Consequences — 영향

- **긍정:**
  - nav·푸터 변경이 한 곳에서 끝난다.
  - 브레드크럼 겹침이 해소됐다(nav 하단 74px → 브레드크럼 106px, 모바일 60 → 92px, CDP 실측).
  - 헤더가 실제로 고정된다(1400px 스크롤 시 top 0).
  - 서비스 페이지 푸터에도 소개·FAQ·활용 사례·기사 링크가 생겨 내부 링크가 늘었다.
- **부정(의도된 시각 변화):**
  - 콘텐츠 페이지 헤더가 처음부터 흰색+그림자로 보인다.
  - 서비스 페이지의 hover 밑줄·모바일 메뉴 폭·글자 크기가 다수 페이지 기준으로 바뀐다.
  - owner 카드 액션 줄의 누수 margin 이 사라진다.
  - `/faq` 열림 아이콘이 `−` 대신 `+` 45° 회전(×)으로 바뀌고, 닫힌 답변이 접근성 트리에 남는다(다른 14페이지와 동일).
- **문서 영향:** ADR common/seo/0013 의 "기본 푸터 mailto + footer-minimal 하단 문의 줄" 서술은 이제 "단일 푸터 브랜드
  블록의 문의 줄"을 가리킨다. 연락처를 사이트 전역 푸터에 노출한다는 결정 자체는 불변이다.
- **후속:**
  - 브레드크럼 인라인 16벌과 "홈" 메시지 키 6개를 fragment 로 합치는 일(JSON-LD BreadcrumbList 와 원천 공유)
  - 에러 페이지 크롬 — 컨테이너 에러 경로는 MockMvc 로 검증할 수 없다
  - 짧은 페이지의 푸터 부유

## References

- 관련 코드: `src/main/resources/templates/fragments/header.html`, `fragments/footer.html`,
  `static/css/style.css`(`.site-header`, `.nav-link`, `SITE FOOTER` 블록), `static/js/faq-toggle.js`,
  `templates/index.html`(`th:with="navOverlay=true"`)
- 관련 테스트: `common/presentation/SiteChromeConsistencyTest`, `InternalLinkingTest`
- 선례: [ADR 0002 공유 Create 모달](0002-shared-create-schedule-modal.md) — 페이지 인라인 마크업의 fragment 추출
- 관련 ADR: [common/seo/0012](../../common/seo/0012-breadcrumb-hub-hierarchy.md), [common/seo/0013](../../common/seo/0013-publisher-identity-contact-and-sameas.md)
