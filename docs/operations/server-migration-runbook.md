# 서버 마이그레이션 런북 (1회성) — 직접 실행 → compose 스택 전환

> 대상: GCP e2-micro `instance-20250702-091605` (RAM 958MiB, swap 2GB, Ubuntu 25.04,
> Docker 28.3.3 + compose v2.39). 현행: 호스트 nginx → localhost:8081, 앱은 `~/calendar`
> 에서 직접 실행(app.log 272MB), topping postgres/pgadmin 컨테이너(미사용 프로젝트) 상주.
> 결정 근거: [ADR common/infrastructure/0001·0002](../adr/common/infrastructure/).
> 상시 운영 절차: [deployment.md](deployment.md).

🔥 = 파괴적/외부 영향 — **각 단계 실행 전 사용자 확인**. 각 단계에 검증 명령과 롤백 지점 명시.
실행 시간대: 평일 09:15~11:25 KST(주식 LIVE 창) 회피.

## 1. 실측 (읽기 전용 — 이후 단계의 입력값 확정)

| 항목 | 명령 | 산출 |
|---|---|---|
| 기동 방식·JVM 플래그 | `ps auxww \| grep java` | 4단계 롤백 커맨드, `JAVA_OPTS` 기준 |
| 메모리 실사용 | `ps -o rss= -p <pid>` | `JAVA_OPTS`(-Xmx 상당)·compose `mem_limit` 확정 |
| 부팅 소요 (3회) | `app.log` 의 `Started CalendarApplication in ...` | compose `start_period`·`HEALTH_TIMEOUT` = p95×2 |
| 호스트 TZ | `timedatectl show -p Timezone --value` (출력 파싱보다 정확) | `.deploy/env` 의 `HOST_TZ` (무존 trading cron 시각 보존 — 추정 UTC, 단정 금지) |
| uid/gid | `id -u; id -g` — **반드시 `ourbalance_topping` 로 로그인한 채**. `sudo id -g` 는 0(root) 이 나오고 그 값을 넣으면 컨테이너가 root 그룹으로 돈다 | `.deploy/env` 의 `APP_GID` |
| server `.env` 실값 | `sudo cat ~/calendar/.env` | **TRADING_BOT_MODE / STOCK_BOT_MODE / RUNNER_ADMIN_PASSWORD 존재·강도** 확인. 기본값 admin123 이면 여기서 교체 |
| certbot 상태 | `sudo certbot certificates`; `sudo cat /etc/letsencrypt/renewal/datedate.site.conf`; `systemctl list-timers \| grep certbot` | authenticator/installer 확인 (8단계 입력) |
| 크론·잔재 | `crontab -l`; `sudo crontab -l` | 10단계 정리 목록 |
| nginx 원문 | `sudo nginx -T > ~/backup/nginx-T-before.txt` | 7단계 전환 후 diff 대조 기준 |
| rrsync 존재·**경로** | `command -v rrsync \|\| ls /usr/share/rsync/scripts/` | 3단계 rsync 키 게이트. **PATH 에 없으면 authorized_keys2 에 절대경로로 써야 한다** — forced command 는 최소 PATH 로 실행돼 `/usr/share/rsync/scripts/rrsync` 를 못 찾는다 |

## 2. 백업

- 구 프로세스 **정지 상태에서** `cp -a ~/calendar/data ~/backup/data-$(date +%F)` (H2 락 주의
  — 정지 전이면 4단계 후로 미룬다), `.env` 사본, `~/backup` 권한 700.
- topping 최후 백업: `sudo docker exec topping_postgres pg_dumpall -U <user> | gzip > ~/backup/topping-final.sql.gz`
  (10단계 삭제 전 필수).
- 검증: 백업 파일 크기 > 0, `~/backup` 목록 기록.

## 3. 준비 (무중단 — 기존 서비스 영향 없음)

1. `sudo usermod -aG docker ourbalance_topping` → 재로그인 → `docker ps` 무 sudo 동작.
2. `mkdir -p ~/apps/calendar/{deploy,data,logs,.deploy/backup}`.
3. `.env` 복사: `cp ~/calendar/.env ~/apps/calendar/.env && chmod 600 ~/apps/calendar/.env`
   (현행 775 는 과다). 필요 시 `H2_CONSOLE_ENABLED` 는 **넣지 않는다**(운영 기본 비활성).
4. `sudo chown -R 10001:$(id -g) ~/apps/calendar/data ~/apps/calendar/logs` (컨테이너 uid 10001).
   `$(id -g)` 는 sudo 실행 **전에** 사용자 셸이 확장하므로 이 형태는 옳다. 통째로
   `sudo bash -c '... $(id -g) ...'` 로 감싸면 0 이 들어가니 금지.
