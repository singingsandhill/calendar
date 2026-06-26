# ADR-0004: GET /{ownerId} 의 owner 자동 생성 제거 → 미존재 owner 는 404 + 빈 대시보드

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-06-11 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 / SEO / 에러 처리 |
| 관련 커밋 | (Section P, Commit 54) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

AdSense "Low value content" 재심사 대비 감사에서 발견:

1. **GET 요청이 mutation 발생** — `OwnerController.dashboard` 가 GET 에서
   `getOrCreateOwner` 호출. `datedate.site/zz-no-such-page` 같은 임의 URL
   (`[a-z0-9-]{2,20}` 매치) 이 **HTTP 200 + Owner row 영속화** (라이브 확인됨).
   검색 봇이 GET 만 호출해도 DB 가 봇 트래픽으로 오염.
2. **소프트 404** — 존재하지 않는 페이지가 무한히 200 으로 응답. noindex 메타가
   색인은 막지만, GSC 크롤 예산 낭비 + "무한 200 URL 공간" 자체가 품질 신호 오염.
3. **ADR-0003 의 사각지대** — schedule 자동 생성은 제거했지만 (GET 무변형 원칙),
   owner 단계의 같은 패턴이 남아 있었음.

## Decision — 무엇을 골랐나

GET 무변형 원칙을 owner 로 확장.

- **GET /{ownerId}, owner 미존재** → 생성하지 않고 동일한 `owner/dashboard` 뷰
  (빈 상태 + 일정 만들기 CTA) 를 **HTTP 404** 로 렌더링. 존재 시 기존대로 200.
- **owner 생성 경로 한정** — POST /start (`HomeController`) 와 schedule 생성
  (`ScheduleService.createSchedule` → `getOrCreateOwner`) 두 곳뿐.
- **`HttpServletResponse.setStatus(404)` 사용** — `sendError()` 는 컨테이너 error
  페이지로 포워딩되어 빈 상태 대시보드 UX 를 깨뜨리므로 금지.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 현행 유지 (200 + 생성) | 첫 방문 즉시 사용 | GET mutation, 소프트 404, 봇 발 DB 오염 |
| 200 유지 + 생성만 제거 | UX 동일, DB 보호 | 소프트 404 잔존 — 품질 신호 미해결 |
| 404 + error/4xx 페이지 | 크롤러에 가장 표준적 | 홈 카피가 "datedate.site/내ID 로 시작" 을 안내 — 직접 URL 입력 온보딩 흐름 단절 |
| **(선택) 404 + dashboard 빈 상태 렌더** | 봇에 정확한 신호 + 사람에게 동일 UX | — |

404 페이지에서 일정 생성을 누르면 그제서야 owner 가 생성되는 흐름은
ADR-0003 의 "명시적 create 의도" 원칙과 정확히 일치한다.

## Consequences — 영향

- **긍정:**
  - GET 무변형 완성 (ADR-0003 + 본 ADR) → 캐싱/CDN 안전, 봇 발 Owner row 생성 차단.
  - 소프트 404 제거 — 임의 URL 이 404 를 반환해 GSC/AdSense 품질 신호 정상화.
  - dashboard 미사용 `owner` 모델 속성 제거 (템플릿은 `ownerId`/`schedules` 만 사용).
- **부정:**
  - GET 마다 `ownerExists` 체크 쿼리 1회 추가 (PK 단건 조회 — 무시 가능).
  - 미존재 owner URL 을 공유받은 참여자는 404 상태 페이지를 봄 — 단, 화면은 기존과
    동일한 빈 대시보드라 체감 차이 없음.
- **후속:**
  - 회귀 테스트 `OwnerDashboard404IntegrationTest` 가 불변식 고정
    (404 + 뷰 렌더 + row 미생성 / 존재 owner 200).

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/OwnerController.java`
  - `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/OwnerDashboard404IntegrationTest.java`
- 관련 ADR: [`datedate/domain/0003`](0003-no-auto-create-on-missing-schedule.md) — GET 무변형 원칙의 schedule 단계
- 관련 docs: `docs/adsense-low-value-content-remediation.md`
