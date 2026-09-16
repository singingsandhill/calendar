# ADR-0003: GHCR 이미지 발행을 `deploy` 릴리스 브랜치로 분리 (main 은 빌드·테스트만)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-09-10 |
| 도메인 | common |
| 관심사 | 인프라 / 배포 |
| 관련 ADR | [common/infrastructure/0001](0001-container-restart-deploy-pipeline.md) (재시작 배포 파이프라인 — 이 ADR 이 그 "빌드" 항목만 갱신), [0002](0002-nginx-in-compose-and-certbot-webroot.md) |
| 관련 이슈 | 사용자 요청 (2026-09-10) — "main push 가 배포 트리거인데 deploy 브랜치로" |

## Context — 무엇이 문제였나

- [0001](0001-container-restart-deploy-pipeline.md) 은 `ci.yml` 이 **main 푸시마다** GHCR 에
  `:<git-sha>` + `:latest` 를 발행하도록 정했다. 서버 반영은 `workflow_dispatch` 수동 버튼이므로
  발행 자체가 서버를 건드리지는 않는다.
- 그러나 발행 브랜치가 곧 개발 브랜치라 부작용이 셋 있다.
  1. **`:latest` 가 개발 tip 을 따라간다.** 문서 한 줄 커밋에도 `:latest` 가 옮겨간다.
     `deploy.sh` 는 `latest` 를 받아도 즉시 다이제스트로 고정하므로 배포된 것이 바뀌지는 않지만,
     "지금 `:latest` 가 무엇인가" 가 릴리스 의도와 무관해진다.
  2. **레지스트리에 배포 후보가 아닌 이미지가 쌓인다.** 발행 = 릴리스 후보라는 신호가 없다.
  3. 사용자가 main 푸시를 배포 트리거로 인지하고 있었다 — 실제로는 아니지만, 발행과 개발이
     같은 브랜치에 묶여 있으면 그 오인이 계속 재생산된다.
- 반대로 main 의 빌드·테스트는 유지해야 한다. 회귀를 여기서 잡지 못하면 릴리스 브랜치에서
  처음 발견하게 된다.

## Decision — 무엇을 골랐나

**이미지 발행 잡만 `deploy` 브랜치로 옮긴다. 서버 반영은 0001 그대로 수동 버튼 전용.**

- `ci.yml` 트리거: `push: branches: [main, deploy]` + `pull_request: branches: [main]`.
- `Upload jar for image job` 과 `push-image` 잡의 조건: `github.ref == 'refs/heads/deploy'`.
- 결과 계약:

| 이벤트 | 빌드·테스트 | 이미지 발행 | 서버 반영 |
|---|---|---|---|
| main 으로의 PR | ○ | ✗ | ✗ |
| main 푸시 | ○ | ✗ | ✗ |
| **deploy 푸시** | ○ | **○** (`:<git-sha>`, `:latest`) | ✗ |
| `deploy.yml` 수동 버튼 | — | — | ○ |

- 릴리스 절차는 `main → deploy` fast-forward 푸시. 두 ref 가 같은 커밋을 가리키면 SHA 도 같으므로
  `deploy.yml` 의 `image_tag` 가 그대로 맞는다.
- `deploy.yml` 의 `image_tag` description 에 "비우면 dispatch 한 브랜치의 SHA 가 쓰이므로
  브랜치를 `deploy` 로 고를 것" 을 명시했다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| main 푸시 발행 유지 (0001 현행) | 브랜치 1개, 단순 | 위 Context 의 세 부작용이 그대로. 사용자 요청과 배치 |
| deploy 푸시 = 서버 자동 배포 | 버튼 한 번이 줄어듦 | 0001 이 이미 기각한 자동 배포를 브랜치로 격리해 되살리는 것. 실계좌 LIVE 봇 서버라 푸시 실수가 곧 봇 재시작(보호전용 강등 + 서킷브레이커 카운터 리셋). **반영 시점은 사람이 고른다**는 0001 의 핵심 불변식을 깬다 |
| git tag 푸시로 발행 (`v*`) | 릴리스가 불변 지점에 고정 | 태그 발급이 한 단계 늘고, 되돌릴 때 태그를 지우거나 새로 파야 한다. 현 운영 규모(1인·수동 버튼)에 비해 과함 |
| main 발행 + `:latest` 만 수동 승격 | 레지스트리 이력 보존 | 승격 절차가 또 하나의 수동 단계인데 얻는 것이 `:latest` 의미뿐. deploy 브랜치 쪽이 절차 수가 같고 신호가 명확 |

- **브랜치를 고른 이유**: 배포 대상 선택이 이미 `deploy.yml` 의 "Use workflow from" 브랜치
  드롭다운에 존재한다. 발행 브랜치와 dispatch 브랜치를 같은 이름으로 맞추면 조작 지점이 하나다.
- `deploy` 라는 이름은 이 저장소에 전례가 있다 (`b678bce`, 지금은 삭제된 Cloud Run 워크플로).

## Consequences — 영향

- **긍정:** `:latest` 가 릴리스 의도를 뜻하게 된다. GHCR 에 쌓이는 이미지가 배포 후보와 1:1.
  main 은 계속 전체 스위트를 돌려 회귀 감지가 유지된다.
- **부정:** 릴리스마다 `main → deploy` 푸시 한 단계가 는다. 이 단계를 잊으면 배포 버튼이
  "없는 이미지" 를 가리키는데, `deploy.yml` 의 GHCR manifest 선검증이 서버 접속 전에 잡는다.
- **부정:** 같은 커밋이 두 ref 로 푸시되면 CI 가 두 번 돈다 (`concurrency` 그룹이 ref 별).
  main 쪽 실행은 이미지 잡을 건너뛰므로 비용은 빌드·테스트 1회분.
- **운영:** `deploy` 브랜치는 origin 에 **존재해야** 트리거된다. 최초 1회 `git push -u origin deploy`.
- **함정:** 브랜치명 `deploy` 가 저장소의 `deploy/` 디렉터리와 모호해 `git log deploy` 는
  `fatal: ambiguous argument` 로 실패한다. `git log deploy --` 또는 `refs/heads/deploy` 로 쓴다.
- **0001 과의 관계:** 0001 은 Superseded 가 아니다. 재시작 배포·다이제스트 롤백·수동 반영·
  종료 계약은 전부 유효하고, 이 ADR 은 0001 Decision 의 "빌드" 항목 한 줄만 갱신한다.

## References

- `.github/workflows/ci.yml` (트리거·`push-image` 조건), `.github/workflows/deploy.yml` (`image_tag` description)
- [ADR common/infrastructure/0001](0001-container-restart-deploy-pipeline.md) — 파이프라인 전체 결정
- 운영 절차: `docs/operations/deployment.md`, 세팅 체크리스트: `docs/guides/deploy-setup.md`