5. `.deploy/env` 작성 (1단계 실측값 — 저장소에 커밋된 템플릿이 없다. `compose.yaml` 이
   `APP_REF`·`HOST_TZ`·`JAVA_OPTS`·`APP_GID` 를 `:?` fail-fast 로 요구한다):
   ```
   HOST_TZ=<timedatectl 값>
   JAVA_OPTS=-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError
   APP_GID=<id -g 값>
   ```
6. GHCR 로그인. ★**classic PAT 이어야 한다** — 공식 문서: "GitHub Packages only supports
   authentication using a personal access token (classic)." fine-grained 토큰은 ghcr.io 에
   동작하지 않는다.
   - 발급: GitHub → Settings → Developer settings → Personal access tokens →
     **Tokens (classic)** → Generate new token (classic).
     Note `calendar-server-ghcr-pull`, Expiration 지정(무기한 금지),
     스코프는 **`read:packages` 하나만** 체크 — 이 토큰은 서버에 평문에 가깝게 저장되므로
     표면을 최소로 둔다(pull 이 401 이면 그때 넓힌다). 토큰 값은 생성 직후 화면에서만 보인다.
   - 서버에서 (셸 히스토리에 토큰을 남기지 않는 형태):
     ```
     read -rsp 'PAT: ' PAT && echo "$PAT" | docker login ghcr.io -u singingsandhill --password-stdin && unset PAT
     chmod 600 ~/.docker/config.json
     docker pull ghcr.io/singingsandhill/calendar:latest
     ```
     `~/.docker/config.json` 은 토큰을 **base64 로 인코딩만** 해서 담는다(암호화 아님) —
     `chmod 600` 이 유일한 보호막이다.
   - **PAT 만료일 달력 등록.** 만료 증상은 "어느 날 갑자기 모든 배포가 exit 5".
   - 이 단계는 A(CI 그린 → 첫 이미지 발행) 이후에만 성공한다 — 받을 이미지가 있어야 한다.
7. **첫 번들 반입 — 게이트 설치보다 먼저.** 8단계의 `sudo cp` 가 이 파일을 소스로 쓴다
   (순서가 뒤집히면 `No such file`). 로컬에서:
   ```
   rsync -az deploy/ 서버:~/apps/calendar/deploy/
   rsync -az deploy/compose.yaml 서버:~/apps/calendar/compose.yaml
   ```
   맨 `ssh` 가 아직 안 뚫려 있으면 `gcloud compute scp --recurse --zone <존> deploy <인스턴스>:~/apps/calendar/`
   로 대체. 이후 반입은 deploy.yml 이 수행한다.
