# 배포 CI/CD 구축 이슈 (2026-09-10)

파이프라인을 처음 세우며 실제로 부딪힌 문제와 해결을 모았다. 절차 자체는
[deployment.md](../operations/deployment.md)(상시 운영) ·
[server-migration-runbook.md](../operations/server-migration-runbook.md)(1회성 서버 이관) ·
[deploy-setup.md](../guides/deploy-setup.md)(세팅 체크리스트), 결정 근거는
[ADR common/infrastructure/](../adr/common/infrastructure/).

---

# 1부. 이 파이프라인은 어떻게 굴러가는가

## 왜 흔한 구조를 못 쓰는가 — 제약 4개

| 제약 | 결과 |
|---|---|
| H2 **파일 DB 배타 락** | 두 번째 JVM 이 아예 기동하지 못한다 → blue-green 불가 |
| `@Scheduled` 잡이 **분산 락 없이** 돈다 | 신·구 인스턴스가 겹치면 실주문이 이중 실행된다 |
| e2-micro **RAM 958MiB** | JVM 2개가 물리적으로 안 올라간다 |
| **실계좌 LIVE 봇**이 이 서버에서 돈다 | 배포 = 봇 재시작 = 보호전용 강등 + 서킷브레이커 카운터 리셋 |

앞의 셋이 무중단 배포를 막고, 넷째가 자동 배포를 막는다. 그래서 결론은
**"단일 인스턴스 재시작 배포 + 사람이 누르는 버튼"** 이다 (ADR 0001). 1~3분의 계획된 중단은
nginx 503 유지보수 페이지로 흡수한다 (ADR 0002).

## 3단 구조

```
① 개발        main 푸시/PR ──► ci.yml: gradle build+test + deploy 스크립트 shellcheck
                                                                        [이미지 발행 없음]
② 릴리스 후보  deploy 푸시  ──► ci.yml: 위 + Dockerfile 빌드 → GHCR push (:<sha>, :latest)
                                                                        [서버 무영향]
③ 반영        Actions 버튼 ──► deploy.yml ──ssh──► 게이트 ──► deploy.sh  [1~3분 중단]
```

**①과 ②를 나눈 이유**: 발행 브랜치가 곧 개발 브랜치면 `:latest` 가 문서 커밋마다 옮겨다녀
"지금 latest 가 무엇인가" 가 릴리스 의도와 무관해진다. 릴리스는 `main → deploy` fast-forward
푸시 한 번이다 (ADR 0003).

**②와 ③을 나눈 이유**: 이미지를 만드는 것과 서버에 반영하는 것은 다른 위험이다. 발행은
서버를 건드리지 않으므로 자동이어도 되지만, 반영은 봇을 재시작시키므로 시점을 사람이 고른다.

## ③이 실제로 하는 일

**러너 쪽 (`deploy.yml`)**

| 순서 | 하는 일 | 무엇을 막는가 |
|---|---|---|
| 1 | `image_tag` 를 `^([0-9a-f]{40}\|latest)$` 로 검증 | 입력이 셸 문자열에 보간돼 러너·서버 양쪽에서 임의 명령이 되는 인젝션 |
| 2 | GHCR `docker manifest inspect` 선검증 | 오타 태그가 서버까지 가서 실패하는 헛걸음 |
| 3 | 시크릿을 `env` 매핑 → `printf` 로 파일 기록 | `${{ }}` 직접 보간(로그 유출·인젝션) |
| 4 | `rsync -az --delete deploy/` | `--delete` 를 루트에 걸어 `data/`·`logs/`·`.env` 를 날리는 사고 |
| 5 | `ssh "deploy <tag> [--force]"` 한 줄 | 트레일링 스페이스가 게이트 정규식과 어긋나는 것 |

**서버 게이트 (`/usr/local/bin/calendar-deploy-gate`, root 소유)**

배포 키는 `authorized_keys2` 에 `restrict,command="..."` 로 묶여 있어, 어떤 명령을 보내도
이 스크립트만 실행된다. 스크립트는 `SSH_ORIGINAL_COMMAND` 를 정규식으로 검사해 통과분만
`deploy.sh` 로 넘기고 나머지는 `REJECTED` + exit 1. **배포 유저는 docker 그룹(=root 동등)이라,
이 게이트가 없으면 키 하나가 곧 서버 루트다.**

