# ADR-0002: create-schedule-modal fragment + 공유 JS 모듈

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | datedate |
| 관심사 | 프론트엔드 |
| 관련 커밋 | `docs/git_commit.md` Commit 12 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

세 가지 UX 사고가 한 번에 발생.

1. **CTA 라벨 불일치** — 우상단 "+ New Schedule" / 본문 중앙 "+ Create Schedule"
   두 개의 라벨이 *같은 모달* 을 가리키는데 서로 달라 사용자 혼란.
2. **헤더 컨텍스트 이탈** — 헤더의 "Create Schedule" 링크가 자기 대시보드/일정 뷰에서도
   `/#start-form` 으로 이동해 페이지 이탈 발생.
3. **빈 상태 중복 노출** — 일정 0개일 때 우상단 + 본문 중앙 두 CTA 가 동시 노출 → 시각
   중복.

또 모달 마크업이 `owner/dashboard.html` 인라인이라 다른 페이지(일정 뷰) 에서 재사용 X.

## Decision — 무엇을 골랐나

모달을 fragment + 공유 모듈로 추출하고, CTA 정책을 페이지 컨텍스트에 따라 분기.

- **`fragments/create-schedule-modal.html`** (신규) — 모달 마크업 fragment. `data-create-modal-close`
  로 close 바인딩 (인라인 onclick 제거).
- **`static/js/create-schedule-modal.js`** (신규) — `window.openCreateModal/closeCreateModal/handleCreateSchedule`
  통합. ownerId 는 `window.OWNER_DATA?.ownerId ?? window.SCHEDULE_DATA?.ownerId` 로 해결.
- **헤더 CTA hijack** — 모달이 DOM 에 존재할 때만 `[data-create-cta]` 헤더 CTA 를 hijack
  → 홈/마케팅 페이지에선 기존 `/#start-form` 동작 보존.
- **빈 상태 분기** — 우상단 액션 div 에 `th:if="${!schedules.isEmpty()}"` 추가 → 빈
  상태에서 본문 중앙 CTA 만 노출.
- **CTA 라벨 통일** — 두 버튼 모두 `dashboard.new.schedule` 메시지 키 사용. 값:
  "+ 일정 만들기" / "+ Create Schedule" → 헤더 CTA 와 정렬.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 모달 페이지별 복제 | 단순 | 라벨/동작 분기 매번 — 회귀 |
| 모달 라이브러리(SweetAlert 등) | 빠른 구축 | 라이브러리 추가, CSS 토큰 충돌 |
| **(선택) Thymeleaf fragment + 공유 모듈** | 재사용성 + 빌드 추가 0 | — |

헤더 CTA hijack 정책: *모달 fragment 가 페이지에 포함될 때만* hijack. 이 조건은 ownerId
가 컨텍스트에 있을 때만 모달이 렌더링되는 것과 일치 → 홈/마케팅 페이지는 기존 동작
보존 (페이지 이동).

## Consequences — 영향

- **긍정:**
  - CTA 라벨/동작이 헤더/우상단/본문 중앙에서 일관.
  - 자기 대시보드/일정 뷰에 있는 사용자는 헤더 CTA 클릭 시 모달 오픈 (페이지 이탈
    없음).
  - 빈 상태에서 한 CTA 만 노출 → 시각 중복 제거.
  - hijack 은 `e.preventDefault()` 후 모달 오픈 → 키보드/모바일에서 이벤트 두 번
    잡히지 않음.
- **부정:**
  - 새 페이지에서 모달 활용 시 fragment 포함 + ownerId 주입 누락 시 hijack 안 됨 —
    리뷰 가드.
- **후속:**
  - ADR-0003 (모달 Year/Month 동적) 이 모달 내부의 시점 캐시 버그 별도 수정.

## References

- 관련 코드:
  - `src/main/resources/templates/fragments/create-schedule-modal.html`
  - `src/main/resources/static/js/create-schedule-modal.js`
  - `src/main/resources/templates/owner/dashboard.html`
  - `src/main/resources/templates/schedule/view.html`
  - `src/main/resources/templates/fragments/header.html`
  - `src/main/resources/messages.properties`, `messages_en.properties` (`dashboard.new.schedule`)
- 관련 커밋: `docs/git_commit.md` Commit 12
