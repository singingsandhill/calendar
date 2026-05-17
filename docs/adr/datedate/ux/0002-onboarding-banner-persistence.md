# ADR-0002: 온보딩 배너 닫힘 = localStorage 영구화 + 헤더 토글로 재오픈

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | datedate |
| 관심사 | UX |
| 관련 커밋 | `docs/git_commit.md` Commit 14 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

일정 페이지 1-2-3 안내 배너의 두 가지 문제.

1. **닫기 버튼만 있고 다시 열 방법 없음** — 한 번 닫으면 새로고침해도 그대로 사라짐
   (서버 사이드 조건 X) 또는 새로고침 시 다시 표시 (LS X). 양쪽 다 사용자 의도와 어긋남.
2. **닫힘 상태가 세션 단위** — 동일 사용자가 다음 방문에 다시 보게 됨 → 배너 피로.

## Decision — 무엇을 골랐나

localStorage 영구화 + 헤더 토글 진입점.

- **localStorage 키 `datedate.onboarding.dismissed`** — 닫힘 상태 저장. 다음 방문에도
  유지.
- **`setBannerOpen(open)` 헬퍼** — banner.hidden 과 toggle.hidden 을 상호 토글, aria-expanded
  동기화.
- **`onboarding.dismiss` 이벤트** → LS 저장 + 토글로 포커스 이동.
- **`onboarding.show` 이벤트** → LS 제거.
- **헤더 `#onboardingHelpToggle` 버튼** — `th:hidden` 으로 noscript 케이스 호환.
- **private mode LS 예외** — try/catch 로 흡수 (LS write 실패 무시).
- **i18n** — `schedule.help.show` 메시지 키 ("도움말 보기" / "Show guide").

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 서버 사이드 사용자 설정 저장 | 다기기 동기화 | 인증 없는 사용자 어떻게 식별 — 인프라 추가 |
| 쿠키 사용 | 서버 가시성 | 서버는 이 정보를 사용하지 않음 — 헤더 부담 |
| **(선택) localStorage** | 클라이언트 단독, 인프라 0 | — |

private mode 에서 LS 쓰기 실패는 사용자가 매 방문마다 배너 보는 것 → 큰 문제는 아님.
try/catch 흡수.

## Consequences — 영향

- **긍정:**
  - 한 번 닫으면 다음 방문에도 유지.
  - 헤더 토글로 다시 열기 가능.
  - noscript 환경에서도 깨지지 않음.
- **부정:**
  - 다기기 사용 시 동기화 X — 의도된 단순함.
- **후속:**
  - 같은 LS 키 패턴(`datedate.<feature>.<state>`) 을 다른 *영구 dismiss* 사고에 재사용.

## References

- 관련 코드:
  - `src/main/resources/templates/schedule/view.html`
  - `src/main/resources/static/js/schedule/main.js`
  - `src/main/resources/messages.properties`, `messages_en.properties` (`schedule.help.show`)
- 관련 커밋: `docs/git_commit.md` Commit 14
