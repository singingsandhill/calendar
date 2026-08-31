# ADR-0006: actuator health 그룹 permitAll + `/actuator/**` denyAll + h2-console 기본 비활성

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-17 |
| 도메인 | common |
| 관심사 | 보안 |
| 관련 ADR | [common/infrastructure/0001](../infrastructure/0001-container-restart-deploy-pipeline.md), [common/infrastructure/0002](../infrastructure/0002-nginx-in-compose-and-certbot-webroot.md) |
| 관련 이슈 | CI/CD 설계 (2026-08-17) |

## Context — 무엇이 문제였나

- 배포 파이프라인(ADR infra/0001)의 헬스 게이트가 쓸 무인증 헬스 엔드포인트가 없었다
  (actuator 미의존). `/actuator/health` 는 2세그먼트라 기존 매처 어디에도 안 걸려
  `authenticated()` 로 떨어지는 반면, **`/*/*/*` permitAll 이 3세그먼트 actuator 경로
  (`/actuator/health/db`, `/actuator/env/foo` 등)를 이미 무인증으로 삼키고 있었다** —
  2세그먼트는 잠기고 3세그먼트는 열리는 뒤집힌 방어선.
- actuator 를 그냥 추가하면 `spring-boot-starter-mail` + `spring.mail.host` 조합으로
  `MailHealthIndicator` 가 자동 등록돼 **헬스 조회마다 smtp.gmail.com:587 실접속**한다 —
  자격증명 미설정/지연 시 집계 DOWN → 모든 배포가 게이트에서 실패하고, healthcheck
  주기(30s)마다 Gmail AUTH 를 때려 계정 차단 위험.
- `/h2-console` 은 `enabled: true` + permitAll + CSRF 예외 + frameOptions sameOrigin 으로
  **운영에서 무인증 SQL 콘솔이 공개**돼 있었다 (사용자 데이터·트레이딩 포지션 DB).

## Decision — 무엇을 골랐나

- **actuator 는 health 만 노출**하고 인디케이터는 화이트리스트(`defaults.enabled: false`
  + db/ping/diskspace 만, **mail 명시 차단**). 게이트 전용 그룹
  `/actuator/health/deploy`(db,ping,diskSpace) 를 폴링한다 — 외부 SMTP 가용성은 배포
  가부의 판단 근거가 될 수 없다.
- **SecurityConfig**: `/actuator/health`·`/actuator/health/deploy` permitAll 직후
  `/actuator/**` **denyAll** — 화이트리스트-그다음-전면차단. denyAll 은 `/*/*/*` permitAll
  보다 **앞에** 선언해야 한다(첫 매칭 우선). 회귀 가드: `ActuatorHealthSecurityTest`.
- **nginx 엣지**: `/actuator`·`/h2-console` 전체 404 (존재 은닉). 게이트는 서버 내
  `localhost:8081` 직결이라 외부 노출이 필요 없다.
- **h2-console 기본 비활성**: `spring.h2.console.enabled: ${H2_CONSOLE_ENABLED:false}`.
  로컬 개발은 `.env` 에 `H2_CONSOLE_ENABLED=true`(`.env.example` 반영) — 개발 편의 손실 0.
  운영 접근은 SSH 터널(`ssh -L 8081:127.0.0.1:8081`) + 일시 활성으로.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| nginx 404 한 겹만 | 코드 무변경 | 방어선이 배포 번들 안의 텍스트 파일 1겹 — rsync 로 교체 가능한 층에 유일 방어를 두면 안 된다. 앱 기본 비활성(1겹) + 엣지 404(2겹) |
| 집계 `/actuator/health` 를 게이트로 사용 | 그룹 불필요 | 인디케이터 추가가 곧 게이트 판정 변경이 된다. 전용 그룹은 "배포 가부 판단 근거"를 명시적 집합으로 고정 |
| `EndpointRequest` 매처 사용 | base-path 변경에 강함 | 이 저장소 SecurityConfig 는 문자열 매처 선언 순서가 곧 문서(CLAUDE.md 표)다 — 스타일 일관성 유지, base-path 변경 계획 없음 |
| SecurityConfig 의 h2-console permitAll·CSRF 예외 제거 | 더 깔끔 | 엔드포인트 자체가 비활성이면 404 라 실익 없이 diff 만 커짐(최소 변경). 후속 정리 후보로만 남김 |

## Consequences — 영향

- **긍정:** 운영 무인증 SQL 콘솔 노출 제거. 배포 게이트가 외부 서비스와 무관하게 동작.
  actuator 표면은 health 2경로만 열리고 나머지는 명시 deny — 훗날 `exposure.include`
  확대가 조용한 공개로 이어지지 않는다.
- **부정:** 서버 `.env` 에 `RUNNER_ADMIN_PASSWORD` 가 없으면 이미지에 구워진 기본값
  admin/admin123 으로 ROLE_ADMIN(실주문 API 전권) 획득이 가능하다 — 이 ADR 범위 밖이지만
  런북 1단계(실측)·6단계(admin/admin123 로그인 **실패** 스모크)가 게이트한다. 근본 해소
  (기본값 제거 → 미설정 시 부팅 실패)는 후속 과제.
- **부정:** 로컬에서 h2-console 을 쓰려면 `.env` 에 키 1줄 추가가 필요하다(1회성).
- **후속:** 배포 SSH 키 = docker 그룹 유저(root 동등) — forced-command 게이트·rrsync
  키 분리로 표면을 좁혔다. 상세는 [infra/0001 Consequences](../infrastructure/0001-container-restart-deploy-pipeline.md).

## References

- `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java` (actuator 매처 2줄)
- `src/main/resources/application.yaml` (`management.*`, `spring.h2.console.enabled`)
- `src/test/java/me/singingsandhill/calendar/common/infrastructure/config/ActuatorHealthSecurityTest.java`
- `deploy/nginx/datedate.conf` (`location ^~ /h2-console`, `location ^~ /actuator`)
