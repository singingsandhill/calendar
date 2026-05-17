# ADR-0005: robots `Disallow:/*/*` 제거 → 연도별 enumerate (`/*/2024..2035/`)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-07 |
| 도메인 | common |
| 관심사 | SEO |
| 관련 커밋 | `69b9919`, `docs/git_commit.md` Commit 9 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

robots.txt 에 광범위한 패턴 `Disallow: /*/*` 가 들어가 있었다. 의도는 *UGC 페이지
(특정 owner 의 일정/런 등) 를 색인 차단* 이었지만 부작용이 컸다.

- `/guide`, `/use-cases/friend-meeting`, `/insights/trends` 같은 *콘텐츠* 페이지도 두
  슬래시 매칭에 걸려 차단 → GSC 색인 실패.
- robots.txt 표준은 character class (`[0-9]{4}`) 미지원 — 정규식 차단 불가.

## Decision — 무엇을 골랐나

UGC 차단을 *연도별로 enumerate* 해 콘텐츠 페이지는 자유롭게.

- `Disallow: /*/*` 제거.
- `Disallow: /*/2024/`, `/*/2025/`, ..., `/*/2035/` 12년치 enumerate. UGC 가 연도
  세그먼트를 가지므로 (예: `/runners/2025/...`) 정확히 매칭.
- 콘텐츠 페이지 (`/guide`, `/use-cases/*`, `/insights/*`) 는 차단 패턴 외 → 색인 가능.
- Sitemap 6 → 11개 (`/guide`, `/use-cases/*` 4개 추가).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `Disallow: /*/*` 유지 | 단순 | 콘텐츠 페이지 모두 차단 — GSC 색인 실패 |
| meta robots `noindex` 페이지별 부착 | 정밀 | UGC 페이지 수백 개 모두 수정 — 누락 위험 |
| `/runners/`, `/owner/`, ... 직접 차단 | 의도 명확 | UGC 가 여러 도메인 모듈에 흩어져 패턴 다수 — 회귀 위험 |
| **(선택) `/*/20YY/` 연도 enumerate** | UGC 만 정확 차단, 콘텐츠 자유 | — |

연도 12개 enumerate (2024–2035) 는 의도적 — 7~10년치 미리. 매년 갱신 부담을
의식적으로 분산.

## Consequences — 영향

- **긍정:**
  - GSC 색인 실패 해소.
  - 콘텐츠 페이지(`/guide`, `/use-cases/*`) 새 추가 시 robots 수정 불필요.
- **부정:**
  - 매 7~10년마다 Disallow 라인 추가 필요 (현재 2035 까지). 알람/리마인더 없음.
- **후속:**
  - ADR-0007 (콘텐츠 페이지 확장) 이 이 결정 위에서 가능.

## References

- 관련 코드:
  - `src/main/resources/static/robots.txt`
- 관련 docs: `docs/seo-evolution-playbook.md` (안티패턴: 광범위 Disallow)
- 관련 커밋: `git log -1 69b9919`, `docs/git_commit.md` Commit 9
