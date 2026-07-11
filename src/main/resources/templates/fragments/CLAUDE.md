# Template Fragments

Use explicit attribute selector `[th:fragment='name']` to avoid conflicts with HTML element selectors (e.g., `<footer>` vs fragment named `footer`).

## Fragment Files

| File | Fragments | Description |
|------|-----------|-------------|
| `head.html` | `head(seo)` | `<head>` contents: meta tags, CSS preload, fonts, JSON-LD, AdSense (conditional) |
| `header.html` | `header`, `header-minimal` | Navigation bars only (no head content) |
| `footer.html` | `footer`, `footer-minimal` | Footer markup only (no scripts) |
| `scripts.html` | `scripts` | JS file loading: toast.js, api.js, calendar.js |
| `ad-slot.html` | `leaderboard(adsEnabled)`, `infeed(adsEnabled)`, `rectangle(adsEnabled)` | AdSense slot placeholders, rendered only when `adsEnabled=true` |

## Usage Pattern

```html
<!-- In every page <head> -->
<head th:replace="~{fragments/head :: head(${seo})}"></head>

<!-- Navbar -->
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>
<!-- or minimal variant for content pages -->
<div th:replace="~{fragments/header :: [th:fragment='header-minimal']}"></div>

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
| `/` (index) | None | CTA conversion priority |
| `/{ownerId}` (dashboard) | None | Personal page, trust |
| `/{ownerId}/{year}/{month}` (schedule) | None | Core task flow |
| `/insights/trends` | leaderboard + infeed | Content page, OK |
| `/use-cases/*` | infeed | Long-tail SEO pages |
| `/guide` | leaderboard | Post-content, pre-CTA |
