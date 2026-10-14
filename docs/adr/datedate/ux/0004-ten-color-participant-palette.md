# ADR-0004: 참여자 색 팔레트 10색 — 갈색·청록 2색을 끝에 덧붙인다

| 항목 | 값 |
|---|---|
| 상태 | Accepted — [0001](0001-participant-color-cell-sync.md) 의 "8색 spec" 결정을 대체 (색 = 1차 신호, 색맹 보조 신호 4종, 칩↔셀 색 동기화는 유지) |
| 날짜 | 2026-10-10 |
| 도메인 | datedate |
| 관심사 | UX / 접근성 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 197 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

- 참여자 한도가 10명이 됐다([domain/0010](../domain/0010-participant-cap-10.md)). 색은 `ParticipantColor.ofIndex(index % 8)`,
  `index = participants.size()` 라 9·10번째가 1·2번째와 같은 빨강·주황이 된다. 범례, 셀 점, 1명만 가능한 날의 셀 배경
  (9번째만 가능한 날이 1번째 사람 색으로 칠해짐), 편집 테두리에서 사람을 구분할 수 없다. 화면 문구는 "고유 색상" 을 약속한다.
- ADR-0001 은 "8색 spec 외 색 사용 금지" 를 정했다. 또 "AA 4.5:1 충족, Tailwind-600 은 흰 배경 위에서 모두 통과" 라고 적었지만
  실측 대비는 orange `#F97316` 2.80:1, yellow `#CA8A04` 2.94:1, green `#16A34A` 3.30:1, teal `#0D9488` 3.74:1 이다.

## Decision — 무엇을 골랐나

팔레트 **끝에** 2색을 덧붙여 10색으로 만든다.

- index 8 = `#92400E` (amber-800, 갈색), index 9 = `#0E7490` (cyan-700, 청록). index 0~7 은 그대로.
- 선정 근거 — 흰 배경 대비, 기존 8색과의 최소 CIEDE2000 (일반 / 녹색맹 deutan / 적색맹 protan, Machado 2009 severity 1.0):

  | 후보 | 흰 배경 대비 | 최소 ΔE00 |
  |---|---|---|
  | **amber-800 `#92400E`** | 7.09:1 | 23.7 / 13.9 / 14.1 |
  | **cyan-700 `#0E7490`** | 5.36:1 | 18.0 / 11.9 / 14.2 |
  | pink-600 `#DB2777` | 4.60:1 | 12.3 / 12.7 / 17.0 |
  | fuchsia-600 `#C026D3` | 4.71:1 | 12.2 / 7.4 / 2.7 |
  | lime-600 `#65A30D` | 3.09:1 | 11.5 / 4.7 / 2.9 |

  기존 8색 안의 최근접 쌍(indigo~violet 6.5, deutan 에서 blue~violet 0.5)보다 두 신규 색이 기존 색과 더 멀다.
- 불변식 가드: 한도까지 `nextColorIndex()` 로 채운 참여자의 색은 모두 다르다(`ScheduleTest.participantsUpToLimit_haveDistinctColors`).
  한도를 다시 올리면 팔레트도 늘려야 빌드가 통과한다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 8색 유지(9·10번째 색 중복 감수) | 변경 없음 | "고유 색상" 문구와 ADR-0001 전제가 깨진다 |
| 팔레트를 색상환 순서로 재배열 | 보기 좋은 순서 | 덧붙이기로 충분 — 재배열하면 테스트·문서의 기존 index 기대값만 흔들린다 |
| 직관적 후보 핑크·퓨샤 | 채도 높음 | 기존 rose·violet 과 가까움(ΔE 12.2~12.3), fuchsia 는 protan 2.7 |
| **(선택) 갈색·청록 덧붙이기** | 기존 색과 가장 멀고 대비 5:1 이상 | — |

## Consequences — 영향

- **긍정:** 참여자를 지우지 않는 한 10명까지 색이 겹치지 않는다. 색 번호가 `participants.size()` 라, 공개 API
  `DELETE /api/participants/{id}` 로 누군가를 지운 뒤 추가하면 남은 사람과 같은 색이 될 수 있다(삭제 UI 는 없음, 8명 때와 같은 기존 동작).
  색은 참여자별 hex 로 저장돼 기존 데이터·마이그레이션 영향이 없다(롤백해도 hex 형식만 검사).
- **부정:** 색만으로의 구분은 색각 이상 사용자에게 여전히 불완전 — ADR-0001 의 보조 신호(인원 배지·툴팁·aria-label)에 계속 의존한다.
- **같이 고친 것:** 320px 이하 화면에서 10명 가능한 날의 점이 3줄로 셀 밖 13.4px 까지 넘쳤다 → 그 구간만 점 5px·간격 1px 로 2줄
  (미세 CSS, 별도 ADR 없음).
- **남은 것:** 두 자리 인원 배지("10")가 모바일에서 날짜 숫자를 더 가린다(겹침 면적 +45~57%). 툴팁은 10명×10자 이름이면 1,108px.
  둘 다 기존 구조 문제라 범위 밖.

## References

- 관련 코드: `datedate/domain/participant/ParticipantColor.java`, `static/css/style.css` (`@media (max-width: 320px)` 의 `.participant-dot`)
- 테스트: `ParticipantColorTest`, `ParticipantTest`, `ScheduleTest`
- 계획(계산·실측 근거): `docs/superpowers/plans/2026-10-07-participant-cap-10.md` §0