**`deploy.sh` (서버)**

```
preflight ─► 시간창 가드 ─► 선-pull ─► 다이제스트 고정 ─► :05 틱 회피 대기
   └─► stop ─► 정지상태 백업 ─► up ─► 헬스 게이트 ─► 엣지 게이트 ─► 기록·GC
                                          └─(실패)─► 직전 다이제스트로 자동 롤백
```

| 게이트 | 무엇을 막는가 |
|---|---|
| preflight (`compose config` 로 볼륨 해석 검증) | 경로 회귀로 **빈 DB 를 새로 만들어 기동**하는 조용한 데이터 유실 |
| 주식 LIVE 시간창(평일 09:15~11:25 KST) | 장중 봇 정지 |
| **다이제스트 고정** | `latest` 는 pull 시 로컬 캐시가 덮이므로 태그 롤백 = 같은 불량 이미지 재기동 |
| 크립토 :05 틱 회피 | 매매 루프 한복판의 SIGTERM |
| 정지 상태 H2 백업 5세대 | `ddl-auto: update` 스키마 변경 롤백 |
| 헬스 게이트 | 컨테이너 사망 즉시 실패 + `/actuator/health/deploy` UP 폴링 |
| 엣지 게이트 | 앱만 살고 사이트는 죽은 상태를 성공으로 오보하는 것 |

종료코드가 곧 잡 결과다: `0` 성공 / `2` 가드·preflight 거부 / `3` 롤백 성공(그래도 실패 표시 —
조사 유도) / `4` 롤백도 실패(앱 정지, 유지보수 페이지 유지) / `5` pull·레지스트리 실패.

---

# 2부. 실제로 부딪힌 문제들

## 1. CI 가 빌드도 못 해보고 죽는다 — shellcheck SC1072/SC1073

### 증상
```
In deploy/server/calendar-deploy-gate.sh line 18:
  # shellcheck disable=SC2086 -- force 는 정규식으로 ' --force' 만 허용됨
  ^-- SC1073 (error): Couldn't parse this shellcheck directive.
                                ^-- SC1072 (error): Expected '=' after directive key.
Error: Process completed with exit code 1
```
CI 도입 커밋 이후 **main 푸시 전건**이 `Lint deploy scripts` 단계에서 실패.

### 원인
shellcheck directive 줄은 **`key=value` 토큰만** 받는다. 파서가 `--` 를 다음 directive 키로 읽고
`=` 를 찾지 못해 error 등급 2건을 낸다. CI 임계가 `-S warning` 이라 잡이 exit 1.

**파급이 한 줄보다 컸다.** 이 단계는 `Build & test` 보다 앞이고 이미지 발행 잡은
`needs: build-test` 다 → **GHCR 이미지가 한 번도 발행되지 않았다.** 서버 pull 도 배포 버튼도
성립할 수 없는 상태였고, 원인은 셸 주석 한 줄이었다.

### 해결방법
근거 주석을 directive **위 별도 줄**로 올린다. directive 는 대상 명령 바로 위에 남겨야 한다.

```bash
  # force 는 게이트 정규식이 ' --force' 만 통과시킨다 — 의도된 무인용 확장
  # shellcheck disable=SC2086
  exec bash "$APP_DIR/deploy/deploy.sh" "$tag" $force
```

트레일링 `#` 주석(`disable=SC2086 # 설명`)도 v0.11.0 에서는 통과하지만 CI 가 `ubuntu-latest`
기본 설치본을 쓰고 **버전을 고정하지 않으므로** 버전 의존 문법을 쓰지 않는다.

### 확인
```bash
npx -y shellcheck@4.1.0 -S warning deploy/deploy.sh deploy/server/calendar-deploy-gate.sh
# 무출력 + exit 0
```
directive 가 살아 있는지까지 보려면 severity 필터를 빼고 돌린다. 게이트 스크립트가 무출력이면
SC2086 이 실제로 억제된 것이고, directive 를 지운 사본에서는 SC2086 이 다시 떠야 한다.

