#!/bin/bash
# 배포 SSH 키 forced-command 게이트 (ADR common/security/0006)
#
# 설치(런북 3단계, 1회): sudo cp → /usr/local/bin/calendar-deploy-gate + chmod 755 + chown root:root
# authorized_keys(실행용 키): restrict,command="/usr/local/bin/calendar-deploy-gate" ssh-ed25519 AAAA...
# authorized_keys(rsync용 키): restrict,command="rrsync -wo /home/ourbalance_topping/apps/calendar" ssh-ed25519 AAAA...
#
# 목적: docker 그룹(=root 동등) 유저의 배포 키가 탈취돼도 임의 명령이 아니라
# "정해진 배포 스크립트 + 검증된 태그" 만 실행되게 한다. root 소유·배포유저 쓰기 불가로 유지할 것.
set -euo pipefail

APP_DIR="/home/ourbalance_topping/apps/calendar"
cmd="${SSH_ORIGINAL_COMMAND:-}"

if [[ "$cmd" =~ ^deploy\ ([0-9a-f]{40}|latest)(\ --force)?$ ]]; then
  tag="${BASH_REMATCH[1]}"
  force="${BASH_REMATCH[2]:-}"
  # shellcheck disable=SC2086 -- force 는 정규식으로 ' --force' 만 허용됨
  exec bash "$APP_DIR/deploy/deploy.sh" "$tag" $force
fi

echo "REJECTED: '$cmd' (allowed: deploy <40-hex-sha|latest> [--force])" >&2
exit 1
