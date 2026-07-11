# Schedule Application Layer

> 결정 근거: [`docs/adr/datedate/`](../../../../../../../../docs/adr/datedate/) —
> Schedule 애그리거트 불변식, Selection JSON 컨버터, 자동 생성 제거 등.

## Services

- **OwnerService** - 멱등 owner 생성 (`getOrCreateOwner`). 단, GET `/{ownerId}` 는
  owner 를 생성하지 않음 — 미존재 owner 는 dashboard 빈 상태 + HTTP 404
  ([ADR](../../../../../../../../docs/adr/datedate/domain/0004-no-owner-auto-create-on-get-dashboard.md)).
  생성 경로는 POST /start 와 schedule 생성뿐.
- **ScheduleService** - (ownerId, year, month) 기준 CRUD. 일정 미존재 시 자동 생성 X →
  create 페이지 분기 ([ADR](../../../../../../../../docs/adr/datedate/domain/0003-no-auto-create-on-missing-schedule.md)).
- **ParticipantService** - 스케줄당 최대 8명 / 중복 이름 검증.
- **LocationService** - 장소 투표 (add/delete/vote/unvote).
- **MenuService** - 메뉴 투표 (URL 포함, add/delete/vote/unvote).
- **PopularityService** - 시간 가중 점수 기반 장소/메뉴 인기 순위.
- **SeoService** - 페이지 타입별 SEO 메타데이터 (i18n + JSON-LD 포함).
- **InsightsService** - 집계 인기 통계 (`/insights/trends`).
- **AppUserService** - 카카오 프로필 upsert (`kakaoId` unique, 재로그인 시 닉네임·프로필·lastLoginAt 갱신).
- **UserActivityService** - 로그인 사용자 활동 이벤트(참여·투표·일정생성) append-only 기록,
  (userId, type, targetId) 중복 방지, REQUIRES_NEW + 예외 삼킴으로 본 동작 무영향
  ([ADR](../../../../../../../../docs/adr/datedate/domain/0005-user-activity-event-recap.md)).
- **RecapService** - 연간 recap on-the-fly 집계 (오너 계열 + 활동 계열), 스냅샷 없음.
- **RecapShareService** - (userId, year) 멱등 공유 토큰 발급, 연도 범위 검증(2024~현재).

## 도메인 불변식 위치

`Schedule` 애그리거트가 직접 담당 (참가자 한도, 중복, 주차 변경) —
[ADR](../../../../../../../../docs/adr/datedate/domain/0001-schedule-aggregate-invariants.md).
서비스는 트랜잭션·리포지토리 호출만, 비즈니스 규칙은 도메인 메서드.

`Participant.selections` (`List<Integer>`) 는 `SelectionConverter` (JPA `AttributeConverter`)
로 JSON 직렬화 — [ADR](../../../../../../../../docs/adr/datedate/domain/0002-selections-json-converter.md).

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