### 재발 방지
**shellcheck directive 줄에는 `key=value` 외 토큰을 두지 않는다. 근거는 윗줄에 별도 주석으로.**
`deploy/` 하위 셸을 고치면 푸시 전에 아래를 돌린다.

---

## 2. WSL 에 shellcheck 가 없어 CI 를 재현할 수 없다

### 증상
`command -v shellcheck` 가 비어 있고, docker 도 WSL 통합이 꺼져 있어 CI 와 같은 검사를 로컬에서
돌릴 방법이 없다. CI 도입 커밋이 "shellcheck 는 ci.yml 이 수행 — 로컬 미설치" 라며 선검증을
건너뛴 것이 문제 1의 직접 원인이었다.

### 해결방법
npm 래퍼가 실제 바이너리를 내려받는다. 설치 불필요.
```bash
bash -n deploy/deploy.sh && bash -n deploy/server/calendar-deploy-gate.sh
npx -y shellcheck@4.1.0 -S warning deploy/deploy.sh deploy/server/calendar-deploy-gate.sh
```
래퍼 4.1.0 → shellcheck v0.11.0. 출력이 GitHub Actions 로그와 **바이트 동일**해 RED 재현·GREEN
확인에 그대로 쓸 수 있다.

### 참고
`-S warning` 을 빼면 info 등급까지 나온다. `deploy.sh` 의 SC2012(`ls` 대신 `find`) 1건은
임계 아래의 기존 항목이라 CI 에 영향이 없다.

---

## 3. "main push 가 배포 트리거" — 실제로는 아니다

### 증상
main 에 푸시하면 서버가 바뀐다고 인지하고 있었으나, 실제로는 서버가 그대로다.

### 원인
`deploy.yml` 은 `on: workflow_dispatch:` **단독**이다. main 푸시에 반응하는 것은 `ci.yml` 뿐이고
그 결과물은 GHCR 이미지지 서버 반영이 아니다. ADR 0001 이 "main 푸시 자동 배포" 를 대안표에서
**명시적으로 기각**했다 — 근거는 "문서 커밋에도 봇이 재시작(보호전용 강등 + 서킷브레이커
카운터 리셋)".

### 해결방법
오인이 계속 재생산되는 구조 자체를 고쳤다. 이미지 발행을 `deploy` 릴리스 브랜치로 분리해
main 푸시가 만드는 것이 아무것도 없게 했다 (ADR 0003). 서버 반영은 그대로 수동 버튼.

### 확인
```bash
python3 -c "
import yaml; d=yaml.safe_load(open('.github/workflows/ci.yml'))
print(d['jobs']['push-image']['if'])"
# github.event_name == 'push' && github.ref == 'refs/heads/deploy'
```

### 교훈
**"어느 이벤트가 무엇을 하는가" 는 워크플로 `on:` 과 잡 `if:` 를 직접 읽어 확인한다.**
기억이나 파일 이름(`deploy.yml`)으로 추정하지 않는다.

---

## 4. `git log deploy` 가 fatal: ambiguous argument

### 증상
```
$ git log --oneline -3 deploy
fatal: ambiguous argument 'deploy': both revision and filename
```
브랜치가 분명히 있는데 조회가 실패해 "브랜치가 없다" 고 오판하기 쉽다.

### 원인
브랜치명 `deploy` 와 저장소의 `deploy/` 디렉터리가 같은 이름이라 git 이 revision 인지 경로인지
결정하지 못한다.

### 해결방법
```bash
git log deploy --            # -- 로 revision 임을 명시
git log refs/heads/deploy    # 또는 full ref
git rev-parse deploy         # 이건 원래 모호하지 않다
```
`git switch` / `git push` / `git merge` 는 브랜치만 받으므로 영향이 없다. 이름은 유지하고
함정만 문서화했다.

---

## 5. 런북대로 하면 `No such file` — 게이트 설치가 번들 반입보다 앞

### 증상
런북 3단계 7번의
```bash
sudo cp ~/apps/calendar/deploy/server/calendar-deploy-gate.sh /usr/local/bin/calendar-deploy-gate
```
이 소스 파일 없음으로 실패.

### 원인
그 파일은 같은 단계 **9번(첫 번들 반입)** 에서야 서버에 도착한다. 문서 순서대로 실행하면
아직 없는 파일을 복사하는 셈이다.

