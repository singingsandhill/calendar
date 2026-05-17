# ADR-0001: schedule-view.js → ES module 6개 분해 + inline 핸들러 제거

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-20 |
| 도메인 | datedate |
| 관심사 | 프론트엔드 |
| 관련 커밋 | `7012cc0` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

기존 `schedule-view.js` 는 IIFE + `window.*` 전역에 함수를 노출하고, 템플릿은
`onclick="handleX(...)"` 인라인 핸들러로 호출. 문제:

1. **함수 한 곳, 책임 다섯 곳** — 캘린더 렌더링, 참가자 상태, 투표 관리, 메시지
   유틸, 메인 부트스트랩이 한 파일 ~700줄.
2. **CSP 친화 X** — 인라인 핸들러는 `Content-Security-Policy: script-src 'self'`
   설정과 충돌 (운영자가 CSP 강화 시 깨짐).
3. **단위 테스트 어려움** — IIFE 안의 함수가 외부에서 import 불가.

## Decision — 무엇을 골랐나

`<script type="module">` 기반 ES 모듈로 분해.

- **6개 모듈로 분리:**
  - `main.js` — 부트스트랩, 이벤트 라우팅.
  - `state.js` — 참가자/선택 상태 관리.
  - `calendar.js` — 캘린더 셀 렌더링.
  - `voting.js` — 장소/메뉴 투표.
  - `participants.js` — 참가자 칩/색상.
  - `utils.js` — 메시지 포맷, 토스트.
- **인라인 onclick 제거** — `addEventListener` 또는 `data-*` 속성 + 위임.
- **템플릿에 `window.SCHEDULE_DATA` 인젝션** — Thymeleaf 가 ownerId, scheduleId,
  messages 를 JSON 으로 주입, 모듈은 `window.SCHEDULE_DATA.*` 참조.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 한 파일 유지 | 빌드 변경 없음 | 위 3가지 문제 그대로 |
| 번들러 (webpack/vite) 도입 | tree-shaking, TS | 빌드 인프라 추가 — 단일 페이지 앱 정도가 아닌데 과한 부담 |
| **(선택) ES module + 브라우저 네이티브** | 빌드 추가 0, CSP 친화, 단위 테스트 가능 | — |

`<script type="module">` 은 모던 브라우저 표준. IE 미지원이지만 우리 서비스 브라우저
정책은 모던 브라우저.

## Consequences — 영향

- **긍정:**
  - 모듈별 책임 분리 → 변경 영향 범위 좁아짐.
  - 향후 CSP 강화 시 인라인 핸들러 가드 통과.
- **부정:**
  - `window.SCHEDULE_DATA` 가 모듈 사이의 *공유 상태* 가 됨. 깊은 구조면 props/state
    관리 라이브러리 검토 필요.
- **후속:**
  - ADR-0002 (공유 Create 모달) 이 같은 모듈 패턴으로 대시보드/일정 뷰 양쪽에서
    재사용.

## References

- 관련 코드:
  - `src/main/resources/static/js/schedule/{main,state,calendar,voting,participants,utils}.js`
  - `src/main/resources/templates/schedule/view.html`
- 관련 docs: `docs/datedate-architecture-review.md` (E-3-c)
- 관련 커밋: `git log -1 7012cc0`
