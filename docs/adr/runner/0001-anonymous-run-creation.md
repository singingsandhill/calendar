# ADR-0001: 인증 없이 런(Run) 생성 가능

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-12 |
| 도메인 | runner |
| 관심사 | 도메인 모델 / 사용자 흐름 |
| 관련 커밋 | `29fcfa9` |
| 관련 이슈 | #17 |

## Context — 무엇이 문제였나

97 Runners 크루는 *서버 비용 절감* 을 명시 목표로 단순한 출석/거리 추적만 운영. 처음
구현 시 모든 mutation 에 폼 로그인 강제했지만 — 출석 기록 시점마다 사용자가 비번
입력 → 마찰 큼.

또 크루 운영자는 "오늘 런 만든 다음 멤버들이 출석 마킹" 흐름을 원함. 멤버 한 명씩
계정 만들고 비번 관리하는 부담 회피.

## Decision — 무엇을 골랐나

런 생성 + 출석 마킹은 anonymous, *삭제* 만 어드민 권한.

- **런 생성** — 인증 없이 가능. ownerId / runId 가 사실상 *공유 토큰* 역할.
- **출석 마킹** — 인증 없이 가능. 멤버 본인이 자기 이름 선택 후 저장.
- **삭제 / 어드민 행위** — `/runners/admin/**` 경로, ROLE_ADMIN 폼 로그인 (ADR-0001
  common/security).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 멤버별 회원가입 | 정확한 권한 | 97명 × 비번 관리 — 운영 부담 |
| OAuth (Google) | 회원가입 마찰 ↓ | 어드민 1명 위해 OAuth 인프라 — 비용 비효율 |
| 매직 링크 (이메일) | 가벼움 | 이메일 발송 인프라 (Stock 봇이 메일 사용하지만 1명 운영자에게만) |
| **(선택) anonymous 생성/마킹 + 어드민만 잠금** | 비용/마찰 둘 다 최소 | — |

URL 자체가 *비공개 토큰* — 외부에 노출되면 누구나 마킹 가능. 트레이드오프 인지하고
선택 (97 Runners 는 사실상 사적 그룹).

## Consequences — 영향

- **긍정:**
  - 멤버 가입 마찰 0.
  - 운영자 부담 0 (계정 관리 X).
- **부정:**
  - URL 노출 시 외부인이 마킹 가능 — 관리 책임은 크루 운영자에게.
  - 출석 위변조 가능 — 사적 신뢰 기반 운영.
- **후속:**
  - ADR-0002 (출석 삭제 어드민 전용) 가 위변조 *복구* 채널 보장.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/runner/presentation/controller/RunController.java`
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java` (`/runners/**` permitAll)
- 관련 ADR: [common/security/0001](../common/security/0001-runner-admin-only-form-login.md)
- 관련 커밋: `git log -1 29fcfa9`