같은 종류가 하나 더 있었다 — 7단계에서 `docker compose up -d nginx` 를 하는데
`sudo mkdir -p /var/www/certbot` 은 8단계에 있었다. 바인드 마운트 소스가 없으면 Docker 가
대신 만들며 소유권·컨텍스트가 의도와 달라진다.

### 해결방법
런북을 재정렬했다.

| 이전 | 이후 |
|---|---|
| 7 게이트 설치 → 8 Secrets → 9 번들 반입 | **7 번들 반입 → 8 키·게이트·authorized_keys2 → 9 Secrets** |
| 8단계의 `mkdir /var/www/certbot` | **7단계 2번**으로 이동 (nginx 기동 앞) |

### 확인
```bash
python3 -c "
import io,re
prev=0
for ln,l in enumerate(io.open('docs/operations/server-migration-runbook.md',encoding='utf-8'),1):
    if l.startswith('## '): prev=0; continue
    m=re.match(r'^(\d+)\. ',l)
    if m:
        n=int(m.group(1))
        if n!=prev+1: print('불연속',ln,prev,'->',n)
        prev=n"
```

### 교훈
**절차 문서는 "각 단계가 요구하는 입력이 그 앞 단계에서 만들어지는가" 로 검산한다.**
번호 순서와 의존 순서는 저절로 일치하지 않는다.

---

## 6. 로컬 `ssh user@host` 가 Permission denied (publickey)

### 증상
```
ourbalance_topping@35.237.128.174: Permission denied (publickey).
```
gcloud 로는 잘 들어가지는 서버인데 맨 `ssh` 는 거부당한다.

### 원인
`gcloud compute ssh` 는 `~/.ssh/google_compute_engine` 키를 쓰고, 맨 `ssh` 는 그 키를 쓰지 않는다.
로컬에 서버가 받아주는 키가 없는 상태였다.

### 해결방법
공개키를 서버에 넣는 작업은 **기존에 되는 접속 경로**로 한다. 둘 중 하나.

```bash
# (A) gcloud 를 통해 파이프 — 복붙 오타가 원천 차단된다
{
  echo
  printf 'restrict,command="/usr/local/bin/calendar-deploy-gate" '
  cat ~/.ssh/calendar_deploy.pub
  printf 'restrict,command="rrsync -wo /home/ourbalance_topping/apps/calendar" '
  cat ~/.ssh/calendar_rsync.pub
} | gcloud compute ssh <인스턴스> --zone <존> --command \
    'umask 077; mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys2; chmod 700 ~/.ssh; chmod 600 ~/.ssh/authorized_keys2'
```
```bash
# (B) 서버에서 heredoc — .pub 내용을 붙여넣는다. 항목당 정확히 한 줄
cat >> ~/.ssh/authorized_keys2 <<'EOF'
restrict,command="/usr/local/bin/calendar-deploy-gate" ssh-ed25519 AAAA... calendar-deploy
EOF
```
맨 앞 `echo` 는 기존 파일이 개행 없이 끝났을 때 마지막 줄에 들러붙는 것을 막는다(빈 줄은
sshd 가 무시). `<<'EOF'` 의 따옴표는 필수 — 없으면 셸이 내용을 건드린다.

### 참고 — 키를 어디서 만드는가
**로컬에서 만든다.** 개인키를 쥐는 쪽은 Actions 러너이고 서버는 공개키만 필요하다. 서버에서
만들면 개인키가 서버에 남아 다시 빼내고 지우는 단계가 는다. 서버로 가는 것은 `.pub` 두 줄뿐이다.

### 확인
```bash
chmod go-w ~                                  # StrictModes: 홈이 group/world 쓰기면 키를 무시한다
ls -ld ~ ~/.ssh; ls -l ~/.ssh/authorized_keys2 # 750 / 700 / 600, 소유자 일치
grep -c '^restrict,command=' ~/.ssh/authorized_keys2   # 2
```

---

## 7. GCP 게스트 에이전트가 `authorized_keys` 를 덮어쓸 수 있다

