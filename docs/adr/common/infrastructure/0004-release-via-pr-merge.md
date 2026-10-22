# ADR-0004: 릴리스는 `main → deploy` PR 머지 (fast-forward 푸시 대체)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-17 |
| 도메인 | common |
| 관심사 | 인프라 / 배포 |
| 관련 ADR | [common/infrastructure/0003](0003-image-publish-on-deploy-branch.md) (릴리스 절차 한 줄만 부분 대체 — deploy 푸시에서만 발행·서버 반영은 수동 버튼이라는 결정은 유효), [0001](0001-container-restart-deploy-pipeline.md) |
| 관련 이슈 | 사용자 결정 (2026-10-17) — "PR 머지로 수정" |

## Context — 무엇이 문제였나

- [0003](0003-image-publish-on-deploy-branch.md) 은 릴리스 절차를 `main → deploy` **fast-forward 푸시**로
  정했다. 두 ref 가 같은 커밋을 가리키므로 배포 버튼의 `image_tag` 에 main 의 SHA 를 그대로 쓸 수 있다는 근거였다.
- 실제 릴리스는 GitHub PR 이었다. `deploy` 에만 있는 머지 커밋 `ff476ee`(PR #22)가 그것이고, 첫 서버
  이관(2026-10-17)에 쓴 이미지도 이 머지 커밋에서 발행됐다.
- 머지 커밋은 main 에 없으므로 그 뒤로 fast-forward 푸시는 거절된다. 문서(0003·`deployment.md`·`CLAUDE.md`)와
  관행이 어긋난 채로 두면 다음 릴리스에서 문서대로 한 사람이 막힌다.

## Decision — 무엇을 골랐나

**릴리스는 GitHub 에서 base `deploy` ← compare `main` PR 을 만들어 "Create a merge commit" 으로 머지한다.
`deploy` 브랜치에 직접 커밋·푸시하지 않는다.**

- 머지가 곧 `deploy` 푸시라 0003 의 계약(빌드·테스트 + GHCR 발행, 서버 무영향)이 그대로 돈다.
- 배포 버튼의 `image_tag` 는 **`deploy` 최신 커밋(머지 커밋)의 40자 SHA** 다 — main 의 SHA 와 다르다.
  비워 두면 "Use workflow from: `deploy`" 의 `github.sha` 가 같은 값이 된다.
- 머지 방식은 merge commit 만 쓴다. squash·rebase 머지는 `deploy` 에 main 과 무관한 새 커밋을 만들어
  머지 기준점이 앞으로 가지 않으므로, 다음 PR 마다 이미 반영한 변경이 diff·충돌로 다시 나타난다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| fast-forward 푸시 유지 (0003) | main·deploy SHA 일치, 머지 커밋 없음 | 이미 머지 커밋으로 갈라져 되돌리려면 `deploy` 강제 푸시나 main 역병합이 필요. 관행과 다른 절차를 문서로만 유지하는 셈 |
| PR 머지 (선택) | GitHub 화면에서 릴리스 diff·CI 결과를 보고 머지, 1인 운영의 기존 습관 | 이미지 태그가 main 의 SHA 와 달라짐 — 버튼에 deploy 최신 SHA 를 넣으면 해결 |
| `deploy` 직접 커밋(핫픽스) 허용 | 급할 때 빠름 | main 에 없는 변경이 생겨 다음 릴리스에서 되돌아가거나 충돌. 핫픽스도 main 경유 |

## Consequences — 영향

- **긍정:** 문서와 관행이 일치한다. 릴리스 내용을 PR 화면에서 확인한 뒤 머지한다.
- **부정:** `deploy` 의 SHA 가 main 과 달라 "main 의 어느 커밋이 배포됐나" 는 머지 커밋의 두 번째 부모로 본다.
- **부정:** 0003 과 같이 같은 변경에 CI 가 두 번 돈다(main 푸시, deploy 머지).
- **운영:** 반영은 여전히 수동 버튼이다([0001](0001-container-restart-deploy-pipeline.md)) — PR 머지만으로 서버는
  바뀌지 않는다.

## References

- `.github/workflows/ci.yml` (`push-image` 의 `refs/heads/deploy` 조건), `.github/workflows/deploy.yml` (`image_tag`)
- 운영 절차: `docs/operations/deployment.md`
