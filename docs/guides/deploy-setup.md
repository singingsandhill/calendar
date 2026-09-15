# 배포 세팅 완료 체크리스트 (한시 문서)

> 파이프라인이 서버에 올라가 첫 배포가 성공하면 이 문서는 역할이 끝난다.
> 절차 상세는 재수록하지 않는다 — 상시 운영은 [deployment.md](../operations/deployment.md),
> 서버 1회성 이관은 [server-migration-runbook.md](../operations/server-migration-runbook.md),
> 결정 근거는 [ADR common/infrastructure/0001·0002](../adr/common/infrastructure/).
> 여기엔 **지금 어디까지 됐는지 · 무엇이 무엇을 막는지 · 다른 문서에 없는 실무 명령**만 적는다.
> 막혔을 때 증상별 해결은 [troubleshooting/deploy-cicd.md](../troubleshooting/deploy-cicd.md).

## TL;DR

| | |
|---|---|
| 무엇이 막고 있었나 | `deploy/server/calendar-deploy-gate.sh` 의 shellcheck directive 문법 오류 → CI 가 빌드 전에 죽음 |
| 그래서 무슨 일이 | **GHCR 이미지가 한 번도 발행되지 않았다** — 서버 pull 도 배포 버튼도 성립 불가 |
| 다음 한 수 | `authorized_keys2` 이관 → main 푸시로 CI 그린 → `deploy` 브랜치 origin 푸시로 첫 이미지 → GitHub 설정 → 서버 |

## 1. 진행 상황 (2026-09-10)

### ✅ 완료 — 실측으로 확인된 것

| 항목 | 확인 근거 |
|---|---|
| 배포 산출물 (`Dockerfile`·`compose.yaml`·`nginx/`·`deploy.sh`·게이트) 커밋 | `577db0c` |
| 워크플로 2종 커밋 | `4d90a70` |
| **CI 린트 실패 원인 수정** (작업 트리) | 로컬 `shellcheck -S warning` exit 0 (§2·§5) |
| **SSH 키 2개 로컬 생성** | 서버 `~/.ssh/` 에 개인키 없음 = 로컬 생성 확인 |
| **서버 `~/.ssh/authorized_keys` 에 restrict 2줄** | `grep -c '^restrict,command='` == 2, 항목당 1줄 |
| **키 파일 권한** | `~` 750 / `~/.ssh` 700 / `authorized_keys` 600, 소유자 일치 |
| **`DEPLOY_SSH_PORT` 판정 → 등록 불필요** | `sudo sshd -T` 가 `port 22` |
| **`DEPLOY_KNOWN_HOSTS` 값 확정** | 서버 호스트 키 지문이 로컬 최초 접속이 물어본 값과 일치 (TOFU 아님) |
| **`DEPLOY_SSH_HOST` 값 확인 + ephemeral 판정** | 예약 주소 아님 → §4 |

### ☐ 남은 것

| 항목 | 절 | 비고 |
|---|---|---|
| `authorized_keys` → **`authorized_keys2` 이관** | §4 | `authorizedkeyscommand none` 발견 후 추가된 작업. 파일이 달라 위 "restrict 2줄"과 별개다 |
| 수정본 main 푸시 → CI 그린 | §A-1 | |
| `deploy` 브랜치 origin 푸시 → **첫 이미지 발행** | §A-2·3 | 로컬에만 있음(`c26567b`). 발행 트리거가 `refs/heads/deploy` |
| `production` Environment + 시크릿 **5개** | §B | PORT 제외 |
| 서버 런북 1·2단계 (실측·백업) | 런북 | `id -g`·TZ·rrsync 경로 실측이 3단계 입력값 |
| classic PAT + GHCR 로그인 | 런북 3-6 | **이미지 발행 이후에만 성공** |
| 번들 반입 → 게이트 설치 | 런북 3-7·8 | 이 순서가 뒤집히면 `No such file` |
| 런북 4~8단계 (정지·이관·기동·nginx·certbot) | 런북 | |
| 배포 리허설 | §D / 런북 9 | |

