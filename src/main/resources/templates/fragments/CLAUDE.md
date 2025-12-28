# Template Fragments

Reusable Thymeleaf fragments for consistent page structure.

**IMPORTANT:** Use explicit attribute selector syntax `[th:fragment='name']` to avoid conflicts with HTML element selectors.

## header.html

### `head(seo)` Fragment
Parameterized fragment for HTML `<head>` section with SEO metadata.

```html
<head th:replace="~{fragments/header :: head(${seo})}"></head>
```

**Contains:**
- Meta charset, viewport, SEO tags
- Dynamic title from seo parameter
- CSS link: `/css/style.css`
- Google Tag Manager, AdSense

### `header` Fragment
Default navigation bar component.

```html
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>
```

**Contains:**
- Navbar with logo
- Hamburger menu for mobile
- Navigation links

### `header-minimal` Fragment
Minimal navigation bar (Garriock style).

```html
<div th:replace="~{fragments/header :: [th:fragment='header-minimal']}"></div>
```

---

## footer.html

### `footer` Fragment
Default footer content section.

```html
<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
```

### `footer-minimal` Fragment
Minimal footer (Garriock style).

```html
<div th:replace="~{fragments/footer :: [th:fragment='footer-minimal']}"></div>
```

### `scripts` Fragment
JavaScript imports (use with `th:block`).

```html
<th:block th:replace="~{fragments/footer :: scripts}"></th:block>
```

**Includes:**
- `/js/toast.js`
- `/js/api.js`
- `/js/calendar.js`

---

## Usage Pattern

Every page template should include:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: head(${seo})}"></head>
<body>
    <div th:replace="~{fragments/header :: [th:fragment='header']}"></div>

    <!-- Page content -->

    <div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
    <th:block th:replace="~{fragments/footer :: scripts}"></th:block>
</body>
</html>
```

## Why Explicit Attribute Selectors?

When using `:: footer`, Thymeleaf might interpret it as:
1. Fragment name: `th:fragment="footer"`
2. CSS selector: `<footer>` HTML element

To avoid ambiguity when multiple fragments contain same HTML elements, use explicit attribute selectors: `[th:fragment='footer']`
