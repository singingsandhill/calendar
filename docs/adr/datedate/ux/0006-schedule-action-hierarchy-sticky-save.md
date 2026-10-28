# ADR-0006: 일정 페이지 액션 위계 4단계 + 모바일 저장 줄 하단 고정 + 대시보드 삭제 약화

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-21 |
| 도메인 | datedate |
| 관심사 | UX / 프론트 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 208 (코드), 211 (이 ADR) |
| 관련 이슈 | 모바일 점검(2026-10-21, 375·320px 실측) |

## Context — 무엇이 문제였나

- 일정 페이지의 링크 복사·도움말 보기·+ 설명 추가·+ 추가·투표 추가 3개가 모두 같은 파란 테두리 버튼이라, 핵심 흐름
  (이름 선택 → 날짜 탭 → 저장)의 "저장" 이 묻힌다. 모바일에선 초기화(회색 채움)가 저장 바로 위에 같은 폭으로 붙어 있다.
- 모바일에서 달력(7주 49칸)을 보며 날짜를 탭하는 동안 저장 버튼은 화면 아래 밖에 있다.
- 대시보드 일정 카드는 "삭제"(빨강 채움)가 "보기"(회색 채움)보다 강하고 바로 아래에 있다.

## Decision — 무엇을 골랐나

1. **위계 4단계** — 채움(`.btn-primary`: 저장·보기·+ 일정 만들기) > 테두리(`.btn-outline`: 링크 복사·+ 추가·투표 추가) >
   글자 링크(`.btn-text` + 강조색: 도움말 보기·+ 설명 추가/편집) > 글자 버튼(`.btn-text`: 초기화, 대시보드 삭제는 `--danger-color`).
   새 공통 클래스 없이 기존 `.btn-text` 를 쓰고 색만 컨테이너 범위로 지정한다.
2. **모바일 저장 줄 하단 고정** — 별도 바를 만들지 않고 기존 `.action-buttons`(초기화·저장) 를 `max-width: 768px` 에서
   `position: sticky; bottom: 0.5rem` 로 띄운다. 같은 버튼이라 핸들러(`data-action="selections.save"`)도 같다.
   조건은 "이름이 선택됐고 고른 날짜가 저장된 값과 다를 때" — `calendar.js updateDirtyState()` 가 기존 상태
   `selection.selectedDays` 와 `participant.selections` 를 비교해 `.is-dirty` 를 토글한다(날짜 탭·이름 변경·초기화·저장 후).
   패딩·모서리는 평소에도 두어 고정될 때 레이아웃이 움직이지 않게 한다.
3. **삭제 약화** — 대시보드 "삭제" 는 빨간 글자 버튼. 실행 전 `confirm()` 은 기존(`owner-dashboard.js`) 그대로.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 전부 테두리 유지 | 변경 없음 | 저장이 다른 보조 액션과 같은 무게 |
| 새 고정 바(별도 저장 버튼) | 디자인 자유 | 버튼·핸들러 중복, 두 저장 버튼 상태 동기화 필요 |
| `position: fixed` 상시 바 | 항상 보임 | 저장할 게 없을 때도 화면을 가리고, 본문 하단 여백 보정 필요 |
| **(선택) 기존 줄을 dirty 일 때만 sticky** | 코드 최소, 원래 자리에 오면 자연히 풀린다 | — |
| 삭제 버튼 제거·메뉴 속으로 | 오조작 0 | 기능 접근성 저하, 범위 밖 |

## Consequences — 영향

- **긍정:** 375px 에서 날짜를 탭하면 저장 줄이 화면 하단(y 596~659 / 667)에 떠 있고, 저장하면 원래 자리로 돌아간다(실측).
  데스크톱(>768px)은 고정하지 않는다.
- **부정:** 고정된 줄(높이 약 63px)이 달력 하단 일부를 가린다 — 스크롤로 해소. 초기화는 "저장된 값으로 되돌리기" 가 아니라
  "전부 지우기" 라(기존 동작) 누르면 dirty 상태가 된다.
- **검토 후 유지:** 확장 모드 달력의 다음 달 2줄(7주 49칸)은 커밋 `6ef8638`("선택 가능 일수 확대 #16 - 1달 → 7주")의 의도된 기능이고,
  다른 달 셀 선택이 `Participant.selections`·시간 투표 dayIndex 로 저장되므로 줄이지 않는다([domain/0009](../domain/0009-time-slot-vote.md)).

## References

- 관련 코드: `templates/schedule/view.html`, `templates/owner/dashboard.html`, `static/js/schedule/calendar.js` (`updateDirtyState`),
  `static/css/style.css` (`.action-buttons.is-dirty`, `.schedule-header-actions .btn-text`, `.schedule-card-actions .delete-btn`)
- 관련 ADR: [0005 색 대비 AA](0005-wcag-aa-color-tokens.md)
