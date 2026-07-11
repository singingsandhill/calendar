# 카카오 로그인 + 연간 Recap — 설계 스펙

- 일자: 2026-07-11
- 상태: Draft
- 범위: datedate 모듈에 카카오 OAuth2 로그인 도입, 오너 계정 연결, 로그인 상태 활동
  이벤트 기록, 연간 Wrapped 스타일 recap 페이지 + 공개 공유 링크
- 관련 문서: [ADR common/security](../../adr/common/security/),
  [ADR datedate/domain](../../adr/datedate/domain/),
  [카카오 로그인 REST API 공식 문서](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)

---

## 1. 문제 정의

datedate 는 완전 익명 구조다. 오너는 공개 URL 슬러그(`/{ownerId}`)로만 식별되고
(`OwnerPathInterceptor` 주석: "현재 인증 레이어가 없는 상태"), 참여자는 이름 문자열,
투표는 `Location`/`Menu` 의 `voters` 문자열 리스트다. 사용자 엔티티·세션·토큰이 전혀
없어 "내 기록" 개념이 존재하지 않는다.

본 스펙은 (1) 카카오 로그인을 **선택적 부가 기능**으로 도입하고 (익명 플로우 병행 유지),
(2) 로그인 사용자의 활동을 이벤트로 기록해, (3) 연간 recap 을 제공한다.

## 2. 확정된 요구사항 (사용자 결정)

| 결정 항목 | 선택 |
|---|---|
| 계정 모델 | 오너 계정 연결 + **로그인 상태에서 한 참여·투표 기록만** 연결 (소급 없음, 익명 병행) |
| recap 주기 | **연간** (Wrapped 스타일 카드 스토리) |
| recap 접근 | 상시 조회 (연중 "현재까지" 집계) + **공개 토큰 공유 링크** (OG 메타태그) |
| 구현 방식 | **Spring Security OAuth2 Client** + 커스텀 Kakao provider 등록 |
| recap 집계 | **활동 이벤트 테이블** (`UserActivity`) — 기존 voters 문자열 구조 무변경 |

## 3. 목표 / 비목표

### 목표
- 헤더 메뉴(두 fragment 모두)에 카카오 로그인/프로필 UI
- 카카오 OAuth2 로그인 → `AppUser` upsert → ROLE_USER 세션
- 오너 연결 (자동: 로그인 상태 `POST /start`, 수동: 대시보드 연결 버튼)
- 로그인 상태 참여/투표/일정생성 시 `UserActivity` 1행 기록
- `/recap/{year}` 본인 recap + `/recap/share/{token}` 공개 공유 페이지
- 기존 러너 어드민 인증·트레이딩 보호 규칙 무회귀

### 비목표 (범위 외)
- 카카오 연결끊기(unlink)·회원 탈퇴 플로우 (추후)
- 이메일 수집 (`account_email` — 비즈 앱 전환 필요, recap 에 불필요)
- 익명 시절 기록의 소급 연결
- 카카오톡 메시지 API 공유 (`talk_message`) — 링크 복사 공유만
- 로그인 필수화 — 모든 기존 익명 플로우는 그대로 동작해야 함

## 4. 카카오 공식 문서 확인 사항 (2026-07-11 기준)

- 인가: `GET https://kauth.kakao.com/oauth/authorize` (`client_id`=REST API 키,
  `redirect_uri`, `response_type=code`)
- 토큰: `POST https://kauth.kakao.com/oauth/token` — `client_secret` 은 **POST body**
  (`application/x-www-form-urlencoded`). Spring 기본값(Basic 헤더)이면 실패하므로
  `client-authentication-method: client_secret_post` 필수.
- 사용자 정보: `GET/POST https://kapi.kakao.com/v2/user/me` → `id`(앱별 고유 회원번호,
  Long), `kakao_account.profile.{nickname, profile_image_url, thumbnail_image_url}`,
  `properties.*`
- scope: `profile_nickname`, `profile_image`
- 액세스 토큰 만료: REST 6시간 / 리프레시 2달 — 본 설계는 로그인 시점 1회 호출만 하므로
  토큰 갱신 불필요 (세션 = 서비스 자체 세션)

