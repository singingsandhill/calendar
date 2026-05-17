# ADR-0003: 모달 Year/Month 매 오픈마다 `new Date()` 재계산

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | datedate |
| 관심사 | 프론트엔드 / UX |
| 관련 커밋 | `docs/git_commit.md` Commit 11 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`owner/dashboard.html` 의 New Schedule 모달이 *시점 캐시 버그* 를 가지고 있었다.

- Year `<select>` 의 옵션 7개(2024–2030) 가 **하드코딩**.
- Month `<select>` 가 `th:selected="${m == 12}"` 로 12월 고정 selected.
- 결과: 2026-05-01 에 모달을 열어도 기본값이 "2025/12" 로 표시.

추가 문제: Year 범위가 코드에 박혀 있어 2030 이후 미래 연도 선택 불가.

## Decision — 무엇을 골랐나

모달이 열릴 때마다 `new Date()` 로 재계산.

- **하드코딩 옵션 제거** — Year `<select>` 는 빈 select. JS 가 채움. Month `<option>`
  의 `th:selected="${m == 12}"` 제거.
- **`resetYearMonthToNow()` 헬퍼** — `openCreateModal()` 호출 시마다 실행.
  - `new Date()` 로 현재 연/월 재계산.
  - Year 옵션 `[현재..현재+5]` 6개 재생성.
  - Month 는 현재 월 selected.
- **시점 캐시 금지** — 매 오픈마다 갱신 → 자정 경계 / 연말 경계도 정상 동작.
- **과거 연도 미노출** — Year 시작 = 현재 연도.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 서버 사이드 Year 주입 | Thymeleaf `${T(java.time.LocalDate).now().year}` | 페이지 로드 시점 캐시 — 사용자가 페이지 오래 두면 자정 경계 후 모달 기본값 어긋남 |
| 첫 모달 오픈 시 한 번 계산 후 캐시 | 호출 비용 절감 | 같은 자정 경계 문제 |
| **(선택) 매 오픈마다 재계산** | 시점 항상 정확 | — |

`new Date()` 호출 비용은 마이크로초 — 매 오픈마다 호출해도 비용 무시 가능.

## Consequences — 영향

- **긍정:**
  - 자정 직후 모달 오픈해도 정확한 기본값.
  - Year 범위가 동적이라 2030 이후도 자동 노출.
- **부정:**
  - 사용자가 과거 일정 (예: 2024 년) 을 다시 만들 수 없음 — 디자인 의도. 필요 시
    Year 시작 옵션을 별도 정책으로 변경.
- **후속:**
  - ADR-0002 (공유 Create 모달) 이 이 헬퍼를 공유 모듈로 이동.

## References

- 관련 코드:
  - `src/main/resources/templates/owner/dashboard.html` (이전), `fragments/create-schedule-modal.html` (현재)
  - `src/main/resources/static/js/create-schedule-modal.js`
- 관련 커밋: `docs/git_commit.md` Commit 11