## 2. 왜 막혀 있었나 — CI 실패 원인

`ci.yml` 의 `Lint deploy scripts` 단계에서 shellcheck 가 파싱 에러로 죽었다.

```
# shellcheck disable=SC2086 -- force 는 정규식으로 ' --force' 만 허용됨
                            ^-- SC1072 (error): Expected '=' after directive key
^-- SC1073 (error): Couldn't parse this shellcheck directive
```

shellcheck directive 줄은 **`key=value` 토큰만** 받는다. `--` 를 다음 directive 키로 읽고 `=` 를
찾지 못해 error 2건을 낸다 — `-S warning` 임계 위라 잡이 exit 1. 같은 실행에서 `deploy/deploy.sh`
는 무출력(clean)이었으므로 결함은 이 한 줄뿐이었다.

파급이 한 줄보다 크다. 이 단계는 `Build & test` 스텝보다 **앞**이고 이미지 발행 잡은
`needs: build-test` 다. 그래서 `4d90a70` 이후 어떤 커밋도 이미지가 되지 않았고, 런북 3.6 의
`docker pull ghcr.io/singingsandhill/calendar:latest` 도 `deploy.yml:49` 의 GHCR manifest
선검증도 통과할 수 없는 상태였다.

**재발 방지 규칙 — directive 줄에는 `key=value` 외 토큰을 두지 않는다. 근거는 바로 윗줄에 별도
주석으로.** (로컬에서 확인한 v0.11.0 은 트레일링 `#` 주석 형태를 받는다. 그래도 CI 는 `ubuntu-latest`
기본 설치본을 쓰고 버전을 고정하지 않으므로, 어느 버전에서나 파싱되는 별도 줄 형태를 쓴다.)

## 3. 남은 작업 — 의존성 순서

```
A. 저장소 — main 푸시로 CI 그린 → deploy 푸시로 GHCR 첫 이미지
      │
      ├──► B. GitHub — production Environment + 시크릿 5개
      │
      └──► C. 서버 — 런북 1~8단계   ← 3.6 의 GHCR pull 이 A 를 기다린다
                     │
                     └──► D. 배포 리허설 (런북 9단계) ──► 정리 (런북 10단계)
```

**A 를 건너뛰고 C 를 시작하면 런북 3.6 에서 막힌다** — 받을 이미지가 없다. B 와 C 는 서로
독립이라 순서는 자유.

### A. 저장소 — CI 그린 → 첫 이미지

이미지는 **`deploy` 브랜치 푸시에서만** 발행된다 (main 은 빌드·테스트만 —
[ADR 0003](../adr/common/infrastructure/0003-image-publish-on-deploy-branch.md)). 그래서 두 단계다.

1. 게이트 스크립트 directive 수정분을 main 에 푸시 → Actions 의 `CI` 에서 `build-test` 그린 확인.
   (이 실행에는 `push-image` 잡이 없다. 정상이다.)
2. **`deploy` 브랜치를 origin 에 올린다** — 지금 로컬에만 있다:
   ```bash
   git switch deploy && git merge --ff-only main
   git push -u origin deploy
   ```
3. `deploy` 푸시로 도는 `CI` 에서 `build-test` → `push-image` 두 잡이 모두 그린인지 확인.
4. 저장소 Packages 에 `calendar` 패키지가 생기고 태그 `latest` + `<40자 git sha>` 두 개가
   보이는지 확인. **이 SHA 가 D 의 첫 배포 입력값이다.**

이후 릴리스도 같다 — main 에서 개발하고, 내보낼 시점에 `main → deploy` fast-forward 푸시.

### B. GitHub — Environment + 시크릿

`deploy.yml:29` 가 `environment: production` 을 선언한다. 시크릿을 레포 레벨이 아니라 이
Environment 에 넣으면 다른 워크플로에서 보이지 않는다.