### 카카오 콘솔 준비 체크리스트 (코드 밖 선행 작업)
1. [카카오 디벨로퍼스](https://developers.kakao.com) 앱 생성 → REST API 키 확보
2. 앱 설정 > 플랫폼 > Web: `http://localhost:8081`, `https://datedate.site` 등록
3. 제품 설정 > 카카오 로그인 활성화 + Redirect URI 등록:
   - `http://localhost:8081/login/oauth2/code/kakao`
   - `https://datedate.site/login/oauth2/code/kakao`
4. 카카오 로그인 > 보안: Client Secret 생성·활성화
5. 동의항목: 닉네임(`profile_nickname`)·프로필 사진(`profile_image`) 활성화

## 5. 아키텍처

### 5.1 의존성·설정

- `build.gradle`: `implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'`
- `application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            client-name: Kakao
            scope:
              - profile_nickname
              - profile_image
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

- `.env.example`: `KAKAO_CLIENT_ID=`, `KAKAO_CLIENT_SECRET=` 추가.
  test 프로필에는 더미 값 주입 (컨텍스트 로딩 실패 방지).

### 5.2 도메인 모델 (헥사고날 — domain POJO + JpaEntity + adapter 패턴 유지)

**신규 `datedate/domain/user/AppUser`**
```
Long id, Long kakaoId (unique), String nickname, String profileImageUrl,
LocalDateTime createdAt, LocalDateTime lastLoginAt
```
- 닉네임/프로필 이미지는 로그인 때마다 최신값으로 갱신 (upsert)

**신규 `datedate/domain/activity/UserActivity`**
```
Long id, Long userId, ActivityType type, Long scheduleId, Long targetId,
String detail, LocalDateTime occurredAt
```
- `ActivityType { SCHEDULE_CREATED, PARTICIPATION, LOCATION_VOTE, MENU_VOTE }`
- `targetId` (nullable): 대상 행 id — PARTICIPATION 은 participantId, 투표는
  location/menu id. recap 에서 Participant.selections 를 조인해 "선택한 날짜 수"
  집계에 사용
- `detail`: 대상명 (참여자 이름 / 장소명 / 메뉴명). append-only, 수정·삭제 없음
- 중복 방지 기준: `(userId, type, targetId)` 당 1행
- 인덱스: `(user_id, occurred_at)` — 연도별 집계 쿼리용

**신규 `datedate/domain/recap/RecapShare`**
```
Long id, Long userId, int year, String token (UUID, unique), LocalDateTime createdAt
```
- `(userId, year)` unique — 공유 토큰 생성은 멱등 (있으면 기존 토큰 반환)

**변경 `datedate/domain/owner/Owner`**
- nullable `Long userId` 필드 추가 (유저 1 → 오너 N, 오너당 유저 ≤ 1)
- `linkUser(Long userId)`: 이미 다른 유저에 연결돼 있으면 도메인 예외

**변경 `datedate/domain/owner/ReservedOwnerIds`**
- `me`, `recap`, `oauth2` 추가 (`login`, `logout` 등은 기존 예약됨)

DDL 은 `ddl-auto: update` (H2 dev) 로 자동 반영 — 별도 마이그레이션 파일 없음.

### 5.3 인증 통합 (`common/infrastructure/config/SecurityConfig`)

- 기존 단일 필터체인에 `.oauth2Login()` 추가:
  - `loginPage("/login")` — 커스텀 로그인 페이지 (카카오 버튼)
  - `userInfoEndpoint().userService(kakaoOAuth2UserService)`
  - 성공: `SavedRequest` 있으면 원래 목적지, 없으면 `/me`
  - 실패(사용자 취소 포함): `/login?error`
- **신규 `datedate/infrastructure/security/KakaoOAuth2UserService`**
  (`DefaultOAuth2UserService` 상속):
  - attributes 에서 `id`, `kakao_account.profile.nickname`, `profile_image_url` 파싱
  - `AppUserService.upsert(kakaoId, nickname, profileImageUrl)` → `lastLoginAt` 갱신
  - `DefaultOAuth2User(ROLE_USER, attributes + 내부 userId, "id")` 반환 — 컨트롤러가
    내부 `userId` 를 추가 조회 없이 획득
- **엔트리포인트 분리** (`DelegatingAuthenticationEntryPoint`):
  - `/runners/admin/**` → `/runners/admin/login` (기존)
  - 그 외 보호 경로 → `/login`
- 인가 규칙 추가 (기존 트레이딩·어드민 규칙 **앞뒤 순서 유지**, 포괄 permitAll 보다 먼저):

| 경로 | 접근 |
|---|---|
| `/login`, `/oauth2/**`, `/login/oauth2/**`, `/recap/share/**` | permitAll |
| `/me`, `/recap/**`, `/api/me/**` | `ROLE_USER` |

  (선언 순서: `/recap/share/**` permitAll 이 `/recap/**` ROLE_USER 보다 **먼저** —
  기존 트레이딩 규칙 선순위 원칙과 동일)

- 로그아웃: `POST /logout` → `/` 리다이렉트. 어드민 로그아웃(`/runners/admin/logout`)과
  별개. 서비스 세션만 종료 (카카오 토큰 revoke 없음 — 토큰을 저장·재사용하지 않음)
- CSRF: 기존 정책 유지. `/logout`, `POST /recap/**`, `POST /api/me/**` 는 CSRF 토큰
  포함 폼/헤더로 전송 (`/api/**` 는 기존에 CSRF 비활성 — 그대로)

### 5.4 메뉴 UI (`fragments/header.html` — `header`·`header-minimal` 둘 다)

- 비로그인 (`sec:authorize="!hasRole('USER')"` 계열 조건):
  카카오 로그인 버튼 → `GET /oauth2/authorization/kakao`
  - 공식 디자인 가이드: 배경 `#FEE500`, 카카오 심볼 + "카카오 로그인" 레이블
  - 러너 어드민(ROLE_ADMIN) 세션에는 datedate 로그인 버튼을 그대로 노출 (어드민은
    datedate 사용자 아님 — 상호 간섭 없음)
- 로그인 (ROLE_USER): 프로필 이미지+닉네임 드롭다운 — 마이페이지(`/me`) ·
  나의 리캡(`/recap/{올해}`) · 로그아웃(POST `/logout`)
- i18n: `nav.login.kakao`, `nav.mypage`, `nav.recap`, `nav.logout` 키를
  `messages.properties`/`messages_en.properties` 에 추가 (작은따옴표 규칙 준수)

### 5.5 라우트·컨트롤러

| 라우트 | 접근 | 컨트롤러 | 템플릿 |
|---|---|---|---|
| `GET /login` | permitAll | 신규 `AuthController` | `auth/login.html` |
| `GET /me` | ROLE_USER | 신규 `MyPageController` | `me/mypage.html` |
| `GET /recap/{year}` | ROLE_USER | 신규 `RecapController` | `recap/recap.html` |
| `POST /recap/{year}/share` | ROLE_USER | `RecapController` | (redirect) |
| `GET /recap/share/{token}` | permitAll | `RecapController` | `recap/share.html` |
| `POST /api/me/owners/{ownerId}` | ROLE_USER | 신규 `MeApiController` | (JSON) |

- `/{ownerId}` 와일드카드보다 구체 경로가 우선 매칭되지만, `ReservedOwnerIds` 추가로
  이중 방어 (§5.2)
- `year` 유효 범위: 2024 ~ 현재 연도 (밖이면 400)

### 5.6 오너 연결 플로우

1. **자동**: 로그인 상태에서 `POST /start` → `getOrCreateOwner` 후 오너가 미연결이면
   현재 userId 로 연결
2. **수동**: 로그인 상태에서 `/{ownerId}` 대시보드 방문 시 미연결 오너면
   "내 계정에 연결" 버튼 노출 → `POST /api/me/owners/{ownerId}`
   - 이미 다른 유저에 연결: 409 (`OwnerAlreadyLinkedException`)
   - 내가 이미 연결: 200 멱등
3. **리스크 (명시)**: ownerId 는 공개 슬러그이고 소유 증명 수단이 원천적으로 없다
   (기존 구조의 한계). 연결은 **선점(first-claim)** 정책 — 악의적 선점 시 관리자가 DB
   에서 해제하는 것 외 구제 수단 없음. 서비스 규모 감안해 수용하고, 대시보드 연결
   버튼에 "본인이 만든 일정만 연결하세요" 안내 문구 표기.

### 5.7 활동 이벤트 기록

신규 `datedate/application/service/UserActivityService.record(userId, type, scheduleId, detail)`.

기록 지점 (각 API 성공 후, **인증된 ROLE_USER 세션일 때만**; 익명이면 no-op):

| API | 이벤트 |
|---|---|
| `POST /api/schedules/{id}/participants` (참여자 추가) | `PARTICIPATION`, targetId=participantId, detail=참여자명 |
| `PATCH /api/participants/{id}/selections` (날짜 선택) | 같은 `(userId, PARTICIPATION, participantId)` 행 없으면 기록 |
| `POST /api/locations/{id}/votes` | `LOCATION_VOTE`, detail=장소명 |
| `POST /api/menus/{id}/votes` | `MENU_VOTE`, detail=메뉴명 |
| `POST /api/owners/{ownerId}/schedules/{y}/{m}` (일정 생성) | `SCHEDULE_CREATED` |

- 컨트롤러가 `Authentication` 에서 userId 를 추출해 서비스에 전달 (도메인은
  Spring Security 미의존 — 헥사고날 준수)
- 이벤트 기록 실패는 본 동작(참여/투표)을 실패시키지 않음 (로그만 남김)
- 기존 익명 요청 코드 경로는 무변경

### 5.8 Recap 집계·콘텐츠

신규 `RecapService.buildRecap(userId, year)` — on-the-fly 집계 (스냅샷 없음, 데이터
규모상 충분). 소스 2계열:

- **오너 계열**: 내 userId 에 연결된 오너들의 해당 연도 생성 일정 → 일정 수, 참여자
  합계·이름 빈도, 요일 분포(참여자 selections 기준), 월별 분포
- **활동 계열**: 내 `UserActivity` 의 해당 연도 이벤트 → 참여 일정 수 (distinct
  scheduleId), 선택한 날짜 수 (PARTICIPATION 의 targetId 로 Participant.selections
  조인 합산), 투표한 장소/메뉴 TOP3 (detail 빈도)

카드 시퀀스 (`recap/recap.html`, 세로 스크롤 스냅 카드):

1. 인트로 — "{닉네임}님의 {year} 모임 리캡"
2. 만든 일정 N개 · 함께한 사람 연인원 M명
3. 참여한 일정 K개 · 선택한 날짜 총 D일
4. 가장 많이 모인 요일 · 가장 바빴던 달
5. 투표한 장소 TOP3 · 메뉴 TOP3
6. 자주 함께한 참여자 이름 TOP3 (내 오너 일정 기준)
7. 아웃트로 — 공유 링크 복사 버튼 (`POST /recap/{year}/share` → 토큰 URL)

빈 데이터(해당 연도 기록 0건): 빈 상태 카드 — "아직 기록이 없어요. 첫 일정을
만들어보세요" + `POST /start` 유도.

**공유 페이지** (`/recap/share/{token}`, permitAll):
- 동일 카드 구성의 정적 뷰 (닉네임 노출, 프로필 이미지 미노출)
- `SeoMetadata` 로 OG 태그 (title: "{닉네임}님의 {year} 모임 리캡 — datedate",
  description: 핵심 수치 요약) — 카카오톡 미리보기 카드 대응
- `robots: noindex` (개인 데이터 — 검색 인덱스 제외)
- 토큰 미존재: 404

### 5.9 예외 (기존 2-layer 규칙 준수 — `BusinessException` 상속)

| 예외 | HTTP | 코드 |
|---|---|---|
| `RecapShareNotFoundException` | 404 | `RECAP_SHARE_NOT_FOUND` |
| `OwnerAlreadyLinkedException` | 409 | `OWNER_ALREADY_LINKED` |
| `UserNotFoundException` | 404 | `USER_NOT_FOUND` |

OAuth 실패(취소·토큰 교환 실패)는 시큐리티 레이어에서 `/login?error` 처리 (예외 계층 밖).

## 6. 테스트 계획

| 대상 | 테스트 |
|---|---|
| `KakaoOAuth2UserService` | attributes 파싱(중첩 kakao_account), 신규 가입 upsert, 재로그인 시 닉네임 갱신·lastLoginAt |
| SecurityConfig 회귀 | `/me`·`/recap/2026` 미인증 → 302 `/login`; `/recap/share/{token}` 무인증 200; **기존 admin/trading 차단 테스트 GREEN 유지**; ROLE_USER 로 `/trading` → 403 |
| `RecapService` | 연도 필터, 요일/월 집계, TOP3 산출, 빈 데이터 (Clock 고정) |
| `UserActivityService` | 로그인 시 기록 / 익명 시 no-op / PARTICIPATION 중복 방지 / 기록 실패가 본 동작 미차단 |
| 오너 연결 | 자동 연결, 수동 연결 멱등, 타 유저 선점 409 |
| `RecapShare` | 토큰 멱등 생성, 공유 페이지 렌더, 404 |

## 7. 문서 동기화 (CLAUDE.md/ADR 규칙)

- **ADR 신규 2건** (결정 변경):
  - `docs/adr/common/security/0004-kakao-oauth2-login.md` — 카카오 OAuth2 로그인 도입,
    엔트리포인트 분리, client_secret_post 근거
  - `docs/adr/datedate/domain/0005-user-activity-event-recap.md` — voters 구조 무변경
    + 활동 이벤트 테이블 결정, first-claim 오너 연결 정책
- CLAUDE.md: Security 표 (신규 경로 규칙), DateDate 모듈 섹션 (recap), i18n 키 언급 불필요
- `docs/adr/README.md` 색인 갱신
- 커밋: 저장소 규칙에 따라 직접 커밋하지 않고 `docs/git_commit.md` 에 커밋 섹션 추가

## 8. 오픈 리스크

- **first-claim 오너 연결** (§5.6) — 소유 증명 부재, 명시적 수용
- Spring Boot 4 (Spring Security 7) 에서 `oauth2Login` DSL 시그니처가 6.x 와 다를 수
  있음 — 구현 시 실제 API 확인 (컴파일로 즉시 검증됨)
- 운영 도메인 Redirect URI 는 배포 전 카카오 콘솔 등록 필수 (§4 체크리스트)