### 증상
아직 발현하지 않았지만, GCP 공식 문서가 명시한다.
> "Public SSH keys that you add directly to a VM's `~/.ssh/authorized_keys` files
> **might be overwritten by the VM's guest agent**."

발현하면 어느 날 갑자기 배포 워크플로가 `Permission denied (publickey)` 로 죽는다.

### 진단
```bash
sudo sshd -T | grep -iE '^port|authorizedkeys'
```
```
port 22
authorizedkeyscommand none
authorizedkeysfile .ssh/authorized_keys .ssh/authorized_keys2
```
`authorizedkeyscommand` 가 `/usr/bin/google_authorized_keys` 면 에이전트가 파일을 안 건드리므로
안전하다. **`none` 이면 파일 방식이고 위 경고가 살아 있다.** 이 서버는 `none` 이었다.

### 원인 / 위험의 성격
에이전트가 관리하는 파일명은 `authorized_keys` **하나뿐**인데 sshd 는 `authorizedkeysfile` 에
적힌 **두 파일**을 읽는다.

위험은 **보안이 아니라 가용성**이다. 배포 공개키는 `restrict,command=` 접두사가 붙은 줄에만
존재하므로, 지워지면 함께 사라질 뿐 게이트만 풀린 키가 남지 않는다.

### 해결방법
두 번째 파일로 옮긴다. sshd 설정 변경도 root 도 재시작도 필요 없다.
```bash
grep '^restrict,command=' ~/.ssh/authorized_keys > ~/.ssh/authorized_keys2
chmod 600 ~/.ssh/authorized_keys2
grep -c '^restrict,command=' ~/.ssh/authorized_keys2      # 기대: 2
```
원본은 게이트 설치 후 실제 접속이 확인될 때까지 그대로 둔다(양쪽에 있어도 sshd 가 첫 매칭을
쓰므로 무해). 확인되면 `sed -i '/^restrict,command=/d' ~/.ssh/authorized_keys`.

> ⚠️ **`authorized_keys` 에 추가하는 것과 `authorized_keys2` 로 이관하는 것은 별개 작업이다.**
> 파일이 다르다. 실제로 이 지점에서 혼동이 있었다.

### 확인
에이전트가 정말 덮어쓰는지 며칠 기다릴 필요 없이 강제 재동기화로 즉시 본다.
**서버 세션은 열어둔 채로:**
```bash
sudo systemctl restart google-guest-agent && sleep 5
grep -c '^restrict,command=' ~/.ssh/authorized_keys    # 0 이면 덮어씀이 확정
grep -c '^restrict,command=' ~/.ssh/authorized_keys2   # 항상 2 여야 정상
```

---

## 8. `docker login ghcr.io` 가 fine-grained PAT 으로 안 된다

### 증상
런북과 운영 문서가 "fine-grained PAT(packages:read)" 를 지시하고 있었다. 그대로 따르면
GHCR 로그인 단계에서 막힌다.

### 원인
GitHub 공식 문서:
> "GitHub Packages only supports authentication using a **personal access token (classic)**."

fine-grained 토큰은 Container registry 에 동작하지 않는다. 문서가 처음부터 틀려 있었다.

### 해결방법
Settings → Developer settings → Personal access tokens → **Tokens (classic)** →
Generate new token (classic). 스코프는 **`read:packages` 하나만**, 만료일 지정(무기한 금지).

```bash
# 서버에서 — 토큰을 셸 히스토리에 남기지 않는 형태
read -rsp 'PAT: ' PAT && echo "$PAT" | docker login ghcr.io -u <owner> --password-stdin && unset PAT
chmod 600 ~/.docker/config.json
docker pull ghcr.io/<owner>/calendar:latest
```

`~/.docker/config.json` 은 토큰을 **base64 인코딩만** 해서 담는다(암호화 아님). `chmod 600` 이
유일한 보호막이다. **만료일을 달력에 등록할 것** — 만료 증상은 "어느 날 갑자기 모든 배포가
exit 5" 다.

### 순서 주의
이 단계는 **첫 이미지가 발행된 뒤에만** 성공한다. 받을 이미지가 없으면 pull 이 실패한다.

---

## 9. 외부 IP 가 바뀌면 시크릿 2개가 동시에 죽는다