1. Settings → Environments → `production` 생성.
2. **Required reviewers** 에 본인 추가 (실계좌 LIVE 봇 서버 — 버튼 오조작에 사람 확인 한 겹).
   ⚠️ **"Prevent users from approving workflow runs that they triggered" 는 켜지 않는다.**
   1인 운영이라 켜면 본인이 dispatch 한 배포를 본인이 승인할 수 없어 영구 대기한다.
3. Environment secrets **5개** 등록 (`DEPLOY_SSH_PORT` 는 아래 이유로 불필요):

| Secret | 값 | 만드는 법 |
|---|---|---|
| `DEPLOY_SSH_HOST` | 서버 공인 IP | `gcloud compute instances describe <인스턴스> --zone <존> --format='get(networkInterfaces[0].accessConfigs[0].natIP)'` — **ephemeral 이라 바뀔 수 있다**(§4) |
| `DEPLOY_SSH_USER` | `ourbalance_topping` | 고정 (게이트 스크립트의 `APP_DIR` 경로와 일치해야 함) |
| ~~`DEPLOY_SSH_PORT`~~ | **등록하지 않는다** | `sudo sshd -T \| grep '^port'` 가 22 로 확인됨 → `deploy.yml:36` 폴백이 처리. `sshd_config` 만 보면 `sshd_config.d/*.conf` 를 놓치니 `sshd -T` 로 볼 것 |
| `DEPLOY_SSH_KEY` | 실행용 **개인키 전문** (`BEGIN`/`END` 줄 포함, 줄바꿈 유지) | `ssh-keygen -t ed25519 -N "" -C "calendar-deploy" -f ~/.ssh/calendar_deploy` |
| `DEPLOY_SSH_RSYNC_KEY` | rsync 용 **개인키 전문** | `ssh-keygen -t ed25519 -N "" -C "calendar-rsync" -f ~/.ssh/calendar_rsync` |
| `DEPLOY_KNOWN_HOSTS` | `<호스트> ssh-ed25519 <base64>` 한 줄 | **서버 안에서** 뜬다(아래) |

   **키 생성은 로컬에서.** 개인키를 쥐는 쪽은 Actions 러너이고 서버는 공개키만 필요하다 —
   서버에서 만들면 개인키를 다시 빼내 지우는 단계가 는다. 서버로 가는 것은 `.pub` 두 줄뿐이다.

   **`DEPLOY_KNOWN_HOSTS` 는 `ssh-keyscan` 을 쓰지 않는다.** keyscan 은 TOFU 라 이 시크릿
   (=MITM 방어선)을 신뢰 없는 채널로 만든다. gcloud 로 들어간 서버 안에서 직접:
   ```bash
   ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub    # 지문을 로컬 ssh 가 물어본 값과 대조
   echo "<서버IP> $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"
   ```
   호스트 토큰은 `DEPLOY_SSH_HOST` 와 **글자 그대로** 같아야 한다(IP↔호스트명 혼용 금지).

4. 두 **공개키**(`.pub`)는 서버 **`~/.ssh/authorized_keys2`** 에 런북 8단계의
   `restrict,command=` 접두사와 함께 넣는다(`authorized_keys` 가 아니다 — §4).
   접두사 없이 넣으면 배포 키가 그냥 셸 접근 키가 된다 — 이 키는 docker 그룹(=root 동등)
   유저로 이어진다. 항목당 정확히 한 줄이고 옵션이 줄 맨 앞이어야 한다.

### C. 서버 — 런북으로

[server-migration-runbook.md](../operations/server-migration-runbook.md) 1~8단계를 그대로
따른다. 3단계는 **번들 반입(7) → 게이트 설치·authorized_keys2(8) → Secrets(9)** 순서이고,
7단계 nginx 전환은 `sudo mkdir -p /var/www/certbot` 이 먼저다 — 둘 다 런북에 반영돼 있다.

### D. 배포 리허설

