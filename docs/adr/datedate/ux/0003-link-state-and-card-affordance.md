# ADR-0003: 링크 4-state 일관 + 인기 카드 헤더 어포던스

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | datedate |
| 관심사 | UX / 시각 |
| 관련 커밋 | `docs/git_commit.md` Commit 14 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

사용자 테스트에서 두 가지 시각적 어긋남이 보고됨.

1. **인기 섹션 카드의 파란 가로 막대** — `.popular-card-header` 가 `linear-gradient`
   가로 막대로 디자인되어 *클릭 가능한 탭처럼* 보였지만 실제로는 헤더 장식. 사용자가
   클릭 시도 → 반응 없음으로 혼란.
2. **링크 :visited 보라색** — "View User Guide →" 같은 텍스트 링크가 브라우저 기본
   `:visited` 보라색으로 출력되어 브랜드 블루와 톤 어긋남.
   `.empty-state a / .footer-link / .footer-nav-link / .popular-link` 가 일관성 없음.

## Decision — 무엇을 골랐나

탭 오인 가능성 제거 + 4-state 명시.

- **`.popular-card-header`** — `linear-gradient` 가로 막대 제거 → 흰 배경 + `border-bottom`
  underline. `.popular-card` 에 `border-left: 4px solid var(--primary-color)` 좌측 액센트.
  `.popular-icon` 만 `fill: var(--primary-color)` 로 브랜드 컬러 유지.
- **텍스트 링크 4-state 명시** — `:link / :visited / :hover / :focus` 모든 상태에서
  의도된 색.
  - `.empty-state a` (신규), `.footer-link`, `.footer-nav-link`, `.popular-link` 모두
    `:link, :visited` 에 `var(--primary-color)` 또는 `var(--text-light)` 고정.
  - `:hover, :focus` 는 `var(--primary-dark)` + underline 통일.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 카드 헤더 가로 막대 유지하되 배경 다르게 | 시각 변경 작음 | 가로 막대 자체가 탭 오인 신호 — 색 변경으로 해결 안 됨 |
| `:visited` 만 따로 처리 | 기존 :hover 보존 | 링크별 색 일관성 부족 → 다른 페이지에서 재발 |
| **(선택) 좌측 액센트 + 4-state 명시** | 어포던스 명확 + 톤 통일 | — |

다크모드 토큰은 프로젝트에 없으므로 별도 규칙 미추가 (수용 기준의 "있다면" 조건).

## Consequences — 영향

- **긍정:**
  - 카드 헤더가 가로 막대 → 좌측 액센트 + underline → 탭 오인 가능성 제거.
  - 푸터/빈 상태/인기 카드의 모든 텍스트 링크가 4-state 에서 브랜드 컬러 일관 사용.
- **부정:**
  - 새 텍스트 링크 추가 시 4-state 명시 누락하면 :visited 보라 재발. CSS 글로벌
    selector 또는 컴포넌트 클래스로 적용 가드.
- **후속:**
  - 향후 다크모드 토큰 추가 시 4-state 규칙도 다크 컨텍스트에서 정의 필요.

## References

- 관련 코드:
  - `src/main/resources/static/css/style.css`
- 관련 커밋: `docs/git_commit.md` Commit 14
