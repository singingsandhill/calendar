# Schedule Application Layer

> 결정 근거: [`docs/adr/datedate/`](../../../../../../../../docs/adr/datedate/) —
> Schedule 애그리거트 불변식, Selection JSON 컨버터, 자동 생성 제거 등.

## Services

- **OwnerService** - 멱등 owner 생성 (`getOrCreateOwner`). 단, GET `/{ownerId}` 는
  owner 를 생성하지 않음 — 미존재 owner 는 dashboard 빈 상태 + HTTP 404
  ([ADR](../../../../../../../../docs/adr/datedate/domain/0004-no-owner-auto-create-on-get-dashboard.md)).
  생성 경로는 POST /start 와 schedule 생성뿐.
  랜덤 생성 경로는 예외 — `generateAvailableOwnerId()` 가 `OwnerIdGenerator`(40×40 단어 ×
  1000~9999 = 14,400,000 조합) 후보 중 `existsById` 를 통과한 것만 내고, `createOwner()` 는
  이미 존재하면 `OwnerIdTakenException`(409). 재진입이 정상인 직접 입력과 달리 랜덤 ID 의
  중복은 충돌이라 남의 페이지로 흘려보내지 않는다
  ([ADR](../../../../../../../../docs/adr/datedate/domain/0007-collision-free-random-owner-id.md)).
- **ScheduleService** - (ownerId, year, month) 기준 CRUD. 일정 미존재 시 자동 생성 X →
  create 페이지 분기 ([ADR](../../../../../../../../docs/adr/datedate/domain/0003-no-auto-create-on-missing-schedule.md)).
- **ParticipantService** - 스케줄당 최대 8명 / 중복 이름 검증.
- **LocationService** - 장소 투표 (add/delete/vote/unvote).
- **MenuService** - 메뉴 투표 (URL 포함, add/delete/vote/unvote).
- **PopularityService** - 시간 가중 점수 기반 장소/메뉴 인기 순위. 노출 기준: 집계 후
  최소 2표 + 비속어 블록리스트, 홈·트렌드·top 전역 적용
  ([ADR](../../../../../../../../docs/adr/datedate/domain/0006-popularity-exposure-criteria.md)).
- **SeoService** - 페이지 타입별 SEO 메타데이터 (i18n + JSON-LD 포함). BreadcrumbList 는
  `breadcrumbJsonLd()` 한 곳에서만 생성 — 홈 → 현재 페이지 2단계, **모든 `ListItem` 에 `item` 필수**
  ([ADR](../../../../../../../../docs/adr/common/seo/0008-breadcrumb-item-on-every-listitem.md)).
  guides 계열: `getGuidesIndexSeo()`(CollectionPage, 광고 없음) + `getGuideArticleSeo(slug)`
  (Article JSON-LD — datePublished/dateModified 는 `GuideSlugs` SSOT, 저자 Organization,
  [ADR](../../../../../../../../docs/adr/common/seo/0011-guides-editorial-hub.md)).
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

`Participant.selections` (`List<Integer>`) 는 `SelectionListConverter` (JPA
`AttributeConverter`, `infrastructure/persistence/converter/`) 로 JSON 직렬화 —
[ADR](../../../../../../../../docs/adr/datedate/domain/0002-selections-json-converter.md).

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