런북 9단계. dispatch 화면의 **"Use workflow from" 에서 `deploy` 를 고르고**, `image_tag` 에는
**A-4 에서 확인한 40자 SHA** 를 명시적으로 넣는다. 입력을 비우면 고른 브랜치의 `github.sha` 가
쓰이는데, main 을 골랐다면 아직 이미지가 없는 SHA 일 수 있다 — 그 경우 GHCR manifest 선검증이
서버 접속 전에 실패한다(안전망은 있지만 헛걸음).

## 4. 함정 (이번 조사에서 확인된 것)

- **`deploy` 브랜치는 origin 에 있어야 트리거된다.** 발행 조건이 `refs/heads/deploy` 라
  로컬 브랜치만으로는 아무 일도 일어나지 않는다.
- **브랜치명 `deploy` 가 `deploy/` 디렉터리와 모호하다.** `git log deploy` 는
  `fatal: ambiguous argument` 로 실패한다 — `git log deploy --` 또는 `refs/heads/deploy` 로 쓴다.
  `git switch`/`git push` 는 브랜치만 받으므로 영향 없다.
- **공개키는 `authorized_keys2` 에 둔다.** GCP 게스트 에이전트가 `~/.ssh/authorized_keys` 를
  덮어쓸 수 있다(공식 문서: "might be overwritten by the VM's guest agent"). 이 서버는
  `authorizedkeyscommand none` 이라 파일 방식이 살아 있고, `authorizedkeysfile` 이
  `.ssh/authorized_keys .ssh/authorized_keys2` 둘을 읽는데 에이전트가 관리하는 이름은 앞쪽
  하나뿐이다. 지워져도 게이트가 풀리는 게 아니라 배포 키가 통째로 사라질 뿐 — **가용성** 문제다.
  이관(이미 `authorized_keys` 에 넣었다면):
  ```bash
  grep '^restrict,command=' ~/.ssh/authorized_keys > ~/.ssh/authorized_keys2
  chmod 600 ~/.ssh/authorized_keys2
  grep -c '^restrict,command=' ~/.ssh/authorized_keys2      # 기대: 2
  ```
  원본은 **게이트 설치 후 실제 접속이 확인될 때까지** 그대로 둔다(양쪽에 있어도 sshd 가 첫
  매칭을 쓰므로 무해). 확인되면 `sed -i '/^restrict,command=/d' ~/.ssh/authorized_keys`.
  에이전트가 실제로 덮어쓰는지 즉시 보려면 `sudo systemctl restart google-guest-agent` 후
  두 파일의 `grep -c` 를 비교한다(서버 세션은 열어둔 채).
- **외부 IP 가 ephemeral 이다.** stop/start 하면 바뀌고 그 순간 `DEPLOY_SSH_HOST` 와
  `DEPLOY_KNOWN_HOSTS` 두 시크릿이 동시에 무효가 된다(DNS A 레코드도 함께). 호스트 키는
  부트 디스크에 있어 안 바뀌므로 known_hosts 는 **IP 토큰만 교체**하면 된다. 복구 절차와
  고정 IP 승격은 [deployment.md §외부 IP 가 바뀌었을 때](../operations/deployment.md).
- **`.deploy/env` 는 커밋된 템플릿이 없다.** `compose.yaml` 이 `APP_REF`·`HOST_TZ`·`JAVA_OPTS`·
  `APP_GID` 를 `:?` fail-fast 로 요구하는데 스펙은 프로즈 주석뿐이다. 런북 3.5 에서 만들 실제 내용:
  ```
  HOST_TZ=<timedatectl 실측값>
  JAVA_OPTS=-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError
  APP_GID=<id -g 값>
  ```
  `APP_REF` 는 `deploy.sh` 가 매 배포 기록한다 — 첫 기동분만 런북 6.1 로 수동 추가.
