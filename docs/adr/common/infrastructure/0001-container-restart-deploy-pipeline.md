# ADR-0001: GHCR 이미지 + compose 재시작 배포 파이프라인 (CI 자동 빌드 / 수동 반영 / 다이제스트 롤백)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-17 |
| 도메인 | common |
| 관심사 | 인프라 / 배포 |
| 관련 ADR | [common/infrastructure/0002](0002-nginx-in-compose-and-certbot-webroot.md), [common/security/0006](../security/0006-actuator-health-and-h2-console-lockdown.md), [trading/modes/0003](../../trading/modes/0003-protection-only-recovery-on-restart.md) |
| 관련 이슈 | CI/CD 설계 (2026-08-17) |

## Context — 무엇이 문제였나

- 배포 = "서버 git pull → 서버에서 gradle 빌드 → .env 수동 편집 → 프로세스 수동 재시작".
  e2-micro(RAM 958MiB, swap 986Mi 사용 중)에서 Gradle 빌드가 돌고, stdout 리다이렉트
  `app.log` 가 272MB 까지 무제한 증식했으며, 절차가 문서화돼 있지 않았다.
- `.github/workflows/google-cloudrun-source.yml` 은 플레이스홀더 미설정 + 트리거 브랜치
  오타(`'"deploy"'`)로 한 번도 동작한 적 없는 죽은 파일이었다.
- 이 서버에서는 실계좌 LIVE 코인 봇이 돈다 — 배포 안전성(이중 실행·틱 중단·롤백)이
  일반 웹앱보다 훨씬 무겁다.

## Decision — 무엇을 골랐나

**CI 는 자동, 서버 반영은 수동, 배포는 재시작(1~3분 계획 중단), 롤백은 다이제스트 기준.**

- **빌드**: GitHub Actions `ci.yml` 이 main 푸시·PR 마다 `./gradlew build`(전체 테스트) 실행,
  main 푸시 시 jar 를 Boot 4 `jarmode=tools` 레이어 추출 Dockerfile 로 이미지화해
  `ghcr.io/singingsandhill/calendar:{git-sha, latest}` 로 푸시. 서버 빌드 완전 제거.
- **반영**: `deploy.yml` 은 `workflow_dispatch` 수동 버튼 전용. 태그 정규식 검증 → GHCR
  manifest 선검증 → 배포 번들 rsync → SSH forced-command 게이트 경유 `deploy.sh` 실행.
- **deploy.sh 순서**: preflight(상태 경로·compose 볼륨 해석 기계 검증) → 주식 LIVE 시간창
  가드(평일 09:15~11:25 KST 거부, `--force` 우회) → 선-pull → **다이제스트 고정** →
  크립토 :05 틱 회피 대기(정지 직전) → stop → 정지 상태 H2 백업(5세대) → up →
  헬스 게이트(컨테이너 사망 즉시 실패 + `/actuator/health/deploy` 폴링) → 엣지 게이트
  (nginx 경유 200 + X-Forwarded-Proto 계약) → 실패 시 직전 다이제스트로 자동 롤백.
- **롤백은 태그가 아니라 다이제스트**: `latest` 는 pull 순간 로컬 캐시가 새 이미지로
  덮이므로 태그 기반 롤백은 "같은 불량 이미지 재기동"이 된다. `.deploy/current`/`previous`
  에 `@sha256:...` 를 기록하고 compose 도 다이제스트로 기동한다.
- **종료 계약**: 스케줄러 `await-termination` 25s < `lifecycle` 30s < compose
  `stop_grace_period` 60s. graceful shutdown 은 웹 요청만 보호하고 `@Scheduled` 틱은
  기본 `shutdownNow()` 로 인터럽트되므로 `spring.task.scheduling.shutdown.await-termination`
  이 필수다 — 주문 HTTP 후 DB 커밋 전 kill 이 무기록 체결을 만든다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| blue-green (신구 동시 기동) | 무중단이지만 | H2 파일 DB 배타 락으로 두 번째 JVM 기동 자체가 불가. @Scheduled 11개가 분산락 없이 인스턴스별 실행이라 겹치면 실주문 이중 실행. RAM 958MiB 로 JVM 2개 불가 |
| Cloud Run (기존 템플릿 활성화) | 관리형 인프라 | 상시 스케줄러(min-instances=1 상시 과금)·파일 DB 영속 불가·리비전 교체 시 신구 인스턴스 겹침(봇 이중 실행). 재설계 없이는 구조적으로 부적합 |
| 서버 git pull + 빌드 유지 | 변경 최소 | 1GB 박스 빌드 부하, 절차 비재현, 롤백 수단 없음 — 현행 방식의 문제가 그대로 |
| main 푸시 자동 배포 | 편리 | 문서 커밋에도 봇이 재시작(보호전용 강등 + 서킷브레이커 카운터 리셋). 실계좌 서버는 반영 시점을 사람이 고른다 |
| 재시작 배포 (선택) | 1~3분 중단 | 단일 인스턴스 불변식 유지, 봇 이중 실행 원천 차단. 중단은 nginx 503 유지보수 페이지로 흡수 |

## Consequences — 영향

- **긍정:** 배포가 버튼 1회 + 자동 게이트/롤백. 서버 빌드·수동 절차·무제한 stdout 로그 제거
  (json-file max-size 10m×3). 매 배포 정지 상태 H2 백업이 `ddl-auto: update` 스키마 롤백의
  안전망이 된다(스키마 변경 릴리스는 additive-only 규칙).
- **부정:** 배포마다 세션 전멸(인메모리)·신호/리밸런싱 쿨다운·서킷브레이커 연속손실
  카운터 리셋(수용된 비용 — 수동 버튼이라 빈도 통제). 코인 봇은 보호전용 자동재개 후
  관리자 Start 까지 신규 매매 중단 ([trading/modes/0003](../../trading/modes/0003-protection-only-recovery-on-restart.md)).
- **부정:** 배포 SSH 키는 docker 그룹(=root 동등) 유저로 이어진다 — forced-command 게이트
  (`deploy/server/calendar-deploy-gate.sh`, root 소유)와 rrsync 쓰기 전용 키로 표면을 좁혔지만,
  **두 키가 모두 탈취되면 rsync 로 deploy.sh 를 교체해 실행시킬 수 있다**. GitHub Secrets
  보호와 known_hosts 고정이 실질 경계다.
- **부정:** GHCR PAT(서버 pull 용) 만료 시 배포가 pull 단계(exit 5)에서 실패한다 — 만료
  관리가 `docs/operations/deployment.md` 에 있다.
- **후속:** cid 미부착 구성(V1+OFF)에서는 주문 선영속화가 꺼져 있어 "주문 HTTP 후 커밋 전
  kill" 잔여 갭이 남는다 — 근본 해소는 `docs/trading/remaining-work.md` A-3(선영속화 활성화).

## References

- `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`
- `Dockerfile`, `.dockerignore`, `deploy/compose.yaml`, `deploy/deploy.sh`, `deploy/server/calendar-deploy-gate.sh`
- `src/main/resources/application.yaml` (`spring.task.scheduling.shutdown.*`, `server.shutdown`)
- 운영 절차: `docs/operations/deployment.md`, 1회성 이관: `docs/operations/server-migration-runbook.md`