8. SSH 키 2개 발급 + 게이트 설치 + authorized_keys.
   - **키는 로컬에서 만든다.** 개인키를 쥐는 쪽은 Actions 러너이고 서버는 공개키만 필요하다 —
     서버에서 만들면 개인키가 서버에 남아 지우는 단계가 는다. 서버로 가는 것은 `.pub` 두 줄뿐.
     ```
     ssh-keygen -t ed25519 -N "" -C "calendar-deploy" -f ~/.ssh/calendar_deploy
     ssh-keygen -t ed25519 -N "" -C "calendar-rsync"  -f ~/.ssh/calendar_rsync
     ```
   - 게이트 설치 (7단계 번들이 도착한 뒤):
     ```
     sudo cp ~/apps/calendar/deploy/server/calendar-deploy-gate.sh /usr/local/bin/calendar-deploy-gate
     sudo chown root:root /usr/local/bin/calendar-deploy-gate && sudo chmod 755 /usr/local/bin/calendar-deploy-gate
     ```
   - **`~/.ssh/authorized_keys2` 에** 아래 두 줄 (항목당 정확히 한 줄, 옵션이 줄 맨 앞):
     ```
     restrict,command="/usr/local/bin/calendar-deploy-gate" ssh-ed25519 <실행키 공개키>
     restrict,command="rrsync -wo /home/ourbalance_topping/apps/calendar" ssh-ed25519 <rsync키 공개키>
     ```
     ★`authorized_keys` 가 아니라 **`authorized_keys2`** 다 — GCP 게스트 에이전트가
     `authorized_keys` 를 덮어쓸 수 있고(공식 문서: "might be overwritten by the VM's guest
     agent"), 이 서버는 `authorizedkeyscommand none` 이라 파일 방식이 살아 있다. sshd 의
     `authorizedkeysfile` 이 `.ssh/authorized_keys .ssh/authorized_keys2` 두 개를 읽고
     에이전트가 관리하는 이름은 앞쪽 하나뿐이라, 뒤쪽에 두면 sshd 는 읽고 에이전트는 안 건드린다.
     지워지면 배포 키가 통째로 사라질 뿐 게이트가 풀리지는 않는다 — 가용성 문제다.
     ★`rrsync` 가 PATH 에 없으면(1단계 실측) **절대경로로 쓴다** — forced command 는 최소
     PATH 로 실행돼 `/usr/share/rsync/scripts/rrsync` 를 못 찾고, 증상은 rsync 만 조용히
     실패하는 것이다. 예: `command="/usr/share/rsync/scripts/rrsync -wo /home/..."`.
   - 권한: `chmod 700 ~/.ssh; chmod 600 ~/.ssh/authorized_keys2; chmod go-w ~`
     (홈이 group/world 쓰기 가능이면 StrictModes 가 키를 무시한다).
   - 검증(비파괴, 기존 세션을 열어둔 채 새 터미널에서):
     ```
     ssh -i ~/.ssh/calendar_deploy 서버 "whoami"     # 기대: REJECTED: 'whoami' (allowed: ...) + exit 1
     ssh -i ~/.ssh/calendar_rsync  서버 "ls"          # 기대: rrsync 거부
     rsync -az -e "ssh -i ~/.ssh/calendar_rsync" /tmp/probe.txt 서버:deploy/probe.txt   # 성공해야
     rsync -az -e "ssh -i ~/.ssh/calendar_rsync" 서버:deploy/probe.txt /tmp/            # 거부돼야(-wo)
     ```
     실행키가 사용자명을 찍거나 셸이 열리면 forced-command 미적용 — 즉시 중단.
     `No such file` 이면 게이트 미설치(7↔8 순서).
9. GitHub Secrets 등록 ([deployment.md](deployment.md) 표). 값 출처:
   - `DEPLOY_SSH_PORT` — `sudo sshd -T | grep '^port'` 가 22 면 **등록하지 않는다**(deploy.yml 폴백).
     `sshd_config` 를 직접 보면 `sshd_config.d/*.conf` 를 놓친다.
   - `DEPLOY_KNOWN_HOSTS` — `ssh-keyscan` 은 TOFU 라 이 시크릿(=MITM 방어선)을 신뢰 없는 채널로
     만든다. 서버 안에서 직접 뜬다:
     ```
     ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub          # 지문을 로컬이 본 값과 대조
     echo "<서버IP> $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"
     ```
     호스트 토큰은 `DEPLOY_SSH_HOST` 와 **글자 그대로** 같아야 한다(IP↔호스트명 혼용 금지).
     포트가 22 가 아니면 `[호스트]:포트` 형식.

## 4. 🔥 구 프로세스 정지

- 주식 장중 회피 + 크립토 :05 틱 회피(초 20~50 구간)에서 `kill <pid>` (SIGTERM).
- 검증: `ss -ltnp | grep 8081` 비어 있음, `~/calendar/data/*.lock.db` 부재(H2 락 해제).
- 이 시점부터 사이트 다운(호스트 nginx 는 502) — 6단계까지 신속히.
- **롤백**: 1단계에서 실측한 커맨드로 재기동.

## 5. 데이터 이관

- `cp -a ~/calendar/data/. ~/apps/calendar/data/ && sudo chown -R 10001:$(id -g) ~/apps/calendar/data`
- 검증: `ls -la` 파일 크기 원본 일치. 원본 `~/calendar/data` 는 **삭제하지 않는다**(추가 백업).

## 6. compose **app 만** 기동 (호스트 nginx 유지 상태)

1. `.deploy/env` 에 `APP_REF=ghcr.io/singingsandhill/calendar@<다이제스트>` 추가
   (`docker image inspect --format '{{index .RepoDigests 0}}' ...:latest`).
2. **볼륨 해석 검증**: `docker compose --project-directory ~/apps/calendar -f ~/apps/calendar/compose.yaml --env-file ~/apps/calendar/.deploy/env config`
   에서 data/logs/.env 의 source 가 `~/apps/calendar/...` 절대경로인지 눈으로 확인
   (★compose 상대경로는 compose 파일 위치 기준 — 어긋나면 빈 DB 로 새로 기동).
3. `docker compose ... up -d app` → `curl -s localhost:8081/actuator/health/deploy` UP.
4. **기존 데이터 가시성**: 기존 owner 대시보드 URL 200 + 거래내역 존재(`/api/trading/bot/status`
   말고 실데이터 — 헬스 UP 은 빈 DB 도 통과한다).
5. 호스트 nginx 가 여전히 localhost:8081 프록시 중 → `https://datedate.site` 즉시 복구 확인.
6. 스모크: `/` `/runners` `/stock` 200, **카카오 로그인 왕복**(`/login`→카카오→`/me`),
   `admin/admin123` 로그인 **실패**, 컨테이너 내 `id` = uid 10001, `docker inspect` Memory ≠ 0.
7. 코인 봇: 오픈 포지션 있으면 `PROTECTION-ONLY` 표시 확인 → 정상 확인 후 Start.
- **롤백**: `docker compose stop app` + 4단계 롤백 커맨드.

## 7. 🔥 nginx 전환 (수 초 중단)

1. `sudo systemctl stop certbot.timer` (8단계 완료까지 — nginx authenticator 오발 방지).
2. **`sudo mkdir -p /var/www/certbot`** — compose 가 이 경로를 `:ro` 바인드 마운트한다.
   먼저 만들지 않으면 Docker 가 대신 만들며 소유권·컨텍스트가 의도와 달라진다
   (8단계에도 같은 명령이 있지만 nginx 기동이 먼저라 여기가 실제 필요 시점).
3. `sudo systemctl stop nginx && sudo systemctl disable nginx`.
4. `docker compose ... up -d nginx`.
5. 검증: `curl -I https://datedate.site` 200 / `http://`·`www` 301 / `/h2-console`·`/actuator`
   404 / 카카오 로그인 재확인(X-Forwarded 계약) / `docker compose exec nginx nginx -T` 를
   1단계 덤프와 diff — TLS 프로토콜·암호군 동일(`nmap --script ssl-enum-ciphers -p 443` 전후 비교).
6. 유지보수 페이지: `docker compose stop app` 후 `curl -i https://datedate.site` →
   **503** + `Retry-After: 120` + 점검 문구 본문 → `up -d app` 원복.
- **롤백**: `docker compose stop nginx && sudo systemctl start nginx` (수 초).

## 8. 🔥 certbot webroot 전환

1. `sudo mkdir -p /var/www/certbot`.
2. `sudo certbot reconfigure --cert-name datedate.site --authenticator webroot --webroot-path /var/www/certbot --installer null`
   (★`--installer null` 필수 — nginx installer 잔존 시 갱신이 installer 단계에서 실패).
   구버전 certbot 이면 renewal conf 의 `authenticator`/`installer` 직접 편집.
3. renewal conf 육안 확인: `authenticator = webroot`, `installer` 없음(또는 null).
4. 훅 설치 — `/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh` (root, 755, **절대경로만**
   — 훅은 root 로 실행돼 `~` 가 `/root` 로 확장된다):
   ```sh
   #!/bin/sh
   exec docker compose --project-directory /home/ourbalance_topping/apps/calendar \
     -f /home/ourbalance_topping/apps/calendar/compose.yaml exec -T nginx nginx -s reload
   ```
5. 챌린지 경로 선검증: `echo ok | sudo tee /var/www/certbot/.well-known/acme-challenge/probe`
   → `curl http://datedate.site/.well-known/acme-challenge/probe` == ok → probe 삭제.
6. `sudo certbot renew --dry-run` 성공 + **훅 직접 1회 실행**(dry-run 은 deploy hook 을
   실행하지 않는다) → reload 성공 확인.
7. `sudo systemctl start certbot.timer`.
- **롤백**: renewal conf 원복 + 호스트 nginx 재기동 (인증서는 유효기간 내라 무해).

## 9. 배포 리허설 (파이프라인 검증)

1. Actions deploy.yml 로 동일 다이제스트 재배포 → 재시작 창에 `curl -w '%{http_code}' https://datedate.site` == 503 확인 → 완료 후 200.
2. 거래창 시각(또는 시스템 시간 목킹)에 `--force` 없이 → exit 2 확인.
3. 의도적 실패: `.deploy/env` 의 `JAVA_OPTS` 에 오타 → 배포 → 자동 롤백(exit 3) + 외부 200 확인 → 원복.
4. 틱 도중 SIGTERM 리허설: `docker compose stop app` 시 로그에 인터럽트 스택트레이스 없이
   루프 완주(graceful) 확인.

## 10. 🔥 정리

- `sudo docker rm -f topping_pgadmin topping_postgres` + `docker rmi` 해당 이미지.
- 볼륨은 `docker volume ls` 로 **topping 소속 볼륨명 지정 개별 삭제** (★`docker volume prune`
  전면 금지 — 습관적 prune 차단. calendar 는 bind mount 라 무관하지만 규칙으로).
- `gzip ~/calendar/app.log && mv ~/calendar/app.log.gz ~/backup/`, `errors.log` 동일.
- `~/calendar` 의 `build/`·`.gradle` 삭제 (git checkout·`data/` 원본은 존치 — 추가 백업 역할).
- 1단계 실측된 구 기동 잔재(크론·rc 등) 제거.
- `df -h` 로 회수 확인, `free -h` 로 topping 제거 후 메모리 여유 기록.
