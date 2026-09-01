#!/usr/bin/env bash
# calendar 재시작 배포 스크립트 (ADR common/infrastructure/0001)
# 사용: deploy.sh <image-tag(40자 git sha|latest)> [--force]
# 종료코드: 0 성공 / 2 가드·preflight 거부 / 3 헬스 실패+롤백 성공 / 4 롤백도 실패 / 5 pull·레지스트리 실패
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/apps/calendar}"
DEPLOY_STATE="$APP_DIR/.deploy"
COMPOSE=(docker compose --project-directory "$APP_DIR" -f "$APP_DIR/compose.yaml" --env-file "$DEPLOY_STATE/env")
IMAGE="ghcr.io/singingsandhill/calendar"
HEALTH_URL="http://127.0.0.1:8081/actuator/health/deploy"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-300}"   # 런북 부팅 3회 실측 p95×2 로 확정
DOMAIN="datedate.site"

log() { echo "[deploy $(date -u +%H:%M:%SZ)] $*"; }

# ---------- 인자 ----------
TAG="${1:-}"
FORCE=0
for a in "$@"; do [[ "$a" == "--force" ]] && FORCE=1; done
if [[ ! "$TAG" =~ ^([0-9a-f]{40}|latest)$ ]]; then
  log "REFUSED: invalid tag '$TAG' (40-hex git sha or 'latest')"; exit 2
fi

# ---------- preflight: 상태 경로·볼륨 해석 검증 (빈 DB 로 새 기동하는 조용한 유실 차단) ----------
[[ -f "$APP_DIR/.env" ]] || { log "REFUSED: $APP_DIR/.env missing"; exit 2; }
[[ -f "$APP_DIR/data/scheduledb.mv.db" ]] || { log "REFUSED: H2 file missing at $APP_DIR/data (path regression?)"; exit 2; }
[[ -f "$DEPLOY_STATE/env" ]] || { log "REFUSED: $DEPLOY_STATE/env missing (HOST_TZ/JAVA_OPTS/APP_GID — see runbook)"; exit 2; }
if ! APP_REF="$IMAGE:$TAG" "${COMPOSE[@]}" config 2>/dev/null | grep -Eq "source: $APP_DIR/data$"; then
  log "REFUSED: compose does not resolve ./data to $APP_DIR/data — volume path regression"; exit 2
fi

# ---------- 가드 1: 주식 봇 LIVE 시간창 (평일 09:15~11:25 KST) ----------
# [[ -ge ]] 는 산술 평가라 선행 0 이 8진수로 파싱된다 — 10# 강제 필수.
kst_dow=$(TZ=Asia/Seoul date +%u)
kst_hm=$((10#$(TZ=Asia/Seoul date +%H%M)))
if (( kst_dow <= 5 && kst_hm >= 915 && kst_hm <= 1125 )) && (( FORCE == 0 )); then
  log "REFUSED: stock bot trading window (Mon-Fri 09:15-11:25 KST). Use --force."; exit 2
fi

# ---------- pull (구 컨테이너 가동 중 선-pull → 다운타임 최소화) + 다이제스트 고정 ----------
log "pulling $IMAGE:$TAG"
docker pull "$IMAGE:$TAG" || { log "FAILED: pull (레지스트리 장애 또는 GHCR PAT 만료 확인)"; exit 5; }
DIGEST=$(docker image inspect --format '{{index .RepoDigests 0}}' "$IMAGE:$TAG")
[[ -n "$DIGEST" ]] || { log "FAILED: cannot resolve digest for $IMAGE:$TAG"; exit 5; }
log "resolved digest: $DIGEST"

PREV_REF=$(cat "$DEPLOY_STATE/current" 2>/dev/null || echo "")

set_app_ref() {  # .deploy/env 의 APP_REF 만 교체 — HOST_TZ·JAVA_OPTS·APP_GID 보존 병합
  { grep -v '^APP_REF=' "$DEPLOY_STATE/env" 2>/dev/null || true; echo "APP_REF=$1"; } > "$DEPLOY_STATE/env.new"
  grep -q '^HOST_TZ=' "$DEPLOY_STATE/env.new" || { log "REFUSED: HOST_TZ lost from env"; exit 2; }
  grep -q '^JAVA_OPTS=' "$DEPLOY_STATE/env.new" || { log "REFUSED: JAVA_OPTS lost from env"; exit 2; }
  mv "$DEPLOY_STATE/env.new" "$DEPLOY_STATE/env"
}

health_gate() {  # 상태 기반: 컨테이너 사망은 즉시 실패, 아니면 UP 폴링. 0=UP 1=실패
  local deadline=$(( $(date +%s) + HEALTH_TIMEOUT ))
  local cid restarts0=-1
  sleep 3
  cid=$("${COMPOSE[@]}" ps -q app || true)
  [[ -n "$cid" ]] && restarts0=$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo -1)
  while (( $(date +%s) < deadline )); do
    if [[ -n "$cid" ]]; then
      local state restarts
      state=$(docker inspect -f '{{.State.Status}}' "$cid" 2>/dev/null || echo unknown)
      restarts=$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo -1)
      if [[ "$state" == "exited" || "$state" == "dead" ]] || (( restarts > restarts0 )); then
        log "health gate: container crashed (state=$state restarts=$restarts)"; return 1
      fi
    fi
    if curl -fsS --max-time 3 "$HEALTH_URL" 2>/dev/null | grep -q '"UP"'; then
      return 0
    fi
    sleep 5
  done
  log "health gate: timeout ${HEALTH_TIMEOUT}s"; return 1
}

