# 배포 파이프라인 운영 가이드

> 결정 근거: [ADR common/infrastructure/0001](../adr/common/infrastructure/0001-container-restart-deploy-pipeline.md)
> (재시작 배포·다이제스트 롤백), [0002](../adr/common/infrastructure/0002-nginx-in-compose-and-certbot-webroot.md)
> (nginx compose·certbot webroot), [common/security/0006](../adr/common/security/0006-actuator-health-and-h2-console-lockdown.md)
> (헬스 게이트·엣지 차단), [trading/modes/0003](../adr/trading/modes/0003-protection-only-recovery-on-restart.md)
> (배포 후 코인 봇 보호전용 자동재개).
> 1회성 서버 이관 절차는 [server-migration-runbook.md](server-migration-runbook.md).
> 최초 세팅이 아직 안 끝났다면 남은 작업·의존성 순서는 [guides/deploy-setup.md](../guides/deploy-setup.md).
> 구축 중 부딪힌 문제와 해결은 [troubleshooting/deploy-cicd.md](../troubleshooting/deploy-cicd.md)
> (파이프라인이 왜 이 구조인지에 대한 해설도 같은 문서 1부).

## 파이프라인 개요

```
main 푸시/PR      ──► ci.yml: gradle build+test (+shellcheck)                     [이미지 발행 없음]
deploy 푸시       ──► ci.yml: 위 + GHCR push (:<git-sha>, :latest)               [릴리스 후보]
Actions 수동 버튼 ──► deploy.yml: 태그검증 → manifest 선검증 → 번들 rsync → ssh 게이트 → deploy.sh
```

- **이미지 발행은 `deploy` 브랜치 푸시에서만.** main 은 빌드·테스트만 돌고 이미지를 만들지
  않는다 — 릴리스는 `main → deploy` fast-forward 푸시
  ([ADR 0003](../adr/common/infrastructure/0003-image-publish-on-deploy-branch.md)).
  브랜치명 `deploy` 는 저장소의 `deploy/` 디렉터리와 모호하다 — `git log deploy --` 로 쓸 것.
- **CI 는 자동, 서버 반영은 `workflow_dispatch` 수동 버튼 전용.** 실계좌 LIVE 봇 서버라
  배포마다 봇 재시작(보호전용 자동재개)·서킷브레이커 카운터 리셋이 발생한다 — 반영
  시점은 사람이 고른다.
- 이미지: `ghcr.io/singingsandhill/calendar` — 태그는 full git SHA + `latest`.
- 배포 입력 `image_tag`: 40자 git SHA 권장. 미입력 시 dispatch 한 **브랜치**의 SHA 가 쓰이므로
  Actions 의 "Use workflow from" 에서 `deploy` 를 고른다 — main 을 고르면 아직 이미지가 없는
  SHA 가 될 수 있고, 그 경우 GHCR manifest 선검증에서 서버 접속 전에 실패한다.
  `latest` 도 받지만 서버에서 즉시 **다이제스트로 고정**된다.

## deploy.sh 동작 (서버: `~/apps/calendar/deploy/deploy.sh`)

| 단계 | 내용 |
|---|---|
| preflight | `.env`·`data/scheduledb.mv.db`·`.deploy/env` 존재 + `docker compose config` 로 data 볼륨이 정확히 `~/apps/calendar/data` 로 해석되는지 기계 검증 (경로 회귀 → 빈 DB 기동 차단) |
| 가드 1 | 평일 09:15~11:25 KST(주식 봇 LIVE 창) 배포 거부 — `--force` 로만 우회 |
| pull → 다이제스트 | 구 컨테이너 가동 중 선-pull, `RepoDigests` 로 다이제스트 고정 |
| 가드 2 | 크립토 매분 :05 틱 회피 — 초가 20~50 구간까지 대기 (정지 직전) |
| stop → 백업 → up | SIGTERM(graceful 30s + 스케줄러 대기 25s, grace 60s) → **정지 상태** `data/` 백업 5세대 → 새 다이제스트로 기동 |
| 헬스 게이트 | 컨테이너 사망(exited/재시작 증가) 즉시 실패, 아니면 `/actuator/health/deploy` UP 폴링 (기본 300s — 런북 실측으로 조정) |
| 엣지 게이트 | nginx 경유 `GET /` 200 + `/trading` 리다이렉트가 `https://` 로 시작(X-Forwarded-Proto 계약) → 통과 후 nginx reload |
| 기록·GC | `.deploy/current`/`previous` 에 다이제스트 기록, current/previous 보존 목록 기반 이미지 GC |

**종료코드**: 0 성공 / 2 가드·preflight 거부 / 3 헬스 실패 + 롤백 성공(조사 필요 — Actions
도 실패로 표시) / 4 롤백도 실패(**app 정지 상태로 종료** — 유지보수 페이지 유지, 즉시 개입)
/ 5 pull·레지스트리 실패(**GHCR PAT 만료 여부부터 확인**).

