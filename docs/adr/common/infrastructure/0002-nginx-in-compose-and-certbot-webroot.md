# ADR-0002: nginx 를 compose 스택에 편입 + certbot webroot 전환 + 503 유지보수 페이지

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-17 |
| 도메인 | common |
| 관심사 | 인프라 / 배포 |
| 관련 ADR | [common/infrastructure/0001](0001-container-restart-deploy-pipeline.md), [common/security/0006](../security/0006-actuator-health-and-h2-console-lockdown.md) |
| 관련 이슈 | CI/CD 설계 (2026-08-17) |

## Context — 무엇이 문제였나

- 호스트 nginx 의 `sites-enabled/datedate`(datedate.site 3개 server block)가 저장소에 없어
  서버가 유일본이었다 — 설정 드리프트를 리뷰할 수 없고 서버 유실 시 재현 불가.
- 재시작 배포(ADR 0001) 중 앱이 내려간 1~3분 동안 사용자와 크롤러가 nginx 기본 502 를
  받는다 — SEO 를 명시적으로 관리하는 저장소(docs/seo/, sitemap, IndexNow)에서 색인 오염 위험.
- TLS 는 호스트 certbot(letsencrypt) 이 관리한다. nginx 를 컨테이너로 옮기면 certbot 의
  nginx 플러그인 인증·installer 가 깨진다.

## Decision — 무엇을 골랐나

**nginx 를 compose 서비스로 편입하고 설정을 저장소(`deploy/nginx/datedate.conf`)에서 버전
관리한다. certbot 은 호스트에 남기되 webroot 인증으로 전환한다.**

- 기존 3개 server block 의미·TLS 지시어(protocols/ciphers/http2)·X-Forwarded 5개 헤더
  (카카오 KOE006 계약) 를 그대로 이관. HSTS 등 신규 지시어 추가 없음(행동 보존).
- **유지보수 페이지**: `error_page 502 503 504 =503 /maintenance.html` — `=503` 명시가
  핵심이다. named location 에 코드 미지정 `=` 를 쓰면 정적 파일의 200 이 그대로 나가
  크롤러가 점검 페이지를 정상 콘텐츠로 색인한다. `Retry-After: 120` + `internal`.
- **Docker DNS 재해석**: `resolver 127.0.0.11 valid=10s` + 변수 경유 `proxy_pass` —
  변수 없는 `proxy_pass http://app:8081` 은 기동 시 1회만 해석해 배포로 app 컨테이너가
  재생성되면 죽은 IP 로 영구 502 를 낸다(헬스 게이트는 localhost 직결이라 통과 —
  "사이트는 죽고 파이프라인은 초록"). deploy.sh 의 엣지 게이트 + 배포 후 reload 가 이중 방어.
- **certbot webroot 전환**: `certbot reconfigure --authenticator webroot
  --webroot-path /var/www/certbot --installer null` — **installer 제거가 필수**다. nginx
  installer 가 남으면 호스트 nginx 비활성화 후 갱신이 installer 단계에서 실패해, 인증서가
  갱신돼도 컨테이너 nginx 는 만료 인증서를 계속 쥔다. `/var/www/certbot` 은 호스트
  디렉터리 bind(ro) — certbot 이 호스트 프로세스라 named volume 은 챌린지가 닿지 않는다.
- 갱신 훅 `/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh` 는 root 로 실행되므로
  **절대경로만** 사용(`~` = `/root` 로 확장), 내용은 컨테이너 nginx reload 1줄.
- topping.cloud server block 은 만들지 않는다 — 사용자가 프로젝트 미사용을 확인, 서빙 중단.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 호스트 nginx 유지 + 설정만 저장소 동기화 | 마이그레이션 리스크 0 | 서버 재구축 시 nginx·certbot 수동 설치, 스택이 두 세계(호스트+compose)로 갈라짐. 사용자 결정으로 compose 편입 선택 |
| certbot 도 컨테이너화 | 스택 완전 선언화 | 1회성 이득 대비 인증서 상태·타이머 이관 복잡도. 호스트 certbot 은 이미 동작 실적이 있고 webroot 전환만으로 충분 |
| 유지보수 페이지 없이 502 노출 | 구현 0 | 크롤러가 오류/점검 화면을 수집. Retry-After 있는 503 이 색인 보호의 표준 |

## Consequences — 영향

- **긍정:** 서버 전체가 `compose.yaml` + 저장소 설정으로 재현 가능. 배포 중 사용자는
  브라우저 오류가 아니라 한국어 점검 페이지(30초 자동 새로고침)를 본다.
- **부정:** nginx 컨테이너 **자체**를 재시작하는 배포(설정 변경 시)는 수 초의 TCP 거부
  창이 있다 — 유지보수 페이지로 가릴 수 없다. 앱이 graceful shutdown 중 스스로 내는
  503 짧은 창은 앱 오류 페이지가 통과한다(`proxy_intercept_errors off` 유지 — intercept
  on 은 앱 5xx 페이지 전체를 삼킨다).
- **부정:** 인증서 갱신 실패는 약 60일 뒤에야 발현한다 — 전환 직후 `renew --dry-run` 과
  훅 직접 실행 검증이 런북에 필수 단계로 있고, dry-run 은 deploy hook 을 실행하지 않으므로
  훅 단독 검증을 생략하면 안 된다.
- **후속:** compose 상대 볼륨 경로는 compose 파일 위치 기준이다 — compose.yaml 은 반드시
  상태 디렉터리와 같은 층위(`~/apps/calendar/compose.yaml`)로 동기화하며, deploy.sh
  preflight 가 `docker compose config` 로 매 배포 해석 결과를 검증한다(경로 회귀 시 빈 DB
  로 새로 기동하는 조용한 유실 차단).

## References

- `deploy/nginx/datedate.conf`, `deploy/nginx/maintenance.html`, `deploy/compose.yaml`
- `deploy/deploy.sh` (엣지 게이트·nginx reload)
- 전환 절차: `docs/operations/server-migration-runbook.md` 7·8단계
