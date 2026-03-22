# Common Package

## BusinessException Pattern

Abstract base: subclasses implement `getStatus()` (HttpStatus) and `getCode()` (String).

## GlobalExceptionHandler

| Exception Type | HTTP Status |
|----------------|-------------|
| BusinessException | From exception's getStatus() |
| MethodArgumentNotValidException | 400 |
| IllegalArgumentException | 400 |
| Exception | 500 |

## SecurityConfig Access Rules

| Path Pattern | Access |
|--------------|--------|
| `/runners/admin/**` | ROLE_ADMIN |
| `/api/**`, `/h2-console/**`, `/**` | permitAll |