## 롤백

- 자동: 헬스·엣지 게이트 실패 시 `.deploy/current`(직전 성공 다이제스트)로 자동 롤백.
- 수동(2단계 이상 과거): `.deploy/backup/` 세대 확인 후 원하는 SHA 로 deploy.yml 재실행.
- `ddl-auto: update` 주의: **스키마를 바꾸는 릴리스는 additive-only** — 컬럼 의미 변경·
  NOT NULL 추가는 구 코드 롤백을 깨뜨린다. 매 배포 정지 상태 백업(5세대)이 최후 안전망.

## 배포 후 확인 (매 배포)

1. Actions 잡 그린 + deploy.sh 로그의 `DEPLOYED <digest>`.
2. `https://datedate.site` 200, 카카오 로그인 왕복(마이그레이션·nginx 변경 시).
3. **코인 봇**: 오픈 포지션이 있었다면 대시보드가 `PROTECTION-ONLY` 로 표시된다 —
   리스크 감시는 자동 재개된 상태이며, **신규 매매 재개는 Start 버튼 1회**
   (ADR trading/modes/0003). 서킷브레이커 연속손실 카운터는 리셋돼 있다.
4. 주식 봇: 오픈 포지션 존재 시 보호전용 자동재개(기존 동작, ADR stock/modes/0003).

## GitHub Secrets

| Secret | 용도 |
|---|---|
| `DEPLOY_SSH_HOST` | 서버 공인 IP/호스트명. **ephemeral 주소라 stop/start 시 바뀐다** — §외부 IP 가 바뀌었을 때 |
| `DEPLOY_SSH_USER` | `ourbalance_topping` |
| `DEPLOY_SSH_PORT` | **현재 22 라 등록 불필요** (`deploy.yml` 폴백). 확인은 `sudo sshd -T \| grep '^port'` — `sshd_config` 만 보면 `sshd_config.d/*.conf` 를 놓친다 |
| `DEPLOY_SSH_KEY` | 실행용 ed25519 **개인키 전문**(BEGIN/END 포함). 공개키는 서버 `~/.ssh/authorized_keys2` 에 `restrict,command="/usr/local/bin/calendar-deploy-gate"` 접두사와 함께 |
| `DEPLOY_SSH_RSYNC_KEY` | 번들 동기화용 개인키 전문. 공개키는 같은 파일에 `restrict,command="rrsync -wo /home/ourbalance_topping/apps/calendar"` |
| `DEPLOY_KNOWN_HOSTS` | `<호스트> ssh-ed25519 <base64>` 한 줄 (MITM 방지). 호스트 토큰은 `DEPLOY_SSH_HOST` 와 **글자 그대로** 일치해야 한다 |

**키는 로컬에서 만든다** — 개인키를 쥐는 쪽은 Actions 러너이고 서버는 공개키만 필요하다.
서버로 가는 것은 `.pub` 두 줄뿐이며, 서버에서 생성하면 개인키를 다시 빼내 지우는 단계가 는다.