edge_gate() {  # nginx 경유 검증 — 앱만 살아있고 사이트가 죽은 상태(DNS 캐싱류)를 성공으로 오보하지 않게
  local code
  code=$(curl -ksS -o /dev/null -w '%{http_code}' --max-time 10 \
         --resolve "$DOMAIN:443:127.0.0.1" "https://$DOMAIN/" || echo 000)
  if [[ "$code" != "200" ]]; then log "edge gate: GET / via nginx returned $code"; return 1; fi
  # X-Forwarded-Proto 계약 (카카오 KOE006): 보호 경로 리다이렉트가 https 로 나가는지
  local loc
  loc=$(curl -ksS -o /dev/null -w '%{redirect_url}' --max-time 10 \
        --resolve "$DOMAIN:443:127.0.0.1" "https://$DOMAIN/trading" || echo "")
  if [[ "$loc" != https://* ]]; then log "edge gate: /trading redirect is '$loc' (X-Forwarded-Proto broken?)"; return 1; fi
  return 0
}

start_app() {  # $1 = image ref
  set_app_ref "$1"
  "${COMPOSE[@]}" up -d app
}

# ---------- 가드 2: 크립토 매분 :05 틱 회피 — 반드시 pull 후·정지 직전 ----------
while s=$(date +%S); (( 10#$s < 20 || 10#$s > 50 )); do sleep 1; done

log "stopping app (SIGTERM → graceful 30s + scheduler await 25s, grace 60s)"
"${COMPOSE[@]}" stop app || true

# ---------- 정지 상태 백업 (ddl-auto 롤백 안전망, H2 ~수백 KB) — 5세대 보관 ----------
mkdir -p "$DEPLOY_STATE/backup"
cp -a "$APP_DIR/data" "$DEPLOY_STATE/backup/data-$(date +%F-%H%M%S)"
ls -1dt "$DEPLOY_STATE/backup"/data-* 2>/dev/null | tail -n +6 | xargs -r rm -rf

# ---------- 기동 + 게이트 ----------
log "starting app with $DIGEST"
start_app "$DIGEST"

if health_gate && edge_gate; then
  :
else
  log "deployment FAILED — dumping last logs"
  "${COMPOSE[@]}" logs --tail 100 app || true
  if [[ -n "$PREV_REF" ]]; then
    log "rolling back to $PREV_REF"
    "${COMPOSE[@]}" stop app || true
    start_app "$PREV_REF"
    if health_gate && edge_gate; then
      log "ROLLED BACK to $PREV_REF"; exit 3
    fi
  fi
  log "rollback unavailable/failed — stopping app to break crash loop (maintenance page stays up)"
  "${COMPOSE[@]}" stop app || true
  exit 4
fi

# ---------- nginx 재해석 belt-and-braces + 상태 기록 ----------
"${COMPOSE[@]}" exec -T nginx nginx -s reload || log "warn: nginx reload failed (resolver 재해석이 커버)"

[[ -n "$PREV_REF" ]] && echo "$PREV_REF" > "$DEPLOY_STATE/previous"
echo "$DIGEST" > "$DEPLOY_STATE/current"

# ---------- 이미지 GC — current/previous 보존 목록 기반 (prune 은 태그 이미지 미회수 / -a 는 롤백본 삭제) ----------
keep_ids=""
for ref in "$DIGEST" "$PREV_REF"; do
  [[ -n "$ref" ]] && keep_ids+=" $(docker image inspect --format '{{.Id}}' "$ref" 2>/dev/null || true)"
done
for img_id in $(docker images "$IMAGE" --format '{{.ID}}' | sort -u); do
  full_id=$(docker image inspect --format '{{.Id}}' "$img_id" 2>/dev/null || echo "")
  if [[ -n "$full_id" && "$keep_ids" != *"$full_id"* ]]; then
    docker rmi -f "$img_id" >/dev/null 2>&1 || true
  fi
done
docker image prune -f >/dev/null 2>&1 || true   # dangling 만 — 디스크 회수 근거는 위 GC

log "DEPLOYED $DIGEST (previous: ${PREV_REF:-none})"
df -h / | tail -1
exit 0