### 증상
인스턴스를 stop/start 한 뒤 배포가 `Permission denied` 또는 `Host key verification failed` 로
실패. 동시에 사이트도 내려가 있다.

### 원인
외부 주소가 예약되지 않은 **ephemeral** 이라 stop/start 시 바뀐다(reboot 로는 안 바뀐다).
`DEPLOY_SSH_HOST` 와 `DEPLOY_KNOWN_HOSTS` 가 둘 다 IP 를 박아두고 있어 동시에 무효가 되고,
DNS A 레코드도 함께 깨진다.

### 해결방법 — 복구
**핵심 사실: 호스트 키는 안 바뀐다.** `/etc/ssh/ssh_host_*` 는 부트 디스크에 있어 stop/start 를
넘어 유지된다. 그래서 known_hosts 는 **IP 토큰만 갈아끼우고 base64 는 재사용**한다.

1. 새 IP 확인
   ```bash
   gcloud compute instances describe <인스턴스> --zone <존> \
     --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
   ```
2. **DNS A 레코드 먼저** — 사이트 복구가 배포 복구보다 급하다.
3. 서버에서 호스트 키가 그대로인지 대조: `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`
4. 시크릿 2개 갱신 — `DEPLOY_SSH_HOST` = 새 IP, `DEPLOY_KNOWN_HOSTS` = `<새 IP> ssh-ed25519 <기존 base64>`
5. 배포 전에 SSH 왕복만 먼저 검증

### 해결방법 — 재발 방지
- **고정 IP 승격**(권장): `gcloud compute addresses create <이름> --addresses <현재 IP> --region <리전>`
  — 사용 중인 주소를 그대로 예약하므로 IP 값이 유지된다.
- **도메인을 호스트로**: 두 시크릿의 호스트 토큰을 도메인으로 두면 IP 가 바뀌어도 DNS 만
  갱신하면 된다. 조건은 `dig +short <도메인>` 이 서버 IP 를 직접 돌려주는 것(프록시 뒤면 22번이
  안 통해 불가).

---

## 10. `ssh-keyscan` 으로 만든 known_hosts 는 방어선이 아니다

### 증상 (설계 결함)
`DEPLOY_KNOWN_HOSTS` 는 러너가 서버를 식별하는 유일한 근거 — 곧 MITM 방어선이다. 그런데
`ssh-keyscan` 은 TOFU(처음 본 것을 믿음)라, 그 방어선을 신뢰 없는 채널로 만들어 버린다.

### 해결방법
gcloud 의 인증된 경로로 서버에 들어가 **안에서 직접** 뜬다.
```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub          # 지문
echo "<호스트> $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"   # 시크릿 값
```
지문이 로컬 ssh 가 최초 접속에서 물어본 값과 같으면, 그때의 TOFU 수락도 진짜였음이 함께
확인된다.

### 형식 제약
- 호스트 토큰이 `DEPLOY_SSH_HOST` 와 **글자 그대로** 같아야 한다 (IP↔호스트명 혼용 금지).
- 포트가 22 가 아니면 `[호스트]:포트 ssh-ed25519 ...` 형식.
- `deploy.yml` 이 이 값을 러너의 `~/.ssh/known_hosts` **파일 내용 그대로** 쓴다. 여러 줄 가능.

---

## 11. 커밋 로그의 백틱·`$` 가 Git Bash/PowerShell 에서 깨진다

### 증상
`docs/guides/git-commit.md` 는 복사해서 실행하는 명령 로그다. 커밋 메시지에 백틱과 `$` 를 넣으면
- **Git Bash**: 큰따옴표 안의 `` `...` `` 가 **명령 치환**으로 실행돼 텍스트가 사라진다.
- **PowerShell**: 백틱이 **이스케이프 문자**라 조용히 먹힌다.
- 양쪽: `$force` 가 **변수 확장**되어 빈 문자열이 된다.

### 진단
```bash
python3 -c "
import io
lines=[l for l in io.open('docs/guides/git-commit.md',encoding='utf-8') if l.startswith('git commit -m')]
for ch in ['\`','\$','!']:
    print(ch, sum(l.count(ch) for l in lines))"
```
기존 63개 커밋 줄의 백틱이 **0** 이었다 — 관례가 이미 그랬고 새로 쓴 쪽이 어긴 것이었다.

