# ADR-0001: 참가자 칩 색 ↔ 셀 강조 색 동기화

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | datedate |
| 관심사 | UX / 접근성 |
| 관련 커밋 | `docs/git_commit.md` Commit 13 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`schedule/view` 의 `.calendar-day.selected` 가 `var(--primary-color)` 파랑으로
하드코딩. 결과: *누가 선택했는지 셀 색만으로는 구분 불가*. "겹치는 날짜를 한눈에
본다" 는 제품 핵심 가치가 약화되던 버그.

추가 문제:
- 참가자 색 팔레트 8색이 일관성 없음 (`#E74C3C` 등 임의 hex).
- 색맹(Deuteranopia) 사용자는 색이 같아 보여 정보 인지 X — 색 외 신호 부재.

## Decision — 무엇을 골랐나

색을 정보의 1차 신호로 정렬하고, 색 외 보조 신호를 추가.

- **`ParticipantColor.PRESET_COLORS` 8색 spec 정렬** — red/orange/yellow/green/teal/blue/indigo/violet
  (Tailwind-600 계열, AA 대비 통과). hex: `#E11D48 / #F97316 / #CA8A04 / #16A34A /
  #0D9488 / #2563EB / #4F46E5 / #7C3AED`. 기존 DB 의 hex 는 그대로 (regex 검증 동일).
- **셀 강조 = 참가자 색** — 가용 인원 1명이면 `.solo` + `--solo-color` 인라인으로 참가자
  색을 셀 배경/보더에 직접. 2명+ 은 `.heat-2/.heat-3/.heat-4/.heat-5plus` 명도 단계.
- **색맹 대응 보조 신호:**
  - 우상단 `.cell-count-badge` (≥2일 때 N 표시).
  - 셀 상단 `.cell-tooltip` (hover/focus 시 이름 목록).
  - `tabIndex=0` + `role="button"` + Enter/Space 키보드 토글.
  - `aria-label` 에 인원수 + 이름 노출.
- **참가자 칩 ↔ 현재 편집 색 동기화** — 참가자 선택 변경 시 `#calendarBody` 에
  `--current-color` CSS 변수 주입 → `.selected` 의 보더/박스섀도가 현재 편집 중인 참가자
  색으로 동기화.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `.selected` 그대로 파랑 유지 | 코드 변경 적음 | 핵심 가치 손상 |
| 셀에 참가자 이니셜 표시 | 색 의존 0 | 셀 좁아 가독성 ↓ |
| **(선택) 색 기반 + 보조 신호 4종** | 색맹 사용자도 정보 인지 가능 | — |

WCAG AA 기준 대비 4.5:1 충족 색만 채택. Tailwind-600 은 흰 배경 위에서 모두 통과.

## Consequences — 영향

- **긍정:**
  - 4명 가용 셀(heat-4 / 50% 강도) 이 1명 가용 셀(solo / 18% 틴트) 보다 시각적으로
    명확.
  - 색맹 시뮬레이션: 색이 같아 보여도 점 개수 + N 배지로 구분 가능.
  - Tab 으로 셀 포커스 → focus-visible 아웃라인 + 이름 툴팁 노출, Enter/Space 로 토글.
- **부정:**
  - 셀당 보조 마크업(badge/tooltip) 증가 → DOM 노드 수 ~1.5배. 49일 그리드라 무시 가능.
- **후속:**
  - 참가자 추가 색 확장 시 8색 spec 외 색 사용 금지 — `ParticipantColorTest` 가
    가드.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColor.java`
  - `src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColorTest.java`
  - `src/main/resources/static/js/schedule/calendar.js`
  - `src/main/resources/static/js/schedule/participants.js`
  - `src/main/resources/static/css/style.css`
- 관련 커밋: `docs/git_commit.md` Commit 13