- **`/var/www/certbot` 은 nginx 를 올리기 전에 만든다.** 바인드 마운트 소스가 없으면 Docker 가
  대신 만들며 소유권·컨텍스트가 의도와 달라진다. 원래 런북은 이 명령을 8단계에 두었는데
  nginx 기동이 7단계라 순서가 뒤집혀 있었다 — **7단계 2번으로 옮겨 반영 완료**.
- **nginx 는 인증서가 이미 있어야 뜬다.** `deploy/nginx/datedate.conf` 가
  `/etc/letsencrypt/live/datedate.site/{fullchain,privkey}.pem` 을 참조하는데 이걸 만들어 주는
  코드는 저장소 어디에도 없다. 없으면 앱이 아니라 **엣지 전체**가 죽는다 (이관이라 기존 인증서가
  그대로 쓰이지만, 도메인·서버가 바뀌면 선결 조건).
- **게이트 스크립트는 서버에서 `/usr/local/bin/calendar-deploy-gate` 복사본이다.**
  `deploy.yml` 의 rsync 는 `~/apps/calendar/deploy/server/` 만 갱신한다 → 이 파일을 고칠 때마다
  런북 3단계 8번의 `sudo cp` 를 다시 해야 반영된다. (이번 §2 수정분은 서버 미설치라 무관 — 런북
  3.9 의 첫 번들 반입이 수정본을 가져간다.)
- **서버 `.env` 의 boot 필수 키는 3개뿐** — `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`.
  나머지는 전부 yaml 기본값이 있다. `spring.config.import` 가 `optional:` 이라 `.env` 가 없어도
  즉시 죽지 않고 DataSource 생성 시점에 죽는다 — 그래서 `deploy.sh` preflight 가 파일 존재를
  먼저 검사한다.
- **GHCR 토큰은 반드시 classic PAT.** 공식 문서: "GitHub Packages only supports authentication
  using a personal access token (classic)." fine-grained 토큰은 ghcr.io 에 동작하지 않는다 —
  스코프는 `read:packages` 하나면 된다. GitHub Secret 이 아니라 서버 `~/.docker/config.json` 에
  1회 저장하고(런북 3단계 6번에 발급 절차), 그 파일은 토큰을 base64 인코딩만 해서 담으므로
  `chmod 600` 이 유일한 보호막이다. 만료 증상은 "어느 날 갑자기 모든 배포가 exit 5" —
  [deployment.md](../operations/deployment.md#github-secrets) 에 만료 관리 메모.
- **`rrsync` 가 PATH 에 없으면 절대경로로 써야 한다.** forced command 는 최소 PATH 로 실행돼
  `/usr/share/rsync/scripts/rrsync` 를 못 찾고, 증상은 rsync 만 조용히 실패하는 것이다.
  런북 1단계에서 `command -v rrsync` 로 미리 확인한다.
- **`id -g` 는 `sudo` 없이 배포 유저로 실행한다.** `sudo id -g` 는 0(root) 이고, 그 값이
  `.deploy/env` 의 `APP_GID` 에 들어가면 컨테이너가 root 그룹으로 돈다.

## 5. 푸시 전 로컬 선검증

CI 의 lint 단계와 같은 검사를 미리 돌린다. WSL 에 shellcheck 가 설치돼 있지 않으므로 npx 래퍼로
바이너리를 받아 쓴다.

```bash
bash -n deploy/deploy.sh && bash -n deploy/server/calendar-deploy-gate.sh
npx -y shellcheck@4.1.0 -S warning deploy/deploy.sh deploy/server/calendar-deploy-gate.sh
```

무출력 + exit 0 이면 CI 의 해당 단계는 통과한다. `-S warning` 을 빼면 info 등급까지 나오는데,
`deploy.sh` 의 SC2012(`ls` 대신 `find`) 1건은 임계 아래의 기존 항목이라 CI 에 영향이 없다.

> `run: |` 블록 안의 셸(특히 `deploy.yml` 의 rsync·ssh 조립)은 shellcheck 대상이 아니다 —
> 이 부분은 리뷰로만 지킨다.
