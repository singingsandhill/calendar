# Template Fragments

Use explicit attribute selector `[th:fragment='name']` to avoid conflicts with HTML element selectors (e.g., `<footer>` vs fragment named `footer`).

## Fragment Files

| File | Fragments | Description |
|------|-----------|-------------|
| `head.html` | `head(seo)` | `<head>` contents: meta tags, CSS preload, fonts, JSON-LD, AdSense (conditional) |
| `header.html` | `header` | 전 DateDate 페이지 공용 단일 nav (no head content). 홈만 `<html th:with="navOverlay=true">` 로 투명 fixed 오버레이, 그 외는 흰색 sticky(`<header class="site-header">`) ([ADR](../../../../../docs/adr/datedate/frontend/0004-single-site-chrome.md)) |
| `login-banner.html` | `login-banner` | 카카오 로그인 가치 제안 배너 + 로그인 후 사용법 3단계 (비로그인만, `sec:authorize`) — 홈·가이드에서 사용 |
| `footer.html` | `footer` | 전 DateDate 페이지 공용 단일 푸터 (no scripts) — 브랜드·서비스·도움말·활용 사례·모임 노하우 + 하단 정책 줄. 활용 사례·기사 링크는 `useCaseSlugs`·`guideSlugs` 모델 속성 순회 |
| `create-schedule-modal.html` | `modal` | 일정 생성 모달 — `owner/dashboard.html`·`schedule/view.html` 공유 ([ADR](../../../../../docs/adr/datedate/frontend/0002-shared-create-schedule-modal.md)) |
| `scripts.html` | `scripts` | JS file loading: toast.js, api.js, calendar.js |
| `ad-slot.html` | `leaderboard(adsEnabled)`, `infeed(adsEnabled)`, `rectangle(adsEnabled)` | AdSense slot placeholders, rendered only when `adsEnabled=true` |
| `gtm-noscript.html` | `gtm-noscript` | GTM noscript iframe 폴백 — GTM ID 는 `head.html` 의 JS 로더와 일치 유지. datedate·에러 페이지 `<body>` 직후 배치 |
| `error-head.html` | `error-head(title)` | 에러 페이지(`error/4xx`·`5xx`) 전용 head — 컨테이너 에러 경로(BasicErrorController)에는 `seo` 모델이 없어 `head(seo)` 대신 사용. noindex·GTM·비동기 폰트 포함, canonical/OG 는 에러 URL 에 무의미해 의도적 제외 |

## Usage Pattern

```html
<!-- In every page <head> -->
<head th:replace="~{fragments/head :: head(${seo})}"></head>

<!-- GTM noscript fallback (right after <body>) -->
<div th:replace="~{fragments/gtm-noscript :: gtm-noscript}"></div>

<!-- Navbar (홈 오버레이는 페이지 <html> 에 th:with="navOverlay=true") -->
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>

<!-- Ad slots (inside <main>, content pages only) -->
<div th:replace="~{fragments/ad-slot :: leaderboard(${seo.adsEnabled()})}"></div>
<div th:replace="~{fragments/ad-slot :: infeed(${seo.adsEnabled()})}"></div>

<!-- Footer -->
<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>

<!-- Scripts (before </body>) -->
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>
```

## Ad Slot Strategy

| Page | Ad slots | Rationale |
|------|----------|-----------|
| `/` (index) | None | CTA conversion priority — `adsEnabled=false` 라 스크립트 자체 미로드 (ADR common/seo/0010) |
| `/{ownerId}` (dashboard) | None | Personal page, trust |
| `/{ownerId}/{year}/{month}` (schedule) | None | Core task flow |
| `/insights/trends` | leaderboard + infeed | Content page, OK |
| `/use-cases/*` | infeed | Long-tail SEO pages |
| `/guide` | leaderboard | Post-content, pre-CTA |
| `/guides` (허브) | None | 내비게이션 페이지 — `adsEnabled=false` (ADR common/seo/0011) |
| `/guides/*` (기사) | infeed | 장문 에디토리얼 — 본문 뒤, CTA 앞 |