### 해결방법
커밋 메시지 본문에서 백틱·`$`·`!` 를 쓰지 않는다. 강조는 따옴표 없이 평문으로.
파일에 쓰기 전에 가드를 건다.
```python
for ch in ['"', '`', '$', '!']:
    assert ch not in body, f"body has {ch!r}"
```

### 확인
`git commit -m "..." -m "..."` 한 줄의 큰따옴표는 정확히 **4개**여야 한다.

---

## 12. 문서의 `file:line` 이 편집 한 번에 낡는다

### 증상
`ci.yml:27` 로 적어둔 참조가, 같은 파일에 주석 3줄을 더하자 실제로는 30행을 가리키게 됐다.

### 원인
줄 번호는 그 파일을 건드리는 순간 드리프트한다. 이 저장소는 과거에도 문서가 존재하지 않는
심볼을 지목한 전례가 있다.

### 해결방법
- **방금 편집한 파일**은 줄 번호 대신 **스텝/심볼 이름**으로 가리킨다
  (`ci.yml 의 Lint deploy scripts 단계`).
- 안정적인 구조 참조만 줄 번호를 유지한다.
- 커밋 전에 전수 대조한다.

```bash
python3 - <<'PY'
import io,re,os
for doc in ["docs/guides/deploy-setup.md","docs/operations/deployment.md"]:
    for m in re.finditer(r'`?([\w./-]+\.(?:yml|sh|yaml|java))[:](\d+)`?', io.open(doc,encoding='utf-8').read()):
        f,ln=m.group(1),int(m.group(2))
        path=next((c for c in [f,".github/workflows/"+os.path.basename(f),"deploy/"+os.path.basename(f)]
                   if os.path.exists(c)), None)
        if path:
            lines=io.open(path,encoding='utf-8').read().splitlines()
            print(f"{doc} -> {path}:{ln} = {lines[ln-1].strip()[:60] if ln<=len(lines) else '<범위 초과>'}")
