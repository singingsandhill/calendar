# ADR-0002: 출석 삭제는 어드민 전용

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-11 |
| 도메인 | runner |
| 관심사 | 도메인 모델 / 권한 |
| 관련 커밋 | `2fb453d` |
| 관련 이슈 | #17 |

## Context — 무엇이 문제였나

ADR-0001 (anonymous 생성/마킹) 정책 하에서, 출석 위변조 가능성을 어떻게 *복구* 할지
결정 필요.

문제 시나리오:
- 누군가 다른 멤버 이름으로 잘못 마킹.
- 결제/관리 정확성을 위해 잘못된 마킹 제거 필요.
- 운영자가 직접 SQL 로 row 삭제하는 것보다 안전한 UI 채널이 필요.

## Decision — 무엇을 골랐나

출석 삭제 기능을 어드민 페이지로 노출.

- **어드민 페이지에 출석 삭제 버튼** — `/runners/admin/...`.
- **권한:** ROLE_ADMIN 폼 로그인 (ADR-0001 common/security).
- **CSRF 비활성** (admin 변경 엔드포인트, ADR-0001 common/security 정책 준수).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| anonymous 삭제 허용 | UX 단순 | 위변조 + 위변조 복구 모두 가능 → 관리 신뢰성 0 |
| DB 직접 수정 | UI 부담 0 | 운영자 매번 SSH/SQL — 사고 위험 |
| **(선택) 어드민 UI 삭제** | 안전 + 가시성 | — |

## Consequences — 영향

- **긍정:**
  - 위변조 복구 채널 확보.
  - DB 직접 수정 회피.
- **부정:**
  - 어드민 1명 의존 — 운영자 부재 시 처리 지연.
- **후속:**
  - 향후 *멤버 본인 자기 마킹 삭제* 같은 권한 분리 검토 가능.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerAdminController.java`
- 관련 ADR: [runner/0001](0001-anonymous-run-creation.md), [common/security/0001](../common/security/0001-runner-admin-only-form-login.md)
- 관련 커밋: `git log -1 2fb453d`
