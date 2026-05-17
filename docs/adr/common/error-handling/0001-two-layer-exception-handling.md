# ADR-0001: REST(`GlobalExceptionHandler`) / MVC(`MvcExceptionHandler`) 2-layer 예외 처리

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-16 (강화) ~ 2026-04-13 (에러 페이지 도입) |
| 도메인 | common |
| 관심사 | 에러 처리 |
| 관련 커밋 | `f1af181`, `af3d212` |
| 관련 이슈 | #16 |

## Context — 무엇이 문제였나

Spring `@RestControllerAdvice` 단일 핸들러로 모든 예외를 처리하면 *MVC 뷰 응답 케이스*
가 망가진다. JSON 으로 `{code, message}` 를 받아야 하는 API 와 HTML `error/4xx`
템플릿을 받아야 하는 페이지가 같은 핸들러에 묶이면, Accept 헤더 분기를 매 메서드에
넣어야 한다 — 빠뜨림 사고가 잦았다.

추가로 모듈별 비즈니스 예외(`StockException`, `TradingException`, `RunnerException` 등)
가 *공통 예외 처리에 자기 status/code 를 어떻게 알릴지* 구조 부재.

## Decision — 무엇을 골랐나

응답 매체별로 핸들러를 분리하고, 비즈니스 예외에 표준 인터페이스 부여.

- **Layer 1 — REST APIs** (`GlobalExceptionHandler`, `@RestControllerAdvice`):
  - `BusinessException` → `exception.getStatus()` HTTP, `{code, message}` JSON.
  - `MethodArgumentNotValidException` / `IllegalArgumentException` /
    `MethodArgumentTypeMismatchException` → 400.
  - `NoResourceFoundException` → 404.
  - `Exception` → 500.
- **Layer 2 — MVC views** (`MvcExceptionHandler`, `@ControllerAdvice`):
  - `BusinessException` 4xx → `error/4xx` 템플릿.
  - `BusinessException` 5xx → `error/5xx` 템플릿.
  - `Exception` → `error/5xx`.
- **`BusinessException` abstract base** — 서브클래스가 `getStatus()` (HttpStatus),
  `getCode()` (String) 구현. 새 모듈 예외는 반드시 이 베이스 상속.
- **`ErrorResponse` record** — `{ code, message }` JSON 구조 표준.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 단일 핸들러 + Accept 분기 | 1개 클래스 | 매 메서드에 분기 → 빠뜨림 |
| 모듈별 핸들러 분리 | 도메인 격리 | 공통 예외(타입 불일치 등) 중복 |
| **(선택) 매체별 2-layer + abstract base** | DRY, 도메인 위임 가능 | — |

`@RestControllerAdvice` (REST) 가 `@ControllerAdvice` (MVC) 보다 우선순위가 높아 요청
헤더에 따라 자동 라우팅 — 별도 분기 불필요.

## Consequences — 영향

- **긍정:**
  - 새 모듈 예외 추가 시 `BusinessException` 상속 + status/code 정의면 끝.
  - 4xx/5xx 페이지 분기 자동.
  - JSON / HTML 응답 한 곳에서 일관 처리.
- **부정:**
  - 두 핸들러가 동일 예외 매핑을 *각각 정의* — 변경 시 양쪽 갱신 필요. 새 예외
    매핑은 보통 한 쪽만 추가하면 충분 (404는 양쪽 다).
- **후속:**
  - 에러 페이지 i18n (`error/4xx.html`, `error/5xx.html`) 가 ADR-0001 (i18n) 의 메시지
    소스 사용.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/presentation/api/GlobalExceptionHandler.java`
  - `src/main/java/me/singingsandhill/calendar/common/presentation/controller/MvcExceptionHandler.java`
  - `src/main/java/me/singingsandhill/calendar/common/application/exception/BusinessException.java`
  - `src/main/java/me/singingsandhill/calendar/common/presentation/dto/response/ErrorResponse.java`
  - `src/main/resources/templates/error/4xx.html`, `5xx.html`
- 관련 docs: `CLAUDE.md` (Exception Handling 섹션)
- 관련 커밋: `git log -1 f1af181`, `git log -1 af3d212`
