# 0004. 카카오 OAuth2 로그인 도입 (datedate 사용자 영역)

- Status: Accepted
- Date: 2026-07-11

## Context

datedate 는 인증 레이어가 없는 완전 익명 구조였다 (오너 = 공개 URL 슬러그).
연간 recap 등 "내 기록" 기능을 위해 선택적 로그인이 필요해졌다. 기존 러너
어드민 폼 로그인(ADR 0001)과 한 필터체인에서 공존해야 하고, 익명 플로우는
그대로 동작해야 한다.

## Decision

1. **Spring Security OAuth2 Client + Kakao provider 수동 등록.** 수동 REST
   구현 대비 state 검증·토큰 교환·세션 통합을 프레임워크가 처리한다.
   카카오는 `CommonOAuth2Provider` 에 없어 직접 등록해야 한다. `ClientRegistration`
   은 `application.yaml` 의 `spring.security.oauth2.client.*` 프로퍼티가 아니라
   `KakaoOAuth2ClientConfig` 의 명시적 `@Bean`(top-level `kakao.oauth2.*`
   프로퍼티를 `@Value` 로 주입)으로 등록한다.
2. **프로퍼티 방식 대신 빈 등록을 택한 이유.** `spring.security.oauth2.client.*`
   프로퍼티는 Spring Boot 의 OAuth2 클라이언트 자동설정 조건을 충족시켜,
   이를 명시적으로 스캔하지 않는 모든 bare `@WebMvcTest` 슬라이스에까지
   `OAuth2ClientWebSecurityAutoConfiguration` 을 끌어들인다. 그 결과 슬라이스에는
   `HttpSecurity` 빈이 없어 컨텍스트 로드 자체가 깨진다
   (`ScheduleApiControllerTest` 6/6 실패로 발견). `@Configuration` 클래스는
   `@WebMvcTest` 슬라이스에 컴포넌트 스캔되지 않으므로, 빈으로 등록하면 이
   자동설정이 back-off 되어 회귀가 사라진다.
3. **`client-authentication-method: client_secret_post`.** 카카오 토큰
   엔드포인트는 client_secret 을 POST body 로만 받는다 (Basic 헤더는 KOE010
   실패). `KakaoClientRegistrationTest` 가 회귀 가드.
4. **커스텀 `KakaoOAuth2UserService`** 가 `/v2/user/me` 를 파싱해 `AppUser`
   upsert 후 ROLE_USER 프린시펄을 만든다. 내부 userId 를 attributes 에 실어
   컨트롤러 재조회를 없앤다. scope 는 profile_nickname·profile_image 만
   (account_email 은 비즈 앱 전환 필요 — 범위 외).
5. **진입점 분리.** `/runners/admin/**`·`/trading*`·`/api/trading/**` 미인증은
   기존 어드민 로그인으로, 그 외 보호 경로(`/me`, `/recap/**`)는 `/login`
   (카카오 버튼)으로. 로그아웃도 2계열(`/runners/admin/logout` → `/runners`,
   `POST /logout` → `/`). 구현은 `defaultAuthenticationEntryPointFor` 로 어드민
   경로 4건을 먼저 매핑하고, 마지막에 `AnyRequestMatcher.INSTANCE` 매핑(카카오
   사용자 영역, catch-all)을 하나 더 등록하는 방식이다.
   `ExceptionHandlingConfigurer#authenticationEntryPoint(...)` 를 별도로 호출하면
   앞서 등록한 `defaultAuthenticationEntryPointFor` 매핑 전체가 무시되고 그 값
   하나로 완전히 대체되는 Spring Security 동작 때문에 (`DelegatingAuthenticationEntryPoint`
   가 아예 생성되지 않음), 사용자 진입점은 반드시 마지막 catch-all
   `defaultAuthenticationEntryPointFor` 항목으로 등록해야 어드민 매처가 먼저
   평가된다. `DatedateAuthSecurityTest` 가 회귀 가드.
6. **접근 규칙 순서.** `/recap/share/**` permitAll → `/me`·`/recap/**`·
   `/api/me/**` ROLE_USER 를 포괄 permitAll(`/api/**`, `/*`) 보다 먼저 선언
   (ADR 0003 과 동일 원칙). 회귀 가드: `DatedateAuthSecurityTest`.

## Consequences

- 카카오 액세스 토큰은 저장·재사용하지 않는다 (로그인 시 1회 사용, 세션은
  서비스 자체 세션). 연결끊기(unlink)·탈퇴는 후속 과제.
- ROLE_USER 는 어드민 영역에 접근 불가(403). 어드민 세션은 datedate 사용자
  기능(/me 등)에 접근 불가 — 역할 상호 배타.
- `/api/me/**` 는 기존 정책대로 `/api/**` CSRF 예외 + 무자격증명 CORS 구역에
  있다 — 교차출처는 세션 쿠키가 전송되지 않아 차단되지만, CSRF 토큰화는
  트레이딩 API 와 함께 후속 과제 (ADR 0003 과 동일한 상태).
- 배포 전 카카오 콘솔에 운영 Redirect URI 등록 필수.
