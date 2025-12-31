# Common Package

Shared components used across all modules: exceptions, configuration, and presentation utilities.

## Structure

```
common/
├── application/
│   └── exception/
│       └── BusinessException.java
├── infrastructure/
│   └── config/
│       ├── JpaConfig.java
│       ├── WebConfig.java
│       └── SecurityConfig.java
└── presentation/
    ├── api/
    │   └── GlobalExceptionHandler.java
    ├── controller/
    │   └── StaticResourceController.java
    └── dto/
        ├── ErrorResponse.java
        └── SeoMetadata.java
```

## Exception Handling

### BusinessException
Base class for all business exceptions.

```java
public abstract class BusinessException extends RuntimeException {
    public abstract HttpStatus getStatus();
    public abstract String getCode();
}
```

**Usage Pattern**:
```java
public class RunNotFoundException extends BusinessException {
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getCode() {
        return "RUN_NOT_FOUND";
    }
}
```

### GlobalExceptionHandler
`@RestControllerAdvice` that catches all exceptions.

| Exception Type | HTTP Status | Response |
|----------------|-------------|----------|
| BusinessException | From exception | ErrorResponse |
| MethodArgumentNotValidException | 400 | Validation errors |
| Exception | 500 | Generic error |

### ErrorResponse (Record)
```java
String code
String message
LocalDateTime timestamp
```

## Configuration

### JpaConfig
- `@EnableJpaAuditing` for createdAt auto-population
- Entity scan configuration

### WebConfig
- Static resource handling
- CORS configuration (if needed)
- View controller mappings

### SecurityConfig
Spring Security configuration.

| Path Pattern | Access |
|--------------|--------|
| `/runners/admin/**` | ROLE_ADMIN |
| `/api/**` | permitAll |
| `/h2-console/**` | permitAll (dev only) |
| `/**` | permitAll |

## Presentation Utilities

### StaticResourceController
Serves static files (robots.txt, sitemap.xml, ads.txt).

### SeoMetadata (Record)
SEO metadata for pages.

```java
String title
String description
String keywords
String canonicalUrl
String ogImage
String robots        // "index, follow" or "noindex, nofollow"
```

**Usage in Controller**:
```java
model.addAttribute("seo", SeoMetadata.builder()
    .title("Page Title")
    .description("Page description")
    .build());
```

**Usage in Template**:
```html
<head th:replace="~{fragments/header :: seo(${seo})}">
```
