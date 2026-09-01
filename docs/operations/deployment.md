# 배포 파이프라인 운영 가이드

> 결정 근거: [ADR common/infrastructure/0001](../adr/common/infrastructure/0001-container-restart-deploy-pipeline.md)
> (재시작 배포·다이제스트 롤백), [0002](../adr/common/infrastructure/0002-nginx-in-compose-and-certbot-webroot.md)
> (nginx compose·certbot webroot), [common/security/0006](../adr/common/security/0006-actuator-health-and-h2-console-lockdown.md)
> (헬스 게이트·엣지 차단), [trading/modes/0003](../adr/trading/modes/0003-protection-only-recovery-on-restart.md)
> (배포 후 코인 봇 보호전용 자동재개).
> 1회성 서버 이관 절차는 [server-migration-runbook.md](server-migration-runbook.md).

## 파이프라인 개요

```
main 푸시/PR ──► ci.yml: gradle build+test (+shellcheck) ──main 푸시──► GHCR push (:<git-sha>, :latest)
Actions 수동 버튼 ──► deploy.yml: 태그검증 → manifest 선검증 → 번들 rsync → ssh 게이트 → deploy.sh
```

- **CI 는 자동, 서버 반영은 `workflow_dispatch` 수동 버튼 전용.** 실계좌 LIVE 봇 서버라
  배포마다 봇 재시작(보호전용 자동재개)·서킷브레이커 카운터 리셋이 발생한다 — 반영
  시점은 사람이 고른다.
- 이미지: `ghcr.io/singingsandhill/calendar` — 태그는 full git SHA + `latest`.
- 배포 입력 `image_tag`: 40자 git SHA 권장(미입력 시 dispatch 한 커밋의 SHA).
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
| `DEPLOY_SSH_HOST` | 서버 공인 IP/호스트명 |
| `DEPLOY_SSH_USER` | `ourbalance_topping` |
| `DEPLOY_SSH_PORT` | 생략 시 22 |
| `DEPLOY_SSH_KEY` | 실행용 ed25519 개인키 — 서버 authorized_keys 에 `restrict,command="/usr/local/bin/calendar-deploy-gate"` |
| `DEPLOY_SSH_RSYNC_KEY` | 번들 동기화용 개인키 — `restrict,command="rrsync -wo /home/ourbalance_topping/apps/calendar"` |
| `DEPLOY_KNOWN_HOSTS` | `ssh-keyscan <host>` 결과 (MITM 방지) |

GHCR 푸시는 `GITHUB_TOKEN` 으로 충분. **서버 pull 용 fine-grained PAT(packages:read)** 는
GitHub Secret 이 아니라 서버 `~/.docker/config.json` 에 1회 저장
(`docker login ghcr.io --password-stdin`, chmod 600). **만료일을 달력에 등록할 것** —
만료 증상은 "어느 날 갑자기 모든 배포가 exit 5".

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