PY
```

---

## 13. `rrsync` 가 PATH 에 없으면 rsync 키만 조용히 실패한다

### 증상
실행 키는 되는데 rsync 키만 동작하지 않는다. 뚜렷한 에러가 없다.

### 원인
`authorized_keys2` 의 `command="rrsync -wo ..."` 는 **forced command** 라 최소 PATH 로 실행된다.
배포판에 따라 `rrsync` 가 `/usr/bin` 이 아니라 `/usr/share/rsync/scripts/rrsync` 에 있고,
그러면 PATH 에서 찾지 못한다.

### 해결방법
```bash
command -v rrsync || ls /usr/share/rsync/scripts/
```
PATH 에 없으면 **절대경로**로 적는다.
```
restrict,command="/usr/share/rsync/scripts/rrsync -wo /home/<user>/apps/calendar" ssh-ed25519 AAAA...
```

### 확인
```bash
echo probe > /tmp/probe.txt
rsync -az -e "ssh -i ~/.ssh/calendar_rsync" /tmp/probe.txt <서버>:deploy/probe.txt   # 성공해야
rsync -az -e "ssh -i ~/.ssh/calendar_rsync" <서버>:deploy/probe.txt /tmp/            # 거부돼야(-wo)
ssh   -i ~/.ssh/calendar_rsync <서버> "ls"                                            # 거부돼야
```
경로는 rrsync 루트 기준 상대경로다 — `deploy/probe.txt` → `~/apps/calendar/deploy/probe.txt`.

---

## 14. `sudo id -g` 는 0 이다

### 증상
`.deploy/env` 의 `APP_GID` 에 0 이 들어가 컨테이너가 root 그룹으로 돈다.

### 원인
compose 가 `user: "10001:${APP_GID}"` 로 기동해, 컨테이너가 `data/`·`logs/` 에 쓰는 파일이
호스트 배포 유저의 그룹 소유가 되게 한다(→ sudo 없이 백업·정리 가능). 그런데 값을 뜰 때
`sudo id -g` 를 쓰면 root 의 gid 인 0 이 나온다.

### 해결방법
```bash
id -u; id -g        # 배포 유저로 로그인한 상태에서. sudo 없이
```
런북의 `sudo chown -R 10001:$(id -g) ...` 는 `$(id -g)` 가 **sudo 실행 전에** 사용자 셸에서
확장되므로 옳다. 다만 `sudo bash -c 'chown -R 10001:$(id -g) ...'` 처럼 통째로 감싸면 0 이
들어가므로 금지.

같은 이유로 `HOST_TZ` 도 출력 파싱 대신 정확한 명령을 쓴다.
```bash
timedatectl show -p Timezone --value
```

---

# 3부. 미해결 · 범위 밖 (보고만)

이번 작업 범위가 아니어서 고치지 않았다. 별도 판단이 필요하다.

| 항목 | 내용 |
|---|---|
| `Dockerfile` 의 `ARG JAR_FILE=build/libs/*.jar` | `build.gradle` 에 `jar { enabled = false }` 가 없어 `./gradlew build` 가 `-plain.jar` 까지 2개를 만든다. 로컬 `docker build .` 는 COPY 가 2소스→비디렉터리로 **실패**한다. CI 는 artifact 1개만 내려받아 **우연히** 회피 중 |
| `ci.yml` 의 jar 경로 하드코딩 | `calendar-0.0.1-SNAPSHOT.jar` — `build.gradle` 의 version 을 올리면 upload-artifact 부터 깨진다 |
| `deploy/compose.yaml` 사본 | `deploy.yml` 이 `deploy/` 전체를 rsync 해 서버에 미사용 `~/apps/calendar/deploy/compose.yaml` 이 생긴다. compose 파일 머리말의 SSOT 경고와 상충 |
| `.deploy/env` 템플릿 부재 | compose 가 `APP_REF`·`HOST_TZ`·`JAVA_OPTS`·`APP_GID` 를 `:?` fail-fast 로 요구하는데 저장소에 커밋된 예시가 없다. 현재는 런북 3-5 의 프로즈가 유일한 스펙 |
| nginx healthcheck 부재 | compose 의 `nginx` 서비스에 healthcheck 가 없다. `deploy.sh` 의 `edge_gate` 가 유일한 생존 신호 |
| `.env.example` 누락 키 | `H2_CONSOLE_ENABLED`·`GOOGLE_SITE_VERIFICATION`·`ADSENSE_SLOT_*` 미기재 (전부 선택값, boot 무영향) |
| `run:` 블록의 셸 | `deploy.yml` 의 rsync·ssh 조립은 shellcheck 대상이 아니다 — 리뷰로만 지킨다 |

---

# 부록. 이 세션에서 쓴 검증 명령 모음

```bash
# 셸 스크립트 — CI 의 Lint deploy scripts 단계와 동일
bash -n deploy/deploy.sh && bash -n deploy/server/calendar-deploy-gate.sh
npx -y shellcheck@4.1.0 -S warning deploy/deploy.sh deploy/server/calendar-deploy-gate.sh

# 워크플로 YAML 문법 + 트리거/조건 실제 값
python3 -c "
import yaml; d=yaml.safe_load(open('.github/workflows/ci.yml'))
on=d[True] if True in d else d['on']
print('push:', on['push']['branches'])
print('push-image if:', d['jobs']['push-image']['if'])"

# 문서 상대링크 전수 (깨진 링크 0 이어야 한다)
python3 -c "
import re,os,io,glob
bad=n=0
for d in glob.glob('docs/**/*.md',recursive=True)+['CLAUDE.md']:
    base=os.path.dirname(d)
    for m in re.finditer(r'\]\((?!https?:)([^)#]+)(#[^)]*)?\)', io.open(d,encoding='utf-8').read()):
        t=os.path.normpath(os.path.join(base,m.group(1))); n+=1
        if not os.path.exists(t): print('BROKEN',d,'->',m.group(1)); bad+=1
print(n,'개 링크, 깨짐',bad)"

# 서버 쪽
sudo sshd -T | grep -iE '^port|authorizedkeys'
grep -c '^restrict,command=' ~/.ssh/authorized_keys2
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
command -v rrsync || ls /usr/share/rsync/scripts/
id -u; id -g
timedatectl show -p Timezone --value
```
