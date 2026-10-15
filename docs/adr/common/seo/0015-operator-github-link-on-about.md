# ADR-0015: 운영자 GitHub 링크를 About 화면에 노출

| 항목 | 값 |
|---|---|
| 상태 | Accepted — ADR 0013 결정 3 의 "화면(UI)에는 링크를 노출하지 않는다" 를 대체 |
| 날짜 | 2026-10-14 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / E-E-A-T / AdSense 게시자 신원 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 199 |

## Context — 무엇이 문제였나

- AdSense "가치가 별로 없는 콘텐츠" 4번째 통지(2026-10-12). 통지문은 "신뢰할 수 있는" 정보를 기준으로 든다.
- ADR 0013 은 운영자 GitHub 를 Organization `sameAs` 로만 연결하고 화면에는 두지 않았다. 0013 스스로 "사람 리뷰어에게는 효과 없음" 을 부정적 결과로 적었다.
- About 에는 운영 주체가 누구인지 드러내는 문장이 없었다.

## Decision — 무엇을 골랐나

About 에 "만든 사람" 섹션을 두고 운영자 GitHub(`https://github.com/singingsandhill`) 링크를 화면에 노출한다. 실명은 쓰지 않는다(사용자 결정).

- 문구는 검증 가능한 사실만 — 1인 개발자의 개인 프로젝트, 2025-12 공개, 시간 투표(2026-09)·일정 설명(2026-10) 추가.
- 링크는 새 탭, `rel="me noopener noreferrer"`.
- JSON-LD `sameAs` 는 그대로. `founder`·`Person` 저자는 두지 않는다(ADR 0011 결정 6 유지).
- 기사 바이라인은 "DateDate 팀" → "DateDate" — 1인 운영과 "팀" 표기가 모순.

## Rationale — 왜 이 선택인가

| 대안 | 판정 |
|---|---|
| 0013 유지(JSON-LD 만) | 사람 리뷰어에게 신원 신호 없음 |
| 실명 + GitHub | 신호는 가장 강하지만 개인정보 공개 부담 — 사용자가 택하지 않음 |
| **GitHub 핸들 + 사실 소개 (선택)** | 실재하는 계정으로 확인 가능한 신원, 실명 비공개 |

## Consequences — 영향

- **긍정:** 사람 리뷰어가 운영 주체를 확인할 수 있다. 회귀 가드 `AboutPageRenderingTest`.
- **부정:** GitHub 계정의 공개 정보가 사이트와 연결된다.
- **후속:** 실명·`Person` 저자 도입은 별도 결정.