**공개키는 `authorized_keys` 가 아니라 `authorized_keys2` 에.** GCP 게스트 에이전트가
`~/.ssh/authorized_keys` 를 덮어쓸 수 있고(공식 문서: "might be overwritten by the VM's guest
agent"), 이 서버는 `authorizedkeyscommand none` 이라 파일 방식이 살아 있다. sshd 의
`authorizedkeysfile` 은 `.ssh/authorized_keys .ssh/authorized_keys2` 두 개를 읽는데 에이전트가
관리하는 이름은 앞쪽 하나뿐이다. 지워지면 배포 키가 통째로 사라질 뿐 게이트가 풀리지는
않으므로 **가용성 문제**다. 점검: `grep -c '^restrict,command=' ~/.ssh/authorized_keys2` == 2.

**`DEPLOY_KNOWN_HOSTS` 는 `ssh-keyscan` 대신 서버 안에서 뜬다** — keyscan 은 TOFU 라 이 시크릿
(=MITM 방어선)을 신뢰 없는 채널로 만든다. 서버에서 `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`
로 지문을 대조한 뒤 `echo "<호스트> $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"`.

GHCR 푸시는 `GITHUB_TOKEN` 으로 충분. **서버 pull 용 토큰은 GitHub Secret 이 아니라** 서버
`~/.docker/config.json` 에 1회 저장한다 (`docker login ghcr.io --password-stdin`, chmod 600).
★**classic PAT + `read:packages` 스코프여야 한다** — 공식 문서: "GitHub Packages only supports
authentication using a personal access token (classic)." fine-grained 토큰은 ghcr.io 에
동작하지 않는다(발급 절차는 런북 3단계 6번). `config.json` 은 토큰을 base64 인코딩만 해서 담으므로
`chmod 600` 이 유일한 보호막이다. **만료일을 달력에 등록할 것** — 만료 증상은 "어느 날 갑자기
모든 배포가 exit 5".

## 외부 IP 가 바뀌었을 때 (현재 ephemeral)

서버 외부 IP 는 **예약되지 않은 ephemeral 주소**다. reboot 로는 안 바뀌지만 **stop/start 하면
바뀐다.** 바뀌는 순간 `DEPLOY_SSH_HOST` 와 `DEPLOY_KNOWN_HOSTS` **두 시크릿이 동시에 무효**가
되고, 증상은 배포 워크플로가 Set up SSH keys 다음 단계에서 `Permission denied` 또는
`Host key verification failed` 로 죽는 것이다. (같은 이유로 DNS A 레코드도 깨져 사이트 자체가
내려가므로, 복구 순서는 DNS 가 먼저다.)

**핵심 사실 — 호스트 키는 안 바뀐다.** `/etc/ssh/ssh_host_*` 는 부트 디스크에 있어 stop/start 를
넘어 유지된다. 그래서 `DEPLOY_KNOWN_HOSTS` 는 **IP 토큰만 갈아끼우고 base64 는 그대로 재사용**한다.
base64 까지 새로 떠야 하는 경우는 인스턴스 재생성·OS 재설치뿐이다.

### 복구 절차

1. 새 IP 확인:
   ```bash
   gcloud compute instances describe <인스턴스> --zone <존> \
     --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
   ```
2. **DNS A 레코드 먼저** (`datedate.site`, `www`) → `dig +short datedate.site` 로 전파 확인.
   사이트 복구가 배포 복구보다 급하다.
3. 서버에서 호스트 키가 그대로인지 대조:
   ```bash
   ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
   ```
   지문이 종전과 같으면 base64 재사용, 달라졌으면 새로 뜬다:
   ```bash
   echo "<새 IP> $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"
   ```
4. GitHub `production` Environment 시크릿 2개 갱신 — `DEPLOY_SSH_HOST` = 새 IP,
   `DEPLOY_KNOWN_HOSTS` = `<새 IP> ssh-ed25519 <기존 base64>`. 두 곳의 호스트 문자열이
   **글자 그대로** 같아야 한다.
5. 배포를 돌리기 전에 SSH 왕복만 먼저 확인:
   ```bash
   ssh -i ~/.ssh/calendar_deploy ourbalance_topping@<새 IP> "whoami"
   # 기대: REJECTED: 'whoami' (allowed: deploy <40-hex-sha|latest> [--force])
   ```

### 재발 방지 — 둘 중 하나

- **고정 IP 승격(권장, 근본 해결).** 사용 중인 주소를 그대로 예약하므로 IP 값은 유지된다:
  ```bash
  gcloud compute addresses create calendar-ip --addresses <현재 IP> --region <리전>
  ```
  인스턴스에 붙어 있는 동안의 과금은 ephemeral 과 같다. 인스턴스를 지우고 주소만 남기면
  유휴 주소 과금이 붙으므로 그때 `gcloud compute addresses delete` 로 해제할 것.
- **도메인을 `DEPLOY_SSH_HOST` 로.** `DEPLOY_SSH_HOST`·`DEPLOY_KNOWN_HOSTS` 의 호스트 토큰을
  모두 `datedate.site` 로 두면 IP 가 바뀌어도 **DNS 만 갱신**하면 되고 시크릿은 손댈 필요가 없다.
  조건: `dig +short datedate.site` 가 서버 IP 를 그대로 돌려줘야 한다 — CDN/프록시 뒤에 있으면
  22번이 통하지 않아 쓸 수 없다.

## 불변식 (수정 시에도 유지)

- **rsync `--delete` 는 `~/apps/calendar/deploy/` 하위에만.** `~/apps/calendar/` 루트를
  `--delete` 대상으로 삼는 rsync 는 금지 — 같은 층위에 `data/`(H2)·`logs/`·`.env`·`.deploy/`
  가 산다.
- **compose.yaml 은 `~/apps/calendar/compose.yaml`** (deploy/ 하위 아님) — 상대 볼륨 경로가
  compose 파일 위치 기준으로 해석되기 때문. deploy.sh preflight 가 매 배포 검증한다.
- nginx 컨테이너 자체를 재시작하는 배포(설정 변경)는 수 초의 TCP 거부 창이 있다 —
  유지보수 페이지로 가릴 수 없으므로 트래픽 적은 시간대에.
- 인증서 갱신은 호스트 certbot(webroot). 훅·dry-run 검증 절차는 런북 8단계 — 파손 증상은
  약 60일 뒤에야 발현하므로 nginx 설정 변경 시 `sudo certbot renew --dry-run` 재확인.
