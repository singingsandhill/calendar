# Common Package

## BusinessException Pattern

Abstract base: subclasses implement `getStatus()` (HttpStatus) and `getCode()` (String).

## Exception Handlers (Two-Layer)

**GlobalExceptionHandler** (`presentation/api/`) — applies to `@RestController` only → returns JSON `ErrorResponse`:

| Exception Type | HTTP Status |
|----------------|-------------|
| BusinessException | From exception's getStatus() |
| MethodArgumentNotValidException | 400 |
| IllegalArgumentException | 400 |
| MethodArgumentTypeMismatchException | 400 |
| NoResourceFoundException | 404 |
| Exception | 500 |

**MvcExceptionHandler** (`presentation/controller/`) — applies to `@Controller` only → renders Thymeleaf error templates:

| Exception Type | View |
|----------------|------|
| BusinessException (4xx status) | `error/4xx` |
| BusinessException (5xx status) | `error/5xx` |
| Exception | `error/5xx` |

## SecurityConfig Access Rules

| Path Pattern | Access |
|--------------|--------|
| `/runners/admin/**` | ROLE_ADMIN |
| `/api/**`, `/h2-console/**`, `/**` | permitAll |
