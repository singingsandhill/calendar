# ADR-0001: Runner 어드민 전용 폼 로그인 + 나머지 permitAll

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2025-12-27 (러닝 크루 페이지 도입) |
| 도메인 | common |
| 관심사 | 보안 |
| 관련 커밋 | `bfd6b8d` 이후 |
| 관련 이슈 | #17 |

## Context — 무엇이 문제였나

서비스 다섯 모듈 중 *유일하게 인증이 필요한 영역* 이 Runner 어드민 (출석 삭제, 런
편집). DateDate / Trading / Stock / 인사이트는 모두 anonymous OR 자체 토큰(예:
ownerId 매개변수) 으로 운영. 단일 인증 정책으로 묶기에는 모듈 간 신뢰 모델이 다르다.

추가 제약:
- Spring Security 가 SecurityFilterChain 으로 모든 요청을 통과하므로 *어떤 경로를
  permitAll, 어떤 경로를 인증 강제* 할지 정확히 정해야 함.
- H2 console (`/h2-console/**`) 은 dev 환경에서 인증 없이 사용 — CSRF/X-Frame-Options
  완화 필요.

## Decision — 무엇을 골랐나

경로 패턴 화이트리스트 + Runner 어드민만 ROLE_ADMIN.

- **`/runners/admin/**`** → `ROLE_ADMIN` 폼 로그인.
- **`/runners/admin/login`** → permitAll (로그인 페이지 자체).
- **나머지 모든 경로** (`/runners/**`, `/insights/**`, `/stock/**`, `/api/**`, `/h2-console/**`,
  static assets, `/**`) → permitAll.
- **CSRF 비활성** — `/h2-console/**`, `/api/**`, runner admin 변경 엔드포인트.
- **로그인 흐름** — `/runners/admin/login` → 성공 시 `/runners/admin/...`, 로그아웃 시
  `/runners` 리다이렉트.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 모든 경로에 폼 로그인 | 균일 | DateDate/Trading 의 무인증 핵심 가치 깨짐 |
| OAuth (Google) | UX 좋음 | 단일 어드민(서비스 운영자) 인증에 과한 인프라 |
| API 토큰 인증 | API 친화 | 어드민 페이지(HTML) 은 폼 로그인이 자연 |
| **(선택) Runner 어드민 폼 로그인 + 나머지 permitAll** | 신뢰 모델 차이 그대로 반영 | — |

CSRF 비활성 범위는 *변경 엔드포인트만* — Spring Security 의 폼 로그인은 CSRF 토큰
사용 (어드민 로그인 자체는 보호). API 는 Same-Origin 정책이 다른 가드 (예: 서버 사이드
검증) 가 있어 CSRF 필요성 낮음.

## Consequences — 영향

- **긍정:**
  - 모듈별 신뢰 모델 자연스레 표현.
  - 어드민 외 모든 경로에 인증 부담 없음.
- **부정:**
  - permitAll 범위가 넓어 향후 모듈 추가 시 경로 패턴 갱신 필요.
  - CSRF 비활성 범위 결정이 보안 검토 부담 — 변경 시마다 재평가.
- **후속:**
  - Runner 어드민 비밀번호는 환경변수 (`RUNNER_ADMIN_PASSWORD`) 로 주입, BCrypt 해시.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java`
  - `src/main/resources/templates/runners/admin/login.html`
- 관련 docs: `CLAUDE.md` (Security 섹션)
- 관련 커밋: `git log -1 bfd6b8d`
