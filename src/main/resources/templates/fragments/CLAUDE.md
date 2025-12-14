# Template Fragments

Reusable Thymeleaf fragments for consistent page structure.

## header.html

### `head(title)` Fragment
Parameterized fragment for HTML `<head>` section.

```html
<head th:replace="~{fragments/header :: head('Page Title')}">
```

**Contains:**
- Meta charset, viewport
- Dynamic title: `th:text="${title}"`
- CSS link: `/css/style.css`

### `header` Fragment
Navigation bar component.

```html
<div th:replace="~{fragments/header :: header}">
```

**Contains:**
- Navbar with logo
- Logo links to home (`/`)

---

## footer.html

### `footer` Fragment
Footer content section.

```html
<div th:replace="~{fragments/footer :: footer}">
```

### `scripts` Fragment
JavaScript imports (use with `th:block`).

```html
<th:block th:replace="~{fragments/footer :: scripts}">
```

**Includes:**
- `/js/api.js`
- `/js/calendar.js`

---

## Usage Pattern

Every page template should include:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: head('Title')}"></head>
<body>
    <div th:replace="~{fragments/header :: header}"></div>

    <!-- Page content -->

    <div th:replace="~{fragments/footer :: footer}"></div>
    <th:block th:replace="~{fragments/footer :: scripts}"></th:block>
</body>
</html>
```
