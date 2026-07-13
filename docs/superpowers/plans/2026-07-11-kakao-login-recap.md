# 카카오 로그인 + 연간 Recap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** datedate 모듈에 카카오 OAuth2 로그인(선택적)과 연간 Wrapped 스타일 recap(상시 조회 + 공개 공유 링크)을 추가한다.

**Architecture:** Spring Security OAuth2 Client 에 Kakao provider 를 수동 등록하고, 커스텀 `KakaoOAuth2UserService` 가 `/v2/user/me` 응답을 파싱해 `AppUser` 를 upsert 한다. 로그인 사용자의 참여·투표·일정생성은 append-only `UserActivity` 이벤트로 기록하고(기존 익명 구조 무변경), `RecapService` 가 오너 연결 데이터 + 활동 이벤트를 연도별 on-the-fly 집계한다.

**Tech Stack:** Spring Boot 4.0.0 (Spring Security 7), Java 21, Thymeleaf + thymeleaf-extras-springsecurity6, JPA/H2, JUnit 5 + Mockito + spring-security-test.

**Spec:** `docs/superpowers/specs/2026-07-11-kakao-login-recap-design.md`

> **구현 완료 노트 (2026-07-13):** Task 1~8 전부 구현·커밋 완료 (전체 스위트 413 tests,
> 0 failures GREEN — 체크박스는 실행 당시 TDD 추적용이라 미체크 상태 그대로 둠). 구현 중
> 계획 대비 결정 변경 2건은 ADR `common/security/0004` 에 기록되었고 본문 해당 스텝에도
> 반영됨: ① Kakao `ClientRegistration` 은 `spring.security.oauth2.client.*` 프로퍼티가
> 아니라 `KakaoOAuth2ClientConfig` 빈으로 등록 (Task 1 Step 4), ② 미인증 진입점 분리는
> `authenticationEntryPoint(...)` 가 아니라 마지막 `defaultAuthenticationEntryPointFor(
> userEntryPoint, AnyRequestMatcher.INSTANCE)` catch-all 로 등록 (Task 3 SecurityConfig).
> 그 밖의 테스트 슬라이스 미세 조정(`@MockitoBean(name = "localeLinks")`, 슬라이스 전용
> FixedClockConfig, `redirectedUrlPattern` 선행 슬래시)은 `docs/git_commit.md` Commit
> 72~81 섹션의 본문에 원인과 함께 기록되어 있으며, 최종 코드가 정본이다. 남은 작업은
> `docs/kakao-login-recap-checklist.md` 의 실 키 수동 QA·서버 배포 항목뿐.

## Global Constraints

- **Spring Boot 4.0.0 / Spring Security 7** — `AntPathRequestMatcher` 는 존재하지 않음. 명시적 매처는 `org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults().matcher(...)` 사용.
- **헥사고날 패턴** — domain POJO + repository 포트 인터페이스(`domain/`), JPA 엔티티 + Spring Data 리포지토리 + 어댑터(`infrastructure/persistence/`), 컨트롤러·DTO(`presentation/`). DTO 는 record + `from()` 팩토리.
- **예외** — 반드시 `me.singingsandhill.calendar.common.application.exception.BusinessException` 상속: `super(code, message, HttpStatus)`.
- **트랜잭션** — 서비스 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional` 오버라이드.
- **테스트 실행 (WSL)** — `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"<pattern>\""` (프로젝트 루트 `/mnt/d/projects/calendar` 에서).
- **git commit 금지** — 이 저장소에서는 절대 `git commit` 을 실행하지 않는다. 각 태스크의 커밋 스텝은 `docs/git_commit.md` 에 커밋 섹션(番号 Commit 72부터)을 **추가**하는 것으로 대체한다.
- **i18n** — `messages.properties`(한국어, native2ascii 이스케이프) + `messages_en.properties`(영어) 양쪽에 키 추가. 인자 없는 메시지에 `''` 금지(화면에 그대로 노출됨). 숫자 인자는 `{0,number,#}` 로 천단위 그룹화 차단 (연도 "2,026" 방지).
- **SecurityConfig 규칙 순서** — 구체 규칙(hasRole)은 포괄 `permitAll`(`/api/**`, `/*`) 보다 **먼저** 선언. `/recap/share/**` permitAll 은 `/recap/**` hasRole 보다 먼저.
- **카카오 공식 엔드포인트** (developers.kakao.com 확인, 2026-07-11) — 인가 `https://kauth.kakao.com/oauth/authorize`, 토큰 `https://kauth.kakao.com/oauth/token` (client_secret 은 **POST body** → `client-authentication-method: client_secret_post` 필수), 사용자 정보 `https://kapi.kakao.com/v2/user/me` (`id` Long, `kakao_account.profile.{nickname, profile_image_url}`, `properties.{nickname, profile_image}` 폴백), scope `profile_nickname`, `profile_image`. 이메일 scope 사용 금지(비즈 앱 필요, 범위 외).
- **개인 데이터 페이지** (`/me`, `/recap/**`, `/recap/share/**`) 는 SEO `robots: noindex, nofollow`.
- **기존 익명 플로우 무변경** — 비로그인 요청 경로의 동작·응답은 그대로여야 한다.

### 사전 준비 (코드 밖, 사람 작업 — 실 로그인 검증 시점까지만 필요)

1. developers.kakao.com 앱 생성 → REST API 키 확보
2. 플랫폼 > Web: `http://localhost:8081`, `https://datedate.site` 등록
3. 카카오 로그인 활성화 + Redirect URI: `http://localhost:8081/login/oauth2/code/kakao`, `https://datedate.site/login/oauth2/code/kakao`
4. 보안 > Client Secret 생성·활성화
5. 동의항목: 닉네임·프로필 사진 활성화
6. `.env` 에 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 기입

---

### Task 1: OAuth2 클라이언트 의존성 + Kakao provider 설정

**Files:**
- Modify: `build.gradle` (dependencies 블록)
- Modify: `src/main/resources/application.yaml` (최상위 `kakao.oauth2` 블록)
- Create: `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoOAuth2ClientConfig.java`
- Modify: `.env.example`
- Test: `src/test/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoClientRegistrationTest.java` (Create)

**Interfaces:**
- Consumes: 없음 (최초 태스크)
- Produces: registrationId `"kakao"` 의 `ClientRegistrationRepository` 빈. 이후 태스크의 `/oauth2/authorization/kakao` 진입점과 `KakaoOAuth2UserService` 가 이 등록을 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoClientRegistrationTest.java`:

```java
package me.singingsandhill.calendar.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADR common/security/0004: 카카오는 client_secret 을 POST body 로만 받는다.
 * client_secret_post 가 아니면 토큰 교환이 KOE010 으로 실패하므로 회귀 가드.
 */
@SpringBootTest
@ActiveProfiles("test")
class KakaoClientRegistrationTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("kakao 클라이언트 등록은 공식 엔드포인트와 client_secret_post 를 사용한다")
    void kakaoRegistrationMatchesOfficialDocs() {
        ClientRegistration kakao = clientRegistrationRepository.findByRegistrationId("kakao");

        assertThat(kakao).isNotNull();
        assertThat(kakao.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://kauth.kakao.com/oauth/authorize");
        assertThat(kakao.getProviderDetails().getTokenUri())
                .isEqualTo("https://kauth.kakao.com/oauth/token");
        assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUri())
                .isEqualTo("https://kapi.kakao.com/v2/user/me");
        assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("id");
        assertThat(kakao.getClientAuthenticationMethod())
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        assertThat(kakao.getScopes()).containsExactlyInAnyOrder("profile_nickname", "profile_image");
        assertThat(kakao.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/kakao");
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*KakaoClientRegistrationTest\""`
Expected: **컴파일 실패** — `ClientRegistrationRepository` 클래스 없음 (oauth2-client 의존성 부재).

- [ ] **Step 3: build.gradle 의존성 추가**

`build.gradle` 의 `implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'` 줄 바로 아래에:

```groovy
	// 카카오 OAuth2 로그인 (ADR common/security/0004)
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

- [ ] **Step 4: Kakao ClientRegistration 빈 등록 + 자격증명 프로퍼티**

> **결정 변경 (ADR common/security/0004):** 애초 계획한 `spring.security.oauth2.client.*`
> 프로퍼티 방식은 모든 `@WebMvcTest` 슬라이스에 `OAuth2ClientWebSecurityAutoConfiguration`
> 을 끌어들여 HttpSecurity 부재로 컨텍스트 로드를 깨뜨린다(`ScheduleApiControllerTest` 등
> 회귀). 그래서 `ClientRegistration` 은 `KakaoOAuth2ClientConfig` 빈으로 직접 등록하고
> (`@Configuration` 은 슬라이스에 스캔되지 않음), yaml 에는 자격증명만 최상위
> `kakao.oauth2` 프로퍼티로 둔다.

`src/main/resources/application.yaml` 최상위 `app:` 블록 아래에 추가:

```yaml
# 카카오 OAuth2 로그인 자격증명 (ADR common/security/0004).
# spring.security.oauth2.client.* 프로퍼티 방식은 @WebMvcTest 슬라이스에서
# OAuth2ClientWebSecurityAutoConfiguration 이 HttpSecurity 부재로 컨텍스트 로드를 깨뜨려,
# ClientRegistration 은 KakaoOAuth2ClientConfig 빈으로 직접 등록한다.
kakao:
  oauth2:
    # REST API 키. 미설정 시 더미로 부팅만 가능(로그인 버튼은 동작 안 함).
    client-id: ${KAKAO_CLIENT_ID:kakao-client-id-unset}
    client-secret: ${KAKAO_CLIENT_SECRET:kakao-client-secret-unset}
```

`src/main/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoOAuth2ClientConfig.java` 생성:

```java
package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * 카카오 ClientRegistration 수동 등록 (ADR common/security/0004).
 * spring.security.oauth2.client.* 프로퍼티 대신 빈으로 등록하는 이유:
 * 프로퍼티 방식은 모든 @WebMvcTest 슬라이스에 OAuth2 자동설정을 끌어들여
 * HttpSecurity 부재로 컨텍스트 로드를 깨뜨린다 (@Configuration 은 슬라이스에 스캔되지 않음).
 * 카카오 공식 문서: 토큰 엔드포인트는 client_secret 을 POST body 로만 받는다 → client_secret_post.
 */
@Configuration
public class KakaoOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${kakao.oauth2.client-id}") String clientId,
            @Value("${kakao.oauth2.client-secret}") String clientSecret) {
        ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/kakao")
                .scope("profile_nickname", "profile_image")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        return new InMemoryClientRegistrationRepository(kakao);
    }
}
```

- [ ] **Step 5: .env.example 에 키 추가**

`.env.example` 끝에 추가:

```properties
# Kakao OAuth2 Login (developers.kakao.com > 앱 > REST API 키 / 보안 > Client Secret)
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*KakaoClientRegistrationTest\""`
Expected: PASS (test 프로필은 더미 client-id 로 부팅).

- [ ] **Step 7: 기존 스위트 회귀 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*TradingApiSecurityTest\" --tests \"*CorsConfigTest\""`
Expected: PASS — 아직 SecurityConfig 를 안 건드렸으므로 영향 없음.

- [ ] **Step 8: git_commit.md 에 커밋 섹션 추가**

`docs/git_commit.md` 끝에 추가 (git commit 은 실행하지 않는다):

```
# Commit 72 — feat(common): 카카오 OAuth2 클라이언트 의존성·등록
git add build.gradle src/main/resources/application.yaml .env.example src/main/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoOAuth2ClientConfig.java src/test/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoClientRegistrationTest.java docs/git_commit.md
git commit -m "feat(common): spring oauth2-client 의존성 + Kakao provider 등록" -m "공식 문서 엔드포인트(kauth authorize/token, kapi /v2/user/me), client_secret_post, scope profile_nickname·profile_image. 미설정 환경 부팅용 더미 기본값. KakaoClientRegistrationTest 회귀 가드. ClientRegistration 은 spring.security.oauth2.client.* 프로퍼티 대신 KakaoOAuth2ClientConfig 빈으로 직접 등록 — 프로퍼티 방식은 @WebMvcTest 슬라이스에 OAuth2ClientWebSecurityAutoConfiguration 을 끌어들여 HttpSecurity 부재로 컨텍스트 로드를 깨뜨리는 회귀를 유발함(ScheduleApiControllerTest 등)."
```

---

### Task 2: AppUser 도메인 + 영속성 + upsert 서비스

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/user/AppUser.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/user/AppUserRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/exception/UserNotFoundException.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/service/AppUserService.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/AppUserJpaEntity.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/AppUserJpaRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/AppUserRepositoryAdapter.java`
- Test: `src/test/java/me/singingsandhill/calendar/datedate/application/service/AppUserServiceTest.java` (Create)

**Interfaces:**
- Consumes: 없음
- Produces:
  - `AppUser` — `getId(): Long`, `getKakaoId(): Long`, `getNickname(): String`, `getProfileImageUrl(): String`(nullable), `getCreatedAt()/getLastLoginAt(): LocalDateTime`, `refreshProfile(String nickname, String profileImageUrl, LocalDateTime loginAt): void`, `static signUp(Long kakaoId, String nickname, String profileImageUrl, LocalDateTime now): AppUser`
  - `AppUserRepository` — `findByKakaoId(Long): Optional<AppUser>`, `findById(Long): Optional<AppUser>`, `save(AppUser): AppUser`
  - `AppUserService` — `upsertKakaoUser(Long kakaoId, String nickname, String profileImageUrl): AppUser`, `getUser(Long userId): AppUser` (미존재 시 `UserNotFoundException` 404)
  - 주입되는 `java.time.Clock` 빈은 stock 모듈이 이미 제공 (Asia/Seoul).

- [ ] **Step 1: 실패하는 서비스 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/application/service/AppUserServiceTest.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), SEOUL);

    @Mock
    private AppUserRepository appUserRepository;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository, FIXED);
    }

    @Test
    @DisplayName("신규 카카오 사용자는 가입 처리되고 lastLoginAt 이 현재 시각이다")
    void upsertCreatesNewUser() {
        when(appUserRepository.findByKakaoId(12345L)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = appUserService.upsertKakaoUser(12345L, "지수", "https://img.example/p.jpg");

        assertThat(user.getKakaoId()).isEqualTo(12345L);
        assertThat(user.getNickname()).isEqualTo("지수");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://img.example/p.jpg");
        assertThat(user.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED));
        assertThat(user.getLastLoginAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("기존 사용자는 재로그인 시 닉네임·프로필이 갱신되고 lastLoginAt 이 갱신된다")
    void upsertRefreshesExistingUser() {
        AppUser existing = new AppUser(7L, 12345L, "옛닉", null,
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 1, 0, 0));
        when(appUserRepository.findByKakaoId(12345L)).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = appUserService.upsertKakaoUser(12345L, "새닉", "https://img.example/new.jpg");

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getNickname()).isEqualTo("새닉");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://img.example/new.jpg");
        assertThat(user.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(user.getLastLoginAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("미존재 사용자 조회는 UserNotFoundException(404)")
    void getUserThrowsWhenMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.getUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*AppUserServiceTest\""`
Expected: 컴파일 실패 — `AppUser` 등 클래스 없음.

- [ ] **Step 3: 도메인 + 예외 + 서비스 구현**

`src/main/java/me/singingsandhill/calendar/datedate/domain/user/AppUser.java`:

```java
package me.singingsandhill.calendar.datedate.domain.user;

import java.time.LocalDateTime;

public class AppUser {

    private final Long id;
    private final Long kakaoId;
    private String nickname;
    private String profileImageUrl;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public AppUser(Long id, Long kakaoId, String nickname, String profileImageUrl,
                   LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        if (kakaoId == null) {
            throw new IllegalArgumentException("kakaoId cannot be null");
        }
        this.id = id;
        this.kakaoId = kakaoId;
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static AppUser signUp(Long kakaoId, String nickname, String profileImageUrl, LocalDateTime now) {
        return new AppUser(null, kakaoId, nickname, profileImageUrl, now, now);
    }

    public void refreshProfile(String nickname, String profileImageUrl, LocalDateTime loginAt) {
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = loginAt;
    }

    private static String normalizeNickname(String nickname) {
        return (nickname == null || nickname.isBlank()) ? "카카오사용자" : nickname;
    }

    public Long getId() {
        return id;
    }

    public Long getKakaoId() {
        return kakaoId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/domain/user/AppUserRepository.java`:

```java
package me.singingsandhill.calendar.datedate.domain.user;

import java.util.Optional;

public interface AppUserRepository {

    Optional<AppUser> findByKakaoId(Long kakaoId);

    Optional<AppUser> findById(Long id);

    AppUser save(AppUser user);
}
```

`src/main/java/me/singingsandhill/calendar/datedate/application/exception/UserNotFoundException.java`:

```java
package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND",
                "User not found with id: " + userId,
                HttpStatus.NOT_FOUND);
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/application/service/AppUserService.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final Clock clock;

    public AppUserService(AppUserRepository appUserRepository, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.clock = clock;
    }

    @Transactional
    public AppUser upsertKakaoUser(Long kakaoId, String nickname, String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now(clock);
        return appUserRepository.findByKakaoId(kakaoId)
                .map(user -> {
                    user.refreshProfile(nickname, profileImageUrl, now);
                    return appUserRepository.save(user);
                })
                .orElseGet(() -> appUserRepository.save(
                        AppUser.signUp(kakaoId, nickname, profileImageUrl, now)));
    }

    public AppUser getUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
```

- [ ] **Step 4: 영속성 구현 (JPA 엔티티 + 리포지토리 + 어댑터)**

`src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/AppUserJpaEntity.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = "kakaoId"))
public class AppUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long kakaoId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastLoginAt;

    protected AppUserJpaEntity() {
    }

    public AppUserJpaEntity(Long id, Long kakaoId, String nickname, String profileImageUrl,
                            LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Long getId() {
        return id;
    }

    public Long getKakaoId() {
        return kakaoId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/AppUserJpaRepository.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.AppUserJpaEntity;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, Long> {

    Optional<AppUserJpaEntity> findByKakaoId(Long kakaoId);
}
```

`src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/AppUserRepositoryAdapter.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.AppUserJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.AppUserJpaRepository;

@Repository
public class AppUserRepositoryAdapter implements AppUserRepository {

    private final AppUserJpaRepository jpaRepository;

    public AppUserRepositoryAdapter(AppUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AppUser> findByKakaoId(Long kakaoId) {
        return jpaRepository.findByKakaoId(kakaoId).map(this::toDomain);
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        AppUserJpaEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    private AppUser toDomain(AppUserJpaEntity entity) {
        return new AppUser(
                entity.getId(),
                entity.getKakaoId(),
                entity.getNickname(),
                entity.getProfileImageUrl(),
                entity.getCreatedAt(),
                entity.getLastLoginAt()
        );
    }

    private AppUserJpaEntity toEntity(AppUser user) {
        return new AppUserJpaEntity(
                user.getId(),
                user.getKakaoId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*AppUserServiceTest\""`
Expected: PASS (3 tests).

- [ ] **Step 6: git_commit.md 에 커밋 섹션 추가**

```
# Commit 73 — feat(datedate): AppUser 도메인·영속성·upsert 서비스
git add src/main/java/me/singingsandhill/calendar/datedate/domain/user/ src/main/java/me/singingsandhill/calendar/datedate/application/exception/UserNotFoundException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/AppUserService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/AppUserJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/AppUserJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/AppUserRepositoryAdapter.java src/test/java/me/singingsandhill/calendar/datedate/application/service/AppUserServiceTest.java docs/git_commit.md
git commit -m "feat(datedate): 카카오 사용자 AppUser 도메인·JPA 영속성·upsert" -m "kakaoId unique, 재로그인 시 닉네임·프로필·lastLoginAt 갱신(Clock 주입 결정성). 헥사고날 domain POJO+port+adapter 패턴."
```

---

### Task 3: KakaoOAuth2UserService + SecurityConfig 통합 (엔트리포인트 분리)

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfile.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoOAuth2UserService.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/presentation/support/AuthenticatedUsers.java`
- Modify: `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java` (전체 교체, 아래 코드)
- Modify: `src/test/java/me/singingsandhill/calendar/trading/presentation/api/TradingApiSecurityTest.java` (mock 2개 추가)
- Modify: `src/test/java/me/singingsandhill/calendar/common/infrastructure/config/CorsConfigTest.java` (mock 2개 추가)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java` (Create)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/presentation/DatedateAuthSecurityTest.java` (Create)

**Interfaces:**
- Consumes: Task 1 의 `kakao` ClientRegistration, Task 2 의 `AppUserService.upsertKakaoUser(Long, String, String): AppUser`
- Produces:
  - `KakaoProfile.from(Map<String,Object>): KakaoProfile` — record `(Long kakaoId, String nickname, String profileImageUrl)`
  - `KakaoOAuth2UserService` (빈) — 로그인 성공 시 `DefaultOAuth2User(ROLE_USER, attributes, "id")` 반환. attributes 에 상수 키 `ATTR_APP_USER_ID`(="appUserId", Long), `ATTR_APP_NICKNAME`(="appNickname"), `ATTR_APP_PROFILE_IMAGE`(="appProfileImage") 추가.
  - `AuthenticatedUsers.currentUserId(Authentication): Optional<Long>` — 이후 모든 컨트롤러가 내부 userId 획득에 사용.
  - SecurityConfig: `/me`, `/recap/**`(share 제외), `/api/me/**` → ROLE_USER; `/login`, `/oauth2/**`, `/login/oauth2/**`, `/recap/share/**` → permitAll; 미인증 진입점: admin 영역→`/runners/admin/login`, 그 외→`/login`; `POST /logout` → `/` 리다이렉트.

- [ ] **Step 1: KakaoProfile 파싱 실패 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class KakaoProfileTest {

    @Test
    @DisplayName("kakao_account.profile 에서 닉네임·프로필 이미지를 파싱한다")
    void parsesKakaoAccountProfile() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "kakao_account", Map.of("profile", Map.of(
                        "nickname", "지수",
                        "profile_image_url", "https://img.kakaocdn.net/p.jpg")),
                "properties", Map.of("nickname", "legacy"));

        KakaoProfile profile = KakaoProfile.from(attributes);

        assertThat(profile.kakaoId()).isEqualTo(12345L);
        assertThat(profile.nickname()).isEqualTo("지수");
        assertThat(profile.profileImageUrl()).isEqualTo("https://img.kakaocdn.net/p.jpg");
    }

    @Test
    @DisplayName("kakao_account.profile 이 없으면 properties 로 폴백한다")
    void fallsBackToProperties() {
        Map<String, Object> attributes = Map.of(
                "id", 99L,
                "properties", Map.of("nickname", "프로퍼티닉", "profile_image", "https://img/p2.jpg"));

        KakaoProfile profile = KakaoProfile.from(attributes);

        assertThat(profile.nickname()).isEqualTo("프로퍼티닉");
        assertThat(profile.profileImageUrl()).isEqualTo("https://img/p2.jpg");
    }

    @Test
    @DisplayName("id (Integer 타입 포함) 를 Long 으로 정규화한다")
    void normalizesIntegerId() {
        KakaoProfile profile = KakaoProfile.from(Map.of("id", 777));

        assertThat(profile.kakaoId()).isEqualTo(777L);
        assertThat(profile.nickname()).isNull();
        assertThat(profile.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("id 가 없으면 OAuth2AuthenticationException")
    void throwsWhenIdMissing() {
        assertThatThrownBy(() -> KakaoProfile.from(Map.of("properties", Map.of())))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*KakaoProfileTest\""`
Expected: 컴파일 실패 — `KakaoProfile` 없음.

- [ ] **Step 3: KakaoProfile + KakaoOAuth2UserService + AuthenticatedUsers 구현**

`src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfile.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.security;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * 카카오 /v2/user/me 응답 파싱 결과.
 * 공식 문서: id(회원번호, Long) 필수. 닉네임·프로필 이미지는
 * kakao_account.profile.{nickname, profile_image_url} 우선, properties.{nickname, profile_image} 폴백.
 */
public record KakaoProfile(Long kakaoId, String nickname, String profileImageUrl) {

    @SuppressWarnings("unchecked")
    public static KakaoProfile from(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        if (!(id instanceof Number number)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response"), "kakao user id is missing");
        }
        Map<String, Object> account = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        Map<String, Object> properties = (Map<String, Object>) attributes.getOrDefault("properties", Map.of());

        String nickname = firstNonBlank(
                (String) profile.get("nickname"),
                (String) properties.get("nickname"));
        String imageUrl = firstNonBlank(
                (String) profile.get("profile_image_url"),
                (String) properties.get("profile_image"));

        return new KakaoProfile(number.longValue(), nickname, imageUrl);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoOAuth2UserService.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import me.singingsandhill.calendar.datedate.application.service.AppUserService;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;

/**
 * 카카오 사용자 정보를 AppUser 로 upsert 하고 ROLE_USER 프린시펄을 만든다.
 * 내부 userId 를 attributes 에 실어 컨트롤러가 추가 조회 없이 쓰게 한다.
 */
@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    public static final String ATTR_APP_USER_ID = "appUserId";
    public static final String ATTR_APP_NICKNAME = "appNickname";
    public static final String ATTR_APP_PROFILE_IMAGE = "appProfileImage";

    private final AppUserService appUserService;

    public KakaoOAuth2UserService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        KakaoProfile profile = KakaoProfile.from(oauth2User.getAttributes());

        AppUser user = appUserService.upsertKakaoUser(
                profile.kakaoId(), profile.nickname(), profile.profileImageUrl());

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put(ATTR_APP_USER_ID, user.getId());
        attributes.put(ATTR_APP_NICKNAME, user.getNickname());
        attributes.put(ATTR_APP_PROFILE_IMAGE, user.getProfileImageUrl());

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/presentation/support/AuthenticatedUsers.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.support;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;

/** 컨트롤러에서 현재 카카오 로그인 사용자의 내부 userId 를 꺼내는 헬퍼. 비로그인·어드민 세션이면 empty. */
public final class AuthenticatedUsers {

    private AuthenticatedUsers() {
    }

    public static Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return Optional.empty();
        }
        Object id = oauth2User.getAttribute(KakaoOAuth2UserService.ATTR_APP_USER_ID);
        return (id instanceof Number number) ? Optional.of(number.longValue()) : Optional.empty();
    }
}
```

- [ ] **Step 4: KakaoProfileTest 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*KakaoProfileTest\""`
Expected: PASS (4 tests).

- [ ] **Step 5: SecurityConfig 전체 교체**

`src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java` 를 아래로 교체:

```java
package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   KakaoOAuth2UserService kakaoOAuth2UserService) throws Exception {
        LoginUrlAuthenticationEntryPoint adminEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/runners/admin/login");
        LoginUrlAuthenticationEntryPoint userEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");

        http
            // CORS: /api/** 를 앱인토스 미니앱(다른 origin)에서 호출 가능하게 함 (CorsConfig 빈 사용).
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // 트레이딩 봇 제어·실주문 API 및 제어 대시보드는 관리자 전용 (P0-1).
                // 반드시 아래 /api/** · /* 포괄 permitAll 규칙보다 먼저 선언해야 매칭 우선순위가 보장된다.
                .requestMatchers("/api/trading/**").hasRole("ADMIN")
                .requestMatchers("/trading", "/trading/**").hasRole("ADMIN")

                // 카카오 로그인 사용자 영역 (ADR common/security/0004).
                // /recap/share/** permitAll 은 /recap/** hasRole 보다 먼저 — 공유 링크는 무인증 공개.
                .requestMatchers("/recap/share/**").permitAll()
                .requestMatchers("/login", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/me", "/recap", "/recap/**", "/api/me/**").hasRole("USER")

                // 기존 앱 경로 - 모두 허용
                .requestMatchers("/", "/start", "/index.html", "/privacy-policy", "/about").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/image/**", "/favicon.*", "/manifest.json", "/robots.txt", "/sitemap.xml", "/ads.txt", "/og-image.svg", "/og-image.png", "/1dfcb4404e1d4f6fae3423fd163f97b8.txt").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // 러너 공개 경로
                .requestMatchers("/runners").permitAll()
                .requestMatchers("/runners/announce").permitAll()
                .requestMatchers("/runners/runs", "/runners/runs/**").permitAll()
                .requestMatchers("/runners/members", "/runners/members/**").permitAll()
                .requestMatchers("/runners/css/**", "/runners/js/**", "/runners/images/**").permitAll()
                .requestMatchers("/runners/admin/login").permitAll()

                // 인사이트 공개 경로
                .requestMatchers("/insights", "/insights/**").permitAll()

                // use-cases 공개 경로
                .requestMatchers("/use-cases", "/use-cases/**").permitAll()

                // 도구 공개 경로 (날짜 계산기 등)
                .requestMatchers("/tools", "/tools/**").permitAll()

                // 러너 관리자 경로 - ADMIN 역할 필요
                .requestMatchers("/runners/admin", "/runners/admin/**").hasRole("ADMIN")

                // 주식 트레이딩 봇 경로
                .requestMatchers("/stock", "/stock/**").permitAll()
                .requestMatchers("/api/stock/**").permitAll()

                // 기존 동적 경로 (owner 페이지 등)
                .requestMatchers("/*").permitAll()
                .requestMatchers("/*/*/*").permitAll()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/runners/admin/login")
                .loginProcessingUrl("/runners/admin/login")
                .defaultSuccessUrl("/runners/admin", true)
                .failureUrl("/runners/admin/login?error=true")
                .permitAll()
            )
            // 카카오 OAuth2 로그인 (ADR common/security/0004)
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(kakaoOAuth2UserService))
                .defaultSuccessUrl("/me")
                .failureUrl("/login?error")
            )
            // 미인증 진입점 분리: 어드민 영역은 어드민 로그인, 그 외(카카오 사용자 영역)는 /login
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(adminEntryPoint,
                        PathPatternRequestMatcher.withDefaults().matcher("/runners/admin/**"))
                .defaultAuthenticationEntryPointFor(adminEntryPoint,
                        PathPatternRequestMatcher.withDefaults().matcher("/trading"))
                .defaultAuthenticationEntryPointFor(adminEntryPoint,
                        PathPatternRequestMatcher.withDefaults().matcher("/trading/**"))
                .defaultAuthenticationEntryPointFor(adminEntryPoint,
                        PathPatternRequestMatcher.withDefaults().matcher("/api/trading/**"))
                // 위 admin 매처에 걸리지 않는 나머지 모든 요청의 기본 진입점 (카카오 사용자 영역).
                // 주의: ExceptionHandlingConfigurer#authenticationEntryPoint(...) 를 별도로 호출하면
                // 위에서 등록한 defaultAuthenticationEntryPointFor 매핑 전체가 무시되고 그 값으로
                // 완전히 대체된다 (DelegatingAuthenticationEntryPoint 미생성) — 반드시 마지막
                // catch-all 매핑으로 등록해야 admin 매처가 우선 평가된다 (ADR 0004).
                .defaultAuthenticationEntryPointFor(userEntryPoint, AnyRequestMatcher.INSTANCE)
            )
            // 로그아웃 2계열: 러너 어드민(/runners/admin/logout → /runners), 카카오 사용자(/logout → /)
            .logout(logout -> logout
                .logoutRequestMatcher(new OrRequestMatcher(
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/runners/admin/logout"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/logout")))
                .logoutSuccessHandler((request, response, authentication) -> {
                    String target = request.getRequestURI().startsWith("/runners") ? "/runners" : "/";
                    response.sendRedirect(target);
                })
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                .ignoringRequestMatchers("/api/**")
                .ignoringRequestMatchers("/runners/runs/*/attendance")
                .ignoringRequestMatchers("/runners/runs/create")
                .ignoringRequestMatchers("/runners/admin/attendance/*/delete")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .cacheControl(cache -> cache.disable())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**주의:** 기존 `.logout(logoutUrl("/runners/admin/logout"))` 은 GET 도 허용했을 수 있다. 러너 어드민 템플릿의 로그아웃이 GET 링크라면(구현 시 `grep -rn "admin/logout" src/main/resources/templates/runners/` 로 확인) `OrRequestMatcher` 에 GET 매처를 하나 추가해 기존 동작을 보존한다:
`PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/runners/admin/logout")`.

- [ ] **Step 6: 기존 시큐리티 테스트 2개에 mock 추가**

`TradingApiSecurityTest.java` 와 `CorsConfigTest.java` 의 `@MockitoBean` 필드 목록에 아래 2개를 추가 (SecurityConfig 가 이제 `KakaoOAuth2UserService` 를 주입받고, oauth2Login 이 `ClientRegistrationRepository` 를 요구하므로):

```java
    @MockitoBean
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService kakaoOAuth2UserService;
```

(import 문으로 옮기고 필드는 단순 타입명으로 정리.)

- [ ] **Step 7: 신규 시큐리티 회귀 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/presentation/DatedateAuthSecurityTest.java`:

```java
package me.singingsandhill.calendar.datedate.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.datedate.presentation.controller.HomeController;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * ADR common/security/0004: 카카오 사용자 영역의 접근 규칙·진입점 분리 회귀 가드.
 * 컨트롤러 도달 전 시큐리티 레이어 동작만 검증한다 (핸들러 미존재 경로는 인가 통과 시 404).
 */
@WebMvcTest(HomeController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class DatedateAuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private PopularityService popularityService;

    @MockitoBean
    private InsightsService insightsService;

    @MockitoBean
    private LocaleLinks localeLinks;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @Test
    @DisplayName("미인증 /me 접근은 카카오 로그인 페이지(/login)로 리다이렉트된다")
    void unauthenticatedMeRedirectsToUserLogin() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("미인증 /recap/2026 접근은 /login 으로 리다이렉트된다")
    void unauthenticatedRecapRedirectsToUserLogin() throws Exception {
        mockMvc.perform(get("/recap/2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("미인증 러너 어드민 접근은 여전히 어드민 로그인으로 리다이렉트된다")
    void unauthenticatedAdminRedirectsToAdminLogin() throws Exception {
        mockMvc.perform(get("/runners/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/runners/admin/login"));
    }

    @Test
    @DisplayName("recap 공유 링크는 무인증으로 인가를 통과한다 (핸들러 미존재 → 404, 302 아님)")
    void shareLinkIsPubliclyAuthorized() throws Exception {
        mockMvc.perform(get("/recap/share/some-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("카카오 사용자(ROLE_USER)는 트레이딩 대시보드에 접근할 수 없다 (403)")
    void kakaoUserCannotAccessTrading() throws Exception {
        mockMvc.perform(get("/trading")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카카오 사용자(ROLE_USER)는 /me 인가를 통과한다 (핸들러 미존재 → 404, 403 아님)")
    void kakaoUserPassesMeAuthorization() throws Exception {
        mockMvc.perform(get("/me")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 8: 전체 시큐리티 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*DatedateAuthSecurityTest\" --tests \"*TradingApiSecurityTest\" --tests \"*CorsConfigTest\" --tests \"*KakaoClientRegistrationTest\""`
Expected: 모두 PASS. 실패 시 러너 어드민 로그아웃 GET/POST 이슈(Step 5 주의사항) 또는 mock 누락 여부 확인.

- [ ] **Step 9: git_commit.md 에 커밋 섹션 추가**

```
# Commit 74 — feat(security): 카카오 OAuth2 로그인 통합 + 진입점 분리 (ADR 0004)
git add src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/ src/main/java/me/singingsandhill/calendar/datedate/presentation/support/AuthenticatedUsers.java src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/DatedateAuthSecurityTest.java src/test/java/me/singingsandhill/calendar/trading/presentation/api/TradingApiSecurityTest.java src/test/java/me/singingsandhill/calendar/common/infrastructure/config/CorsConfigTest.java docs/git_commit.md
git commit -m "feat(security): 카카오 OAuth2 로그인 + 사용자/어드민 진입점 분리" -m "KakaoOAuth2UserService 가 /v2/user/me 파싱→AppUser upsert→ROLE_USER 프린시펄(내부 userId attributes 탑재). /me·/recap/**(share 제외)·/api/me/** ROLE_USER, /recap/share/** 공개. 어드민 영역은 기존 어드민 로그인 진입점 유지, POST /logout 분리. 기존 P0-1 회귀 GREEN."
```

---

### Task 4: 로그인 페이지 + 헤더 로그인 UI + i18n 키

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthController.java`
- Create: `src/main/resources/templates/auth/login.html`
- Modify: `src/main/resources/templates/fragments/header.html` (두 fragment 모두)
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java` (`getLoginSeo()` 추가)
- Modify: `src/main/resources/messages.properties`, `src/main/resources/messages_en.properties`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIds.java` (`me`, `recap`, `oauth2` 추가)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthControllerTest.java` (Create)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIdsTest.java` (Modify — 신규 예약어 3개 검증 추가)

**Interfaces:**
- Consumes: Task 3 의 `/oauth2/authorization/kakao` 진입점, principal attributes `appNickname`/`appProfileImage`
- Produces: `GET /login` 뷰 `auth/login`, 헤더의 로그인/프로필 UI, i18n 키 `nav.login.kakao`, `nav.mypage`, `nav.recap`, `nav.logout`, `login.*`, `seo.login.*`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthControllerTest.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(AuthController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private LocaleLinks localeLinks;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @Test
    @DisplayName("로그인 페이지는 카카오 인가 진입점 링크를 렌더링한다")
    void loginPageRendersKakaoButton() throws Exception {
        when(seoService.getLoginSeo()).thenReturn(SeoMetadata.builder().title("로그인").build());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/oauth2/authorization/kakao")));
    }

    @Test
    @DisplayName("이미 로그인한 카카오 사용자가 /login 에 오면 /me 로 보낸다")
    void loggedInUserIsRedirectedToMyPage() throws Exception {
        mockMvc.perform(get("/login")
                        .with(oauth2Login()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me"));
    }
}
```

`ReservedOwnerIdsTest.java` 에 테스트 추가:

```java
    @Test
    @DisplayName("카카오 로그인 라우트 토큰(me, recap, oauth2)은 예약어다")
    void kakaoAuthRouteTokensAreReserved() {
        assertThat(ReservedOwnerIds.isReserved("me")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("recap")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("oauth2")).isTrue();
    }
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*AuthControllerTest\" --tests \"*ReservedOwnerIdsTest\""`
Expected: AuthControllerTest 컴파일 실패(AuthController 없음), ReservedOwnerIdsTest 신규 케이스 FAIL.

- [ ] **Step 3: ReservedOwnerIds 에 예약어 추가**

`ReservedOwnerIds.java` 의 `RESERVED` Set 에서 `"trading", "stock", "runners",` 줄 다음에 추가:

```java
            // 카카오 로그인/리캡 라우트 (ADR common/security/0004)
            "me", "recap", "oauth2",
```

- [ ] **Step 4: AuthController + SeoService.getLoginSeo() 구현**

`src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthController.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class AuthController {

    private final SeoService seoService;

    public AuthController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        if (AuthenticatedUsers.currentUserId(authentication).isPresent()) {
            return "redirect:/me";
        }
        model.addAttribute("seo", seoService.getLoginSeo());
        return "auth/login";
    }
}
```

`SeoService.java` 의 `getDashboardSeo` 아래에 추가 (기존 헬퍼 `m()`, `canonicalKo/En()`, `ogLocale()`, `baseUrl`, `DEFAULT_OG_IMAGE` 재사용):

```java
    /** 카카오 로그인 페이지 (noindex). */
    public SeoMetadata getLoginSeo() {
        String path = "/login";
        return SeoMetadata.builder()
            .title(m("seo.login.title"))
            .description(m("seo.login.description"))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }
```

- [ ] **Step 5: i18n 키 추가**

한국어 값은 `messages.properties` 의 기존 규칙(유니코드 이스케이프)을 따른다. 아래 파이썬 커맨드로 변환해 붙여넣는다:

```bash
python3 - << 'EOF'
pairs = {
    "nav.login.kakao": "카카오 로그인",
    "nav.mypage": "마이페이지",
    "nav.recap": "나의 리캡",
    "nav.logout": "로그아웃",
    "login.title": "로그인",
    "login.subtitle": "카카오 계정으로 3초 만에 시작하세요",
    "login.kakao.button": "카카오 로그인",
    "login.error": "로그인에 실패했어요. 잠시 후 다시 시도해 주세요.",
    "seo.login.title": "로그인 - DateDate",
    "seo.login.description": "카카오 계정으로 로그인하고 내 일정과 연간 리캡을 확인하세요.",
}
for k, v in pairs.items():
    print(k + "=" + v.encode("unicode_escape").decode().replace("\\x", "\\u00"))
EOF
```

출력된 줄들을 `messages.properties` 의 `nav.aria.open` 줄 아래(nav 블록)와 파일 하단(login/seo 블록)에 붙여넣는다.

`messages_en.properties` 에는 영어 원문 그대로 추가:

```properties
nav.login.kakao=Sign in with Kakao
nav.mypage=My Page
nav.recap=My Recap
nav.logout=Sign out
login.title=Sign in
login.subtitle=Start in seconds with your Kakao account
login.kakao.button=Sign in with Kakao
login.error=Sign-in failed. Please try again shortly.
seo.login.title=Sign in - DateDate
seo.login.description=Sign in with Kakao to see your schedules and yearly recap.
```

- [ ] **Step 6: 로그인 템플릿 작성**

`src/main/resources/templates/auth/login.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:lang="${#locale.language}">
<head th:replace="~{fragments/head :: head(${seo})}"></head>
<body>
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>

<main class="login-page">
    <section class="login-card">
        <h1 th:text="#{login.title}">로그인</h1>
        <p class="login-subtitle" th:text="#{login.subtitle}">카카오 계정으로 3초 만에 시작하세요</p>

        <p class="login-error" th:if="${param.error != null}" th:text="#{login.error}">로그인에 실패했어요.</p>

        <!-- 카카오 공식 디자인 가이드: 컨테이너 #FEE500, 심볼+레이블, 레이블 #000000 85% -->
        <a th:href="@{/oauth2/authorization/kakao}" class="kakao-login-btn">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                <path fill="#000000" fill-opacity="0.9"
                      d="M12 3C6.48 3 2 6.52 2 10.86c0 2.77 1.82 5.2 4.56 6.6l-.93 3.45c-.08.3.26.55.52.38l4.1-2.72c.57.08 1.15.12 1.75.12 5.52 0 10-3.52 10-7.83C22 6.52 17.52 3 12 3z"/>
            </svg>
            <span th:text="#{login.kakao.button}">카카오 로그인</span>
        </a>
    </section>
</main>

<style>
    .login-page { display: flex; justify-content: center; padding: 4rem 1rem; }
    .login-card { max-width: 360px; width: 100%; text-align: center; }
    .login-subtitle { color: #666; margin: 0.75rem 0 2rem; }
    .login-error { color: #d32f2f; margin-bottom: 1rem; }
    .kakao-login-btn {
        display: inline-flex; align-items: center; justify-content: center; gap: 8px;
        width: 100%; padding: 14px 16px; border-radius: 12px;
        background: #FEE500; color: rgba(0, 0, 0, 0.85);
        font-weight: 600; text-decoration: none;
    }
</style>

<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>
</body>
</html>
```

- [ ] **Step 7: 헤더 두 fragment 에 로그인 UI 추가**

`fragments/header.html` 의 `<html ...>` 태그에 네임스페이스 추가:

```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
      th:lang="${#locale.language}">
```

**`header` fragment**: `<!-- Language Toggle -->` 주석 바로 위에 삽입:

```html
                    <!-- Auth (카카오 로그인 / 프로필) -->
                    <a sec:authorize="!hasRole('USER')" th:href="@{/oauth2/authorization/kakao}"
                       class="nav-link nav-kakao-login" th:text="#{nav.login.kakao}">카카오 로그인</a>
                    <th:block sec:authorize="hasRole('USER')">
                        <a th:href="@{/me}" class="nav-link nav-profile">
                            <img th:if="${#authentication.principal.attributes['appProfileImage'] != null}"
                                 th:src="${#authentication.principal.attributes['appProfileImage']}"
                                 class="nav-profile-img" alt="" width="24" height="24"/>
                            <span th:text="${#authentication.principal.attributes['appNickname']}">닉네임</span>
                        </a>
                        <a th:href="@{/recap}" class="nav-link" th:text="#{nav.recap}">나의 리캡</a>
                        <form th:action="@{/logout}" method="post" class="nav-logout-form">
                            <button type="submit" class="nav-link nav-logout-btn" th:text="#{nav.logout}">로그아웃</button>
                        </form>
                    </th:block>
```

**`header-minimal` fragment**: 같은 위치(`<!-- Language Toggle -->` 위)에 동일 블록을 삽입하되 `nav-link` 를 `nav-link-animated` 로 바꾼다 (4곳: 로그인 a, 프로필 a, 리캡 a, 로그아웃 button).

스타일: `nav-logout-form { display:inline; margin:0; }`, `nav-logout-btn { background:none; border:none; cursor:pointer; font:inherit; }`, `nav-profile-img { border-radius:50%; vertical-align:middle; margin-right:4px; }`, `nav-kakao-login { background:#FEE500; border-radius:8px; padding:6px 12px; color:rgba(0,0,0,0.85); }` — 메인 CSS 파일(`src/main/resources/static/css/` 의 nav 스타일이 있는 파일, 구현 시 `grep -rn "nav-link" src/main/resources/static/css/` 로 확인) 끝에 추가.

**주의:** `#authentication.principal.attributes` 는 어드민(폼로그인, `User` principal) 세션에서 존재하지 않는다 — 반드시 `sec:authorize="hasRole('USER')"` 블록 안에서만 접근한다 (위 구조 준수). 로그아웃 form 은 `th:action` 이므로 CSRF hidden input 이 자동 삽입된다.

- [ ] **Step 8: 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*AuthControllerTest\" --tests \"*ReservedOwnerIdsTest\" --tests \"*PolicyPagesLocaleRenderingTest\" --tests \"*UseCaseLocaleRenderingTest\""`
Expected: 모두 PASS (기존 렌더링 테스트로 헤더 fragment 변경 회귀 확인).

- [ ] **Step 9: git_commit.md 에 커밋 섹션 추가**

```
# Commit 75 — feat(datedate): 로그인 페이지·헤더 카카오 로그인 UI·i18n
git add src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthController.java src/main/resources/templates/auth/login.html src/main/resources/templates/fragments/header.html src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIds.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthControllerTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIdsTest.java docs/git_commit.md
git commit -m "feat(datedate): /login 페이지 + 헤더 카카오 로그인/프로필 UI + 예약어(me·recap·oauth2)" -m "카카오 버튼 공식 디자인(#FEE500), sec:authorize 로 ROLE_USER 분기(어드민 세션 미노출), POST /logout CSRF 폼. SEO noindex. ko/en 메시지 키."
```

---

### Task 5: Owner–User 연결 (자동 + 수동, first-claim)

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/owner/Owner.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/owner/OwnerRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/exception/OwnerAlreadyLinkedException.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/service/OwnerService.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/OwnerJpaEntity.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/OwnerJpaRepository.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/OwnerRepositoryAdapter.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiController.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeController.java` (POST /start 자동 연결)
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/OwnerController.java` (연결 버튼 모델)
- Modify: `src/main/resources/templates/owner/dashboard.html` (연결 버튼)
- Modify: `src/main/resources/messages.properties`, `messages_en.properties` (owner.link.* 키)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/domain/owner/OwnerTest.java` (Modify)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/application/service/OwnerServiceTest.java` (Modify)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiControllerTest.java` (Create)

**Interfaces:**
- Consumes: Task 3 의 `AuthenticatedUsers.currentUserId(Authentication): Optional<Long>`
- Produces:
  - `Owner` — `getUserId(): Long`(nullable), `linkUser(Long userId): void`(타 유저 연결 시 `OwnerAlreadyLinkedException`), `isLinkedTo(Long userId): boolean`, 새 4-arg 생성자 `Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules, Long userId)`
  - `OwnerRepository` — `findAllByUserId(Long userId): List<Owner>` 추가
  - `OwnerService` — `getOrCreateOwner(String ownerId, Long userId): Owner`(오버로드, 미연결이면 자동 연결), `linkOwnerToUser(String ownerId, Long userId): Owner`, `getOwnersOf(Long userId): List<Owner>`
  - `POST /api/me/owners/{ownerId}` — 200 `{"ownerId":..,"userId":..}` / 404 / 409

- [ ] **Step 1: 실패하는 도메인·서비스 테스트 작성**

`OwnerTest.java` 에 추가:

```java
    @Test
    @DisplayName("미연결 오너는 유저에 연결할 수 있고 멱등이다")
    void linkUserIsIdempotentForSameUser() {
        Owner owner = new Owner("my-crew");

        owner.linkUser(42L);
        owner.linkUser(42L);

        assertThat(owner.getUserId()).isEqualTo(42L);
        assertThat(owner.isLinkedTo(42L)).isTrue();
    }

    @Test
    @DisplayName("이미 다른 유저에 연결된 오너 연결 시도는 409 예외")
    void linkUserRejectsDifferentUser() {
        Owner owner = new Owner("my-crew");
        owner.linkUser(42L);

        assertThatThrownBy(() -> owner.linkUser(43L))
                .isInstanceOf(OwnerAlreadyLinkedException.class);
    }
```

(import 추가: `me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException`, `assertThatThrownBy`)

`OwnerServiceTest.java` 에 추가:

```java
    @Test
    @DisplayName("로그인 상태 getOrCreateOwner 는 미연결 오너를 현재 유저에 자동 연결한다")
    void getOrCreateOwnerLinksUserWhenUnlinked() {
        when(ownerRepository.findById("my-crew")).thenReturn(Optional.empty());
        when(ownerRepository.save(any(Owner.class))).thenAnswer(inv -> inv.getArgument(0));

        Owner owner = ownerService.getOrCreateOwner("my-crew", 42L);

        assertThat(owner.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("비로그인(userId null) getOrCreateOwner 는 연결하지 않는다")
    void getOrCreateOwnerWithoutUserKeepsUnlinked() {
        when(ownerRepository.findById("my-crew")).thenReturn(Optional.empty());
        when(ownerRepository.save(any(Owner.class))).thenAnswer(inv -> inv.getArgument(0));

        Owner owner = ownerService.getOrCreateOwner("my-crew", null);

        assertThat(owner.getUserId()).isNull();
    }

    @Test
    @DisplayName("linkOwnerToUser: 미존재 오너는 OwnerNotFoundException")
    void linkOwnerToUserThrowsWhenOwnerMissing() {
        when(ownerRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerService.linkOwnerToUser("ghost", 42L))
                .isInstanceOf(OwnerNotFoundException.class);
    }

    @Test
    @DisplayName("linkOwnerToUser: 타 유저 선점 오너는 OwnerAlreadyLinkedException")
    void linkOwnerToUserThrowsWhenClaimedByOther() {
        Owner claimed = new Owner("my-crew", LocalDateTime.now(), List.of(), 7L);
        when(ownerRepository.findById("my-crew")).thenReturn(Optional.of(claimed));

        assertThatThrownBy(() -> ownerService.linkOwnerToUser("my-crew", 42L))
                .isInstanceOf(OwnerAlreadyLinkedException.class);
    }
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*OwnerTest\" --tests \"*OwnerServiceTest\""`
Expected: 컴파일 실패 — `linkUser`, `OwnerAlreadyLinkedException` 등 없음.

- [ ] **Step 3: 도메인·예외·서비스 구현**

`OwnerAlreadyLinkedException.java`:

```java
package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class OwnerAlreadyLinkedException extends BusinessException {

    public OwnerAlreadyLinkedException(String ownerId) {
        super("OWNER_ALREADY_LINKED",
                "Owner is already linked to another user: " + ownerId,
                HttpStatus.CONFLICT);
    }
}
```

`Owner.java` 수정 — 필드·생성자·메서드 (기존 검증 로직 유지):

```java
    private final String ownerId;
    private final LocalDateTime createdAt;
    private final List<Schedule> schedules;
    private Long userId;

    public Owner(String ownerId) {
        this(ownerId, LocalDateTime.now(), new ArrayList<>(), null);
    }

    public Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules) {
        this(ownerId, createdAt, schedules, null);
    }

    /** first-claim 연결 (ADR datedate/domain/0005): 소유 증명 수단이 없어 선점 정책. */
    public Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules, Long userId) {
        validateOwnerId(ownerId);
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.schedules = new ArrayList<>(schedules);
        this.userId = userId;
    }

    public void linkUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new OwnerAlreadyLinkedException(ownerId);
        }
        this.userId = userId;
    }

    public boolean isLinkedTo(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public Long getUserId() {
        return userId;
    }
```

(import 추가: `me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException` — `Schedule` 이 application 예외를 import 하는 기존 선례를 따른다.)

`OwnerRepository.java` 에 추가:

```java
    List<Owner> findAllByUserId(Long userId);
```

(import `java.util.List`)

`OwnerService.java` 에 추가:

```java
    @Transactional
    public Owner getOrCreateOwner(String ownerId, Long userId) {
        Owner owner = getOrCreateOwner(ownerId);
        if (userId != null && owner.getUserId() == null) {
            owner.linkUser(userId);
            owner = ownerRepository.save(owner);
        }
        return owner;
    }

    @Transactional
    public Owner linkOwnerToUser(String ownerId, Long userId) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new OwnerNotFoundException(ownerId));
        owner.linkUser(userId);
        return ownerRepository.save(owner);
    }

    public List<Owner> getOwnersOf(Long userId) {
        return ownerRepository.findAllByUserId(userId);
    }
```

(import `me.singingsandhill.calendar.datedate.application.exception.OwnerNotFoundException`)

- [ ] **Step 4: 영속성 구현 — orphanRemoval 보호 필수**

`OwnerJpaEntity.java`: 필드·생성자·접근자 추가:

```java
    /** 카카오 사용자 연결 (nullable, first-claim). AppUserJpaEntity.id 참조. */
    @Column(name = "user_id")
    private Long userId;
```

기존 2-arg 생성자는 유지하고 3-arg 추가 + setter/getter:

```java
    public OwnerJpaEntity(String ownerId, LocalDateTime createdAt, Long userId) {
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
```

`OwnerJpaRepository.java` 에 추가:

```java
    List<OwnerJpaEntity> findAllByUserId(Long userId);
```

`OwnerRepositoryAdapter.java` 수정:

**중요:** `schedules` 는 `cascade = ALL, orphanRemoval = true` 다. 기존 오너를 `toEntity()` 로 새로 만들어 save 하면 빈 schedules 컬렉션으로 병합되어 **일정이 전부 삭제될 수 있다**. 기존 오너는 반드시 영속 엔티티를 로드해 필드만 갱신한다:

```java
    @Override
    public Owner save(Owner owner) {
        OwnerJpaEntity entity = jpaRepository.findById(owner.getOwnerId())
                .map(existing -> {
                    existing.setUserId(owner.getUserId());
                    return existing;
                })
                .orElseGet(() -> new OwnerJpaEntity(
                        owner.getOwnerId(), owner.getCreatedAt(), owner.getUserId()));
        OwnerJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Owner> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
```

`toDomain` 은 4-arg 생성자로 교체:

```java
    private Owner toDomain(OwnerJpaEntity entity) {
        return new Owner(
                entity.getOwnerId(),
                entity.getCreatedAt(),
                entity.getSchedules().stream()
                        .map(this::scheduleToDomain)
                        .collect(Collectors.toList()),
                entity.getUserId()
        );
    }
```

(기존 `toEntity(Owner)` 메서드는 삭제, import `java.util.List` 추가)

- [ ] **Step 5: 도메인·서비스 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*OwnerTest\" --tests \"*OwnerServiceTest\""`
Expected: PASS.

- [ ] **Step 6: MeApiController 실패 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiControllerTest.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(MeApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class MeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kakaoUser(long userId) {
        return oauth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, userId));
    }

    @Test
    @DisplayName("로그인 사용자는 미연결 오너를 자기 계정에 연결할 수 있다")
    void linkOwnerSucceeds() throws Exception {
        Owner linked = new Owner("my-crew", LocalDateTime.now(), List.of(), 42L);
        when(ownerService.linkOwnerToUser(eq("my-crew"), eq(42L))).thenReturn(linked);

        mockMvc.perform(post("/api/me/owners/my-crew").with(kakaoUser(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("my-crew"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("타 유저 선점 오너 연결은 409 + OWNER_ALREADY_LINKED")
    void linkOwnerConflicts() throws Exception {
        when(ownerService.linkOwnerToUser(eq("my-crew"), eq(42L)))
                .thenThrow(new OwnerAlreadyLinkedException("my-crew"));

        mockMvc.perform(post("/api/me/owners/my-crew").with(kakaoUser(42L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("미인증 연결 요청은 로그인으로 리다이렉트된다")
    void anonymousLinkIsRedirected() throws Exception {
        mockMvc.perform(post("/api/me/owners/my-crew"))
                .andExpect(status().is3xxRedirection());
    }
}
```

- [ ] **Step 7: 실패 확인 후 컨트롤러·템플릿 구현**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*MeApiControllerTest\""` → 컴파일 실패 확인.

`src/main/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiController.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@RestController
@RequestMapping("/api/me")
public class MeApiController {

    public record LinkedOwnerResponse(String ownerId, Long userId) {

        public static LinkedOwnerResponse from(Owner owner) {
            return new LinkedOwnerResponse(owner.getOwnerId(), owner.getUserId());
        }
    }

    private final OwnerService ownerService;

    public MeApiController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping("/owners/{ownerId}")
    public ResponseEntity<LinkedOwnerResponse> linkOwner(@PathVariable String ownerId,
                                                         Authentication authentication) {
        Long userId = AuthenticatedUsers.currentUserId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("authenticated kakao user required"));
        Owner owner = ownerService.linkOwnerToUser(ownerId, userId);
        return ResponseEntity.ok(LinkedOwnerResponse.from(owner));
    }
}
```

`HomeController.java` 의 `start(...)` 수정 — 시그니처에 `Authentication authentication` 추가, 서비스 호출 교체:

```java
    @PostMapping("/start")
    public String start(@RequestParam String ownerId,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            String normalizedId = ownerId.toLowerCase();
            Long userId = AuthenticatedUsers.currentUserId(authentication).orElse(null);
            ownerService.getOrCreateOwner(normalizedId, userId);
            return localeLinks.redirect("/" + normalizedId);
```

(catch 블록·나머지는 그대로. import 추가: `org.springframework.security.core.Authentication`, `me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers`)

`OwnerController.java` 의 `dashboard(...)` 수정 — 시그니처에 `Authentication authentication` 추가, `schedules` 조회 앞에 삽입:

```java
        Optional<Long> currentUserId = AuthenticatedUsers.currentUserId(authentication);
        Owner owner = ownerService.getOwner(ownerId);
        boolean linkedToMe = owner != null && currentUserId.isPresent()
                && owner.isLinkedTo(currentUserId.get());
        boolean canLink = owner != null && currentUserId.isPresent() && owner.getUserId() == null;
        model.addAttribute("linkedToMe", linkedToMe);
        model.addAttribute("canLink", canLink);
```

(import 추가: `java.util.Optional`, `org.springframework.security.core.Authentication`, `me.singingsandhill.calendar.datedate.domain.owner.Owner`, `...presentation.support.AuthenticatedUsers`)

`owner/dashboard.html` — 대시보드 헤더 액션 영역("이번 달"/"+ 일정 만들기" 버튼 블록) 바로 아래에 삽입:

```html
        <!-- 카카오 계정 연결 (first-claim: 본인이 만든 페이지만 연결하도록 안내) -->
        <div class="owner-link-banner" th:if="${canLink}">
            <span th:text="#{owner.link.prompt}">이 페이지를 내 카카오 계정에 연결할 수 있어요. 본인이 만든 페이지만 연결하세요.</span>
            <button type="button" id="linkOwnerBtn" class="btn-link-owner"
                    th:attr="data-owner-id=${ownerId}" th:text="#{owner.link.button}">내 계정에 연결</button>
        </div>
        <div class="owner-link-banner owner-link-done" th:if="${linkedToMe}">
            <span th:text="#{owner.link.done}">내 카카오 계정에 연결된 페이지예요.</span>
        </div>
```

같은 파일 `</body>` 직전 스크립트 블록에 추가:

```html
<script>
    (function () {
        var btn = document.getElementById('linkOwnerBtn');
        if (!btn) return;
        btn.addEventListener('click', function () {
            fetch('/api/me/owners/' + btn.dataset.ownerId, { method: 'POST' })
                .then(function (res) {
                    if (res.ok) { location.reload(); return; }
                    return res.json().then(function (body) { alert(body.message); });
                });
        });
    })();
</script>
```

i18n 키 (Task 4 Step 5 와 동일한 방식으로 ko 는 유니코드 이스케이프 변환):

- ko: `owner.link.prompt=이 페이지를 내 카카오 계정에 연결할 수 있어요. 본인이 만든 페이지만 연결하세요.` / `owner.link.button=내 계정에 연결` / `owner.link.done=내 카카오 계정에 연결된 페이지예요.`
- en: `owner.link.prompt=You can link this page to your Kakao account. Only link pages you created.` / `owner.link.button=Link to my account` / `owner.link.done=This page is linked to your Kakao account.`

- [ ] **Step 8: 테스트 통과 + 회귀 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*MeApiControllerTest\" --tests \"*OwnerTest\" --tests \"*OwnerServiceTest\" --tests \"*OwnerDashboard404IntegrationTest\""`
Expected: 모두 PASS (`OwnerDashboard404IntegrationTest` 로 대시보드 변경 회귀 확인).

- [ ] **Step 9: git_commit.md 에 커밋 섹션 추가**

```
# Commit 76 — feat(datedate): 오너-카카오 계정 연결 (자동/수동, first-claim)
git add src/main/java/me/singingsandhill/calendar/datedate/domain/owner/ src/main/java/me/singingsandhill/calendar/datedate/application/exception/OwnerAlreadyLinkedException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/OwnerService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/OwnerJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/OwnerJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/OwnerRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/OwnerController.java src/main/resources/templates/owner/dashboard.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/test/java/me/singingsandhill/calendar/datedate/domain/owner/OwnerTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/OwnerServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiControllerTest.java docs/git_commit.md
git commit -m "feat(datedate): 오너-카카오 계정 연결 — POST /start 자동 + 대시보드 수동 버튼" -m "Owner.userId nullable(first-claim, 타 유저 선점 409). 어댑터 save 는 기존 엔티티 로드 후 갱신(orphanRemoval 로 인한 일정 삭제 방지). findAllByUserId 포트 추가."
```

---

### Task 6: UserActivity 활동 이벤트 — 도메인 + 기록 훅

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/activity/ActivityType.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/activity/UserActivity.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/activity/UserActivityRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/service/UserActivityService.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/UserActivityJpaEntity.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/UserActivityJpaRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/UserActivityRepositoryAdapter.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ParticipantApiController.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/api/LocationApiController.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/api/MenuApiController.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ScheduleApiController.java`
- Test: `src/test/java/me/singingsandhill/calendar/datedate/application/service/UserActivityServiceTest.java` (Create)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ParticipantApiActivityRecordingTest.java` (Create)

**Interfaces:**
- Consumes: Task 3 의 `AuthenticatedUsers.currentUserId(Authentication)`
- Produces:
  - `ActivityType` enum — `SCHEDULE_CREATED, PARTICIPATION, LOCATION_VOTE, MENU_VOTE`
  - `UserActivity` — `(Long id, Long userId, ActivityType type, Long scheduleId, Long targetId, String detail, LocalDateTime occurredAt)` + getters
  - `UserActivityRepository` — `save(UserActivity): UserActivity`, `existsByUserIdAndTypeAndTargetId(Long, ActivityType, Long): boolean`, `findAllByUserIdAndOccurredAtBetween(Long, LocalDateTime, LocalDateTime): List<UserActivity>`
  - `UserActivityService.record(Long userId, ActivityType type, Long scheduleId, Long targetId, String detail): void` — userId null 이면 no-op, `(userId,type,targetId)` 중복이면 skip, 저장 실패는 WARN 로그만 (본 동작 미차단)

- [ ] **Step 1: 실패하는 서비스 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/application/service/UserActivityServiceTest.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private UserActivityRepository repository;

    private UserActivityService service;

    @BeforeEach
    void setUp() {
        service = new UserActivityService(repository, FIXED);
    }

    @Test
    @DisplayName("로그인 사용자의 활동은 1행 기록된다")
    void recordsActivityForLoggedInUser() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.LOCATION_VOTE, 5L))
                .thenReturn(false);

        service.record(42L, ActivityType.LOCATION_VOTE, 3L, 5L, "성수 카페");

        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(repository).save(captor.capture());
        UserActivity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getType()).isEqualTo(ActivityType.LOCATION_VOTE);
        assertThat(saved.getScheduleId()).isEqualTo(3L);
        assertThat(saved.getTargetId()).isEqualTo(5L);
        assertThat(saved.getDetail()).isEqualTo("성수 카페");
        assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("userId 가 null(비로그인)이면 아무것도 하지 않는다")
    void noOpForAnonymous() {
        service.record(null, ActivityType.PARTICIPATION, 3L, 5L, "지수");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("(userId, type, targetId) 중복 이벤트는 다시 기록하지 않는다")
    void skipsDuplicateByUserTypeTarget() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.PARTICIPATION, 5L))
                .thenReturn(true);

        service.record(42L, ActivityType.PARTICIPATION, 3L, 5L, "지수");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("저장 실패는 예외를 삼키고 본 동작을 막지 않는다")
    void swallowsPersistenceFailure() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.MENU_VOTE, 5L))
                .thenReturn(false);
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.record(42L, ActivityType.MENU_VOTE, 3L, 5L, "마라탕"))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*UserActivityServiceTest\""`
Expected: 컴파일 실패.

- [ ] **Step 3: 도메인·서비스·영속성 구현**

`ActivityType.java`:

```java
package me.singingsandhill.calendar.datedate.domain.activity;

public enum ActivityType {
    SCHEDULE_CREATED,
    PARTICIPATION,
    LOCATION_VOTE,
    MENU_VOTE
}
```

`UserActivity.java`:

```java
package me.singingsandhill.calendar.datedate.domain.activity;

import java.time.LocalDateTime;

/**
 * 로그인 사용자의 활동 이벤트 (append-only, ADR datedate/domain/0005).
 * 기존 익명 데이터 구조(voters 문자열 등)를 건드리지 않기 위한 별도 기록.
 * targetId: PARTICIPATION=participantId, *_VOTE=location/menu id, SCHEDULE_CREATED=scheduleId.
 */
public class UserActivity {

    private final Long id;
    private final Long userId;
    private final ActivityType type;
    private final Long scheduleId;
    private final Long targetId;
    private final String detail;
    private final LocalDateTime occurredAt;

    public UserActivity(Long id, Long userId, ActivityType type, Long scheduleId,
                        Long targetId, String detail, LocalDateTime occurredAt) {
        if (userId == null || type == null) {
            throw new IllegalArgumentException("userId and type are required");
        }
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.scheduleId = scheduleId;
        this.targetId = targetId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public ActivityType getType() {
        return type;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
```

`UserActivityRepository.java`:

```java
package me.singingsandhill.calendar.datedate.domain.activity;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository {

    UserActivity save(UserActivity activity);

    boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId);

    List<UserActivity> findAllByUserIdAndOccurredAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
```

`UserActivityService.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;

/**
 * 로그인 사용자 활동 이벤트 기록. 컨트롤러가 본 동작 성공 후 호출한다.
 * 기록 실패가 참여/투표 자체를 실패시키면 안 되므로 REQUIRES_NEW + 예외 삼킴.
 */
@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserActivityRepository repository;
    private final Clock clock;

    public UserActivityService(UserActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, ActivityType type, Long scheduleId, Long targetId, String detail) {
        if (userId == null) {
            return;
        }
        try {
            if (targetId != null && repository.existsByUserIdAndTypeAndTargetId(userId, type, targetId)) {
                return;
            }
            repository.save(new UserActivity(null, userId, type, scheduleId, targetId, detail,
                    LocalDateTime.now(clock)));
        } catch (Exception e) {
            log.warn("user activity record failed: userId={}, type={}, targetId={}", userId, type, targetId, e);
        }
    }
}
```

`UserActivityJpaEntity.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;

@Entity
@Table(name = "user_activities",
        indexes = @Index(name = "idx_user_activities_user_occurred", columnList = "userId, occurredAt"))
public class UserActivityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    private Long scheduleId;

    private Long targetId;

    @Column(length = 200)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected UserActivityJpaEntity() {
    }

    public UserActivityJpaEntity(Long id, Long userId, ActivityType type, Long scheduleId,
                                 Long targetId, String detail, LocalDateTime occurredAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.scheduleId = scheduleId;
        this.targetId = targetId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public ActivityType getType() {
        return type;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
```

`UserActivityJpaRepository.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.UserActivityJpaEntity;

public interface UserActivityJpaRepository extends JpaRepository<UserActivityJpaEntity, Long> {

    boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId);

    List<UserActivityJpaEntity> findAllByUserIdAndOccurredAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to);
}
```

`UserActivityRepositoryAdapter.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.UserActivityJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.UserActivityJpaRepository;

@Repository
public class UserActivityRepositoryAdapter implements UserActivityRepository {

    private final UserActivityJpaRepository jpaRepository;

    public UserActivityRepositoryAdapter(UserActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserActivity save(UserActivity activity) {
        UserActivityJpaEntity saved = jpaRepository.save(new UserActivityJpaEntity(
                activity.getId(), activity.getUserId(), activity.getType(),
                activity.getScheduleId(), activity.getTargetId(), activity.getDetail(),
                activity.getOccurredAt()));
        return toDomain(saved);
    }

    @Override
    public boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId) {
        return jpaRepository.existsByUserIdAndTypeAndTargetId(userId, type, targetId);
    }

    @Override
    public List<UserActivity> findAllByUserIdAndOccurredAtBetween(Long userId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findAllByUserIdAndOccurredAtBetween(userId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private UserActivity toDomain(UserActivityJpaEntity entity) {
        return new UserActivity(entity.getId(), entity.getUserId(), entity.getType(),
                entity.getScheduleId(), entity.getTargetId(), entity.getDetail(), entity.getOccurredAt());
    }
}
```

- [ ] **Step 4: 서비스 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*UserActivityServiceTest\""`
Expected: PASS (4 tests).

- [ ] **Step 5: 컨트롤러 기록 훅 실패 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ParticipantApiActivityRecordingTest.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.UserActivityService;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * ADR datedate/domain/0005: 활동 이벤트는 로그인 세션에서만 기록되고,
 * 익명 요청 경로는 완전히 무변경이어야 한다.
 */
@WebMvcTest(ParticipantApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class ParticipantApiActivityRecordingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipantService participantService;

    @MockitoBean
    private UserActivityService userActivityService;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    private Participant participant() {
        return new Participant(5L, 3L, "지수", new ParticipantColor(0), List.of(1, 2), LocalDateTime.now());
    }

    @Test
    @DisplayName("카카오 로그인 사용자의 참여자 추가는 PARTICIPATION 이벤트를 기록한다")
    void recordsParticipationForKakaoUser() throws Exception {
        when(participantService.addParticipant(eq(3L), eq("지수"))).thenReturn(participant());

        mockMvc.perform(post("/api/schedules/3/participants")
                        .with(oauth2Login()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"지수\"}"))
                .andExpect(status().isCreated());

        verify(userActivityService).record(42L, ActivityType.PARTICIPATION, 3L, 5L, "지수");
    }

    @Test
    @DisplayName("익명 참여자 추가는 이벤트를 기록하지 않는다 (기존 플로우 무변경)")
    void doesNotRecordForAnonymous() throws Exception {
        when(participantService.addParticipant(eq(3L), eq("지수"))).thenReturn(participant());

        mockMvc.perform(post("/api/schedules/3/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"지수\"}"))
                .andExpect(status().isCreated());

        verify(userActivityService, never()).record(anyLong(), any(), anyLong(), anyLong(), anyString());
    }
}
```

**주의:** `ParticipantColor` 생성자가 `new ParticipantColor(int)` 가 아니면 (구현 시 `ParticipantColor.java` 확인) 기존 `ParticipantColorTest` 의 생성 방식을 따른다.

- [ ] **Step 6: 실패 확인 후 컨트롤러 4개에 훅 추가**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*ParticipantApiActivityRecordingTest\""` → 컴파일 실패 확인.

네 컨트롤러 공통 패턴: 생성자에 `UserActivityService userActivityService` 주입 추가, 대상 메서드 시그니처에 `Authentication authentication` 파라미터 추가, 본 동작 성공 **후** 기록. import 공통 추가:

```java
import org.springframework.security.core.Authentication;
import me.singingsandhill.calendar.datedate.application.service.UserActivityService;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;
```

`ParticipantApiController` — `addParticipant`:

```java
    @PostMapping("/schedules/{scheduleId}/participants")
    public ResponseEntity<ParticipantResponse> addParticipant(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ParticipantCreateRequest request,
            Authentication authentication) {
        Participant participant = participantService.addParticipant(scheduleId, request.name());
        AuthenticatedUsers.currentUserId(authentication).ifPresent(userId ->
                userActivityService.record(userId, ActivityType.PARTICIPATION,
                        scheduleId, participant.getId(), participant.getName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ParticipantResponse.from(participant));
    }
```

`ParticipantApiController` — `updateSelections` (추가+날짜선택 이중 기록은 `(userId,type,targetId)` 중복 방지로 차단됨):

```java
    @PatchMapping("/participants/{participantId}/selections")
    public ResponseEntity<ParticipantResponse> updateSelections(
            @PathVariable Long participantId,
            @Valid @RequestBody SelectionUpdateRequest request,
            Authentication authentication) {
        Participant participant = participantService.updateSelections(participantId, request.selections());
        AuthenticatedUsers.currentUserId(authentication).ifPresent(userId ->
                userActivityService.record(userId, ActivityType.PARTICIPATION,
                        participant.getScheduleId(), participantId, participant.getName()));
        return ResponseEntity.ok(ParticipantResponse.from(participant));
    }
```

`LocationApiController` — `vote`:

```java
    @PostMapping("/locations/{locationId}/votes")
    public ResponseEntity<LocationResponse> vote(
            @PathVariable Long locationId,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        Location location = locationService.vote(locationId, request.voterName());
        AuthenticatedUsers.currentUserId(authentication).ifPresent(userId ->
                userActivityService.record(userId, ActivityType.LOCATION_VOTE,
                        location.getScheduleId(), locationId, location.getName()));
        return ResponseEntity.ok(LocationResponse.from(location));
    }
```

`MenuApiController` — `vote` (동일 패턴, `ActivityType.MENU_VOTE`, `menu.getScheduleId()`, `menuId`, `menu.getName()`).

`ScheduleApiController` — `createSchedule`:

```java
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @PathVariable String ownerId,
            @Valid @RequestBody ScheduleCreateRequest request,
            Authentication authentication) {
        Schedule schedule = scheduleService.createSchedule(
                ownerId,
                request.year(),
                request.month(),
                request.weeks()
        );
        AuthenticatedUsers.currentUserId(authentication).ifPresent(userId ->
                userActivityService.record(userId, ActivityType.SCHEDULE_CREATED,
                        schedule.getId(), schedule.getId(), ownerId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(schedule));
    }
```

**기존 컨트롤러 단위 테스트 보수:** `ScheduleApiControllerTest` 등 위 4개 컨트롤러의 기존 `@WebMvcTest` 에 `@MockitoBean UserActivityService userActivityService;` 필드를 추가한다 (생성자 주입 추가로 컨텍스트 로딩 실패 방지).

- [ ] **Step 7: 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*ParticipantApiActivityRecordingTest\" --tests \"*UserActivityServiceTest\" --tests \"*ScheduleApiControllerTest\""`
Expected: 모두 PASS.

- [ ] **Step 8: git_commit.md 에 커밋 섹션 추가**

```
# Commit 77 — feat(datedate): UserActivity 활동 이벤트 기록 (로그인 세션 한정)
git add src/main/java/me/singingsandhill/calendar/datedate/domain/activity/ src/main/java/me/singingsandhill/calendar/datedate/application/service/UserActivityService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/UserActivityJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/UserActivityJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/UserActivityRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ src/test/java/me/singingsandhill/calendar/datedate/application/service/UserActivityServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ParticipantApiActivityRecordingTest.java docs/git_commit.md
git commit -m "feat(datedate): 로그인 사용자 활동 이벤트(참여·투표·일정생성) append-only 기록" -m "(userId,type,targetId) 중복 방지, REQUIRES_NEW+예외 삼킴으로 본 동작 무영향, 익명 경로 무변경. recap 집계 원천 (ADR datedate/domain/0005)."
```

---

### Task 7: 연간 Recap — 집계 서비스 + 공유 토큰 + 페이지 + 마이페이지

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/recap/RecapShare.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/recap/RecapShareRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/dto/RecapDto.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/exception/InvalidRecapYearException.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/exception/RecapShareNotFoundException.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapService.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapShareService.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/RecapShareJpaEntity.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/RecapShareJpaRepository.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/RecapShareRepositoryAdapter.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapController.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/MyPageController.java`
- Create: `src/main/resources/templates/recap/recap.html`, `recap/share.html`, `me/mypage.html`
- Modify: `SeoService.java` (`getMyPageSeo()`, `getRecapSeo(int)`, `getRecapShareSeo(String, int)`)
- Modify: `messages.properties`, `messages_en.properties`
- Test: `src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java` (Create)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapControllerTest.java` (Create)

**Interfaces:**
- Consumes: Task 2 `AppUserRepository`, Task 5 `OwnerRepository.findAllByUserId` / `OwnerService.getOwnersOf`, Task 6 `UserActivityRepository.findAllByUserIdAndOccurredAtBetween`, 기존 `ScheduleRepository.findAllByOwnerId`, `ParticipantRepository.findById`, `YearMonth.indexToDate(int)`
- Produces:
  - `RecapDto(int year, String nickname, int schedulesCreated, int totalParticipants, int participationCount, int daysSelected, String topWeekday, Integer busiestMonth, List<String> topLocations, List<String> topMenus, List<String> topCompanions, boolean empty)` — `topWeekday` 는 `DayOfWeek.name()` 문자열(예: "MONDAY", 없으면 null)
  - `RecapService.buildRecap(Long userId, int year): RecapDto` — 연도 범위 밖이면 `InvalidRecapYearException`(400)
  - `RecapShareService.getOrCreateShare(Long userId, int year): RecapShare`(멱등), `getByToken(String token): RecapShare`(없으면 `RecapShareNotFoundException` 404)
  - 라우트: `GET /recap`(→올해 redirect), `GET /recap/{year}`, `POST /recap/{year}/share`, `GET /recap/share/{token}`(공개), `GET /me`

- [ ] **Step 1: 실패하는 RecapService 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class RecapServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private ParticipantRepository participantRepository;

    private RecapService recapService;

    @BeforeEach
    void setUp() {
        recapService = new RecapService(appUserRepository, ownerRepository, scheduleRepository,
                userActivityRepository, participantRepository, FIXED);
    }

    private void stubUser() {
        when(appUserRepository.findById(42L)).thenReturn(Optional.of(
                new AppUser(42L, 12345L, "지수", null,
                        LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0))));
    }

    @Test
    @DisplayName("오너 계열: 해당 연도 일정 수·연인원·최다 요일·가장 바쁜 달·동행 TOP 을 집계한다")
    void aggregatesOwnerLine() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of(
                new Owner("my-crew", LocalDateTime.of(2026, 1, 1, 0, 0), List.of(), 42L)));

        // 2026-03: 그리드 시작 = 2026-03-01(일). index 2 = 3/2(월), index 9 = 3/9(월), index 3 = 3/3(화)
        Participant p1 = new Participant(1L, 10L, "민준", new ParticipantColor(0),
                List.of(2, 9), LocalDateTime.of(2026, 3, 1, 0, 0));
        Participant p2 = new Participant(2L, 10L, "서연", new ParticipantColor(1),
                List.of(2, 3), LocalDateTime.of(2026, 3, 1, 0, 0));
        Schedule mar2026 = new Schedule(10L, "my-crew", 2026, 3, 7,
                LocalDateTime.of(2026, 2, 20, 0, 0), List.of(p1, p2));
        Schedule dec2025 = new Schedule(11L, "my-crew", 2025, 12, 7,
                LocalDateTime.of(2025, 11, 20, 0, 0), List.of());
        when(scheduleRepository.findAllByOwnerId("my-crew")).thenReturn(List.of(mar2026, dec2025));
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.year()).isEqualTo(2026);
        assertThat(recap.nickname()).isEqualTo("지수");
        assertThat(recap.schedulesCreated()).isEqualTo(1);
        assertThat(recap.totalParticipants()).isEqualTo(2);
        assertThat(recap.topWeekday()).isEqualTo("MONDAY");
        assertThat(recap.busiestMonth()).isEqualTo(3);
        assertThat(recap.topCompanions()).containsExactly("민준", "서연");
        assertThat(recap.empty()).isFalse();
    }

    @Test
    @DisplayName("활동 계열: 참여 일정 수(distinct)·선택 날짜 합·투표 TOP3 을 집계한다")
    void aggregatesActivityLine() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of());
        LocalDateTime when1 = LocalDateTime.of(2026, 5, 1, 12, 0);
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of(
                        new UserActivity(1L, 42L, ActivityType.PARTICIPATION, 20L, 100L, "지수", when1),
                        new UserActivity(2L, 42L, ActivityType.PARTICIPATION, 20L, 101L, "지수", when1),
                        new UserActivity(3L, 42L, ActivityType.LOCATION_VOTE, 20L, 200L, "성수 카페", when1),
                        new UserActivity(4L, 42L, ActivityType.LOCATION_VOTE, 21L, 201L, "성수 카페", when1),
                        new UserActivity(5L, 42L, ActivityType.MENU_VOTE, 20L, 300L, "마라탕", when1)));
        when(participantRepository.findById(100L)).thenReturn(Optional.of(
                new Participant(100L, 20L, "지수", new ParticipantColor(0), List.of(1, 2, 3),
                        when1)));
        when(participantRepository.findById(101L)).thenReturn(Optional.empty());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.participationCount()).isEqualTo(1);
        assertThat(recap.daysSelected()).isEqualTo(3);
        assertThat(recap.topLocations()).containsExactly("성수 카페");
        assertThat(recap.topMenus()).containsExactly("마라탕");
        assertThat(recap.empty()).isFalse();
    }

    @Test
    @DisplayName("기록이 전혀 없으면 empty=true")
    void emptyRecapWhenNoData() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of());
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.empty()).isTrue();
        assertThat(recap.topWeekday()).isNull();
        assertThat(recap.busiestMonth()).isNull();
    }

    @Test
    @DisplayName("연도 범위(2024~현재) 밖이면 InvalidRecapYearException")
    void rejectsOutOfRangeYear() {
        assertThatThrownBy(() -> recapService.buildRecap(42L, 2023))
                .isInstanceOf(InvalidRecapYearException.class);
        assertThatThrownBy(() -> recapService.buildRecap(42L, 2027))
                .isInstanceOf(InvalidRecapYearException.class);
    }
}
```

**주의:** `ParticipantColor` 생성 방식은 Task 6 과 동일하게 실제 생성자를 확인해 맞춘다.

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*RecapServiceTest\""`
Expected: 컴파일 실패.

- [ ] **Step 3: DTO·예외·RecapService 구현**

`RecapDto.java`:

```java
package me.singingsandhill.calendar.datedate.application.dto;

import java.util.List;

/** 연간 recap 집계 결과. topWeekday 는 DayOfWeek.name() (없으면 null). */
public record RecapDto(
        int year,
        String nickname,
        int schedulesCreated,
        int totalParticipants,
        int participationCount,
        int daysSelected,
        String topWeekday,
        Integer busiestMonth,
        List<String> topLocations,
        List<String> topMenus,
        List<String> topCompanions,
        boolean empty
) {
}
```

`InvalidRecapYearException.java`:

```java
package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class InvalidRecapYearException extends BusinessException {

    public InvalidRecapYearException(int year) {
        super("INVALID_RECAP_YEAR",
                "Recap year out of range: " + year,
                HttpStatus.BAD_REQUEST);
    }
}
```

`RecapShareNotFoundException.java`:

```java
package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class RecapShareNotFoundException extends BusinessException {

    public RecapShareNotFoundException(String token) {
        super("RECAP_SHARE_NOT_FOUND",
                "Recap share not found: " + token,
                HttpStatus.NOT_FOUND);
    }
}
```

`RecapService.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.YearMonth;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

/**
 * 연간 recap on-the-fly 집계 (ADR datedate/domain/0005 — 스냅샷 없음).
 * 오너 계열: 내 오너들의 해당 연도 일정. 활동 계열: 내 UserActivity 이벤트.
 */
@Service
@Transactional(readOnly = true)
public class RecapService {

    private static final int MIN_YEAR = 2024;
    private static final int TOP_LIMIT = 3;

    private final AppUserRepository appUserRepository;
    private final OwnerRepository ownerRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserActivityRepository userActivityRepository;
    private final ParticipantRepository participantRepository;
    private final Clock clock;

    public RecapService(AppUserRepository appUserRepository,
                        OwnerRepository ownerRepository,
                        ScheduleRepository scheduleRepository,
                        UserActivityRepository userActivityRepository,
                        ParticipantRepository participantRepository,
                        Clock clock) {
        this.appUserRepository = appUserRepository;
        this.ownerRepository = ownerRepository;
        this.scheduleRepository = scheduleRepository;
        this.userActivityRepository = userActivityRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    public RecapDto buildRecap(Long userId, int year) {
        validateYear(year);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 오너 계열
        List<Schedule> mySchedules = ownerRepository.findAllByUserId(userId).stream()
                .map(Owner::getOwnerId)
                .flatMap(ownerId -> scheduleRepository.findAllByOwnerId(ownerId).stream())
                .filter(schedule -> schedule.getYear() == year)
                .toList();

        int schedulesCreated = mySchedules.size();
        int totalParticipants = mySchedules.stream().mapToInt(Schedule::getParticipantCount).sum();

        Map<DayOfWeek, Integer> weekdayFreq = new HashMap<>();
        Map<Integer, Integer> monthFreq = new HashMap<>();
        Map<String, Integer> companionFreq = new HashMap<>();
        for (Schedule schedule : mySchedules) {
            for (Participant participant : schedule.getParticipants()) {
                companionFreq.merge(participant.getName(), 1, Integer::sum);
                for (Integer index : participant.getSelections()) {
                    if (index == null || index < 1 || index > YearMonth.FIXED_TOTAL_DAYS) {
                        continue;
                    }
                    LocalDate date = schedule.getYearMonth().indexToDate(index);
                    weekdayFreq.merge(date.getDayOfWeek(), 1, Integer::sum);
                    monthFreq.merge(date.getMonthValue(), 1, Integer::sum);
                }
            }
        }

        // 활동 계열
        List<UserActivity> activities = userActivityRepository.findAllByUserIdAndOccurredAtBetween(
                userId,
                LocalDateTime.of(year, 1, 1, 0, 0),
                LocalDateTime.of(year, 12, 31, 23, 59, 59));

        List<UserActivity> participations = activities.stream()
                .filter(activity -> activity.getType() == ActivityType.PARTICIPATION)
                .toList();
        int participationCount = (int) participations.stream()
                .map(UserActivity::getScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int daysSelected = participations.stream()
                .map(UserActivity::getTargetId)
                .filter(Objects::nonNull)
                .map(participantRepository::findById)
                .flatMap(java.util.Optional::stream)
                .mapToInt(participant -> participant.getSelections().size())
                .sum();

        List<String> topLocations = topDetails(activities, ActivityType.LOCATION_VOTE);
        List<String> topMenus = topDetails(activities, ActivityType.MENU_VOTE);

        boolean empty = schedulesCreated == 0 && activities.isEmpty();

        return new RecapDto(
                year,
                user.getNickname(),
                schedulesCreated,
                totalParticipants,
                participationCount,
                daysSelected,
                maxKey(weekdayFreq).map(DayOfWeek::name).orElse(null),
                maxKey(monthFreq).orElse(null),
                topLocations,
                topMenus,
                topKeys(companionFreq),
                empty
        );
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > Year.now(clock).getValue()) {
            throw new InvalidRecapYearException(year);
        }
    }

    private List<String> topDetails(List<UserActivity> activities, ActivityType type) {
        Map<String, Integer> freq = new HashMap<>();
        activities.stream()
                .filter(activity -> activity.getType() == type)
                .map(UserActivity::getDetail)
                .filter(Objects::nonNull)
                .forEach(detail -> freq.merge(detail, 1, Integer::sum));
        return topKeys(freq);
    }

    private <K> java.util.Optional<K> maxKey(Map<K, Integer> freq) {
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private <K extends Comparable<K>> List<K> topKeys(Map<K, Integer> freq) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<K, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }
}
```

- [ ] **Step 4: RecapServiceTest 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*RecapServiceTest\""`
Expected: PASS (4 tests). 요일 검증 실패 시 2026-03 의 그리드 시작일(일요일) 계산을 `YearMonth.getGridStartDate()` 로 재확인.

- [ ] **Step 5: RecapShare 도메인·영속성·서비스 구현**

`src/main/java/me/singingsandhill/calendar/datedate/domain/recap/RecapShare.java`:

```java
package me.singingsandhill.calendar.datedate.domain.recap;

import java.time.LocalDateTime;

/** 연간 recap 공개 공유 토큰. (userId, year) 당 1개, 멱등 생성. */
public class RecapShare {

    private final Long id;
    private final Long userId;
    private final int year;
    private final String token;
    private final LocalDateTime createdAt;

    public RecapShare(Long id, Long userId, int year, String token, LocalDateTime createdAt) {
        if (userId == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("userId and token are required");
        }
        this.id = id;
        this.userId = userId;
        this.year = year;
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

`src/main/java/me/singingsandhill/calendar/datedate/domain/recap/RecapShareRepository.java`:

```java
package me.singingsandhill.calendar.datedate.domain.recap;

import java.util.Optional;

public interface RecapShareRepository {

    Optional<RecapShare> findByToken(String token);

    Optional<RecapShare> findByUserIdAndYear(Long userId, int year);

    RecapShare save(RecapShare share);
}
```

`RecapShareJpaEntity.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recap_shares", uniqueConstraints = {
        @UniqueConstraint(columnNames = "token"),
        @UniqueConstraint(columnNames = {"userId", "shareYear"})
})
public class RecapShareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 'year' 는 SQL 예약어 충돌 위험이 있어 shareYear 로 저장. */
    @Column(name = "shareYear", nullable = false)
    private int year;

    @Column(nullable = false, length = 36)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RecapShareJpaEntity() {
    }

    public RecapShareJpaEntity(Long id, Long userId, int year, String token, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.year = year;
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

`RecapShareJpaRepository.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.RecapShareJpaEntity;

public interface RecapShareJpaRepository extends JpaRepository<RecapShareJpaEntity, Long> {

    Optional<RecapShareJpaEntity> findByToken(String token);

    Optional<RecapShareJpaEntity> findByUserIdAndYear(Long userId, int year);
}
```

`RecapShareRepositoryAdapter.java`:

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShareRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.RecapShareJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.RecapShareJpaRepository;

@Repository
public class RecapShareRepositoryAdapter implements RecapShareRepository {

    private final RecapShareJpaRepository jpaRepository;

    public RecapShareRepositoryAdapter(RecapShareJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RecapShare> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public Optional<RecapShare> findByUserIdAndYear(Long userId, int year) {
        return jpaRepository.findByUserIdAndYear(userId, year).map(this::toDomain);
    }

    @Override
    public RecapShare save(RecapShare share) {
        RecapShareJpaEntity saved = jpaRepository.save(new RecapShareJpaEntity(
                share.getId(), share.getUserId(), share.getYear(), share.getToken(), share.getCreatedAt()));
        return toDomain(saved);
    }

    private RecapShare toDomain(RecapShareJpaEntity entity) {
        return new RecapShare(entity.getId(), entity.getUserId(), entity.getYear(),
                entity.getToken(), entity.getCreatedAt());
    }
}
```

`RecapShareService.java`:

```java
package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShareRepository;

@Service
@Transactional(readOnly = true)
public class RecapShareService {

    private final RecapShareRepository recapShareRepository;
    private final Clock clock;

    public RecapShareService(RecapShareRepository recapShareRepository, Clock clock) {
        this.recapShareRepository = recapShareRepository;
        this.clock = clock;
    }

    @Transactional
    public RecapShare getOrCreateShare(Long userId, int year) {
        return recapShareRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> recapShareRepository.save(new RecapShare(
                        null, userId, year, UUID.randomUUID().toString(), LocalDateTime.now(clock))));
    }

    public RecapShare getByToken(String token) {
        return recapShareRepository.findByToken(token)
                .orElseThrow(() -> new RecapShareNotFoundException(token));
    }
}
```

- [ ] **Step 6: 컨트롤러 실패 테스트 작성**

`src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapControllerTest.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.RecapService;
import me.singingsandhill.calendar.datedate.application.service.RecapShareService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(RecapController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class RecapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecapService recapService;

    @MockitoBean
    private RecapShareService recapShareService;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private LocaleLinks localeLinks;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    private static RecapDto sampleRecap() {
        return new RecapDto(2026, "지수", 3, 12, 2, 9, "FRIDAY", 5,
                List.of("성수 카페"), List.of("마라탕"), List.of("민준"), false);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kakaoUser() {
        return oauth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L));
    }

    @Test
    @DisplayName("로그인 사용자는 연도 recap 페이지를 본다")
    void rendersRecapForKakaoUser() throws Exception {
        when(recapService.buildRecap(42L, 2026)).thenReturn(sampleRecap());
        when(seoService.getRecapSeo(2026)).thenReturn(SeoMetadata.builder().title("리캡").build());

        mockMvc.perform(get("/recap/2026").with(kakaoUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("recap/recap"));
    }

    @Test
    @DisplayName("/recap 은 올해 recap 으로 리다이렉트한다")
    void redirectsToCurrentYear() throws Exception {
        mockMvc.perform(get("/recap").with(kakaoUser()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("공유 토큰 생성은 멱등이고 recap 페이지로 돌아간다")
    void createsShareAndRedirectsBack() throws Exception {
        when(recapShareService.getOrCreateShare(42L, 2026)).thenReturn(
                new RecapShare(1L, 42L, 2026, "abc-token", LocalDateTime.now()));

        mockMvc.perform(post("/recap/2026/share").with(kakaoUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recap/2026"));
    }

    @Test
    @DisplayName("공유 페이지는 무인증으로 렌더링된다")
    void sharePageIsPublic() throws Exception {
        when(recapShareService.getByToken("abc-token")).thenReturn(
                new RecapShare(1L, 42L, 2026, "abc-token", LocalDateTime.now()));
        when(recapService.buildRecap(42L, 2026)).thenReturn(sampleRecap());
        when(seoService.getRecapShareSeo(eq("지수"), anyInt()))
                .thenReturn(SeoMetadata.builder().title("리캡 공유").robots("noindex, nofollow").build());

        mockMvc.perform(get("/recap/share/abc-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("recap/share"));
    }

    @Test
    @DisplayName("없는 공유 토큰은 404")
    void missingShareTokenIs404() throws Exception {
        when(recapShareService.getByToken("nope"))
                .thenThrow(new RecapShareNotFoundException("nope"));

        mockMvc.perform(get("/recap/share/nope"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 7: 실패 확인 후 컨트롤러·SeoService·템플릿 구현**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*RecapControllerTest\""` → 컴파일 실패 확인.

`RecapController.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import java.time.Clock;
import java.time.Year;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.service.RecapService;
import me.singingsandhill.calendar.datedate.application.service.RecapShareService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class RecapController {

    private final RecapService recapService;
    private final RecapShareService recapShareService;
    private final SeoService seoService;
    private final Clock clock;
    private final String baseUrl;

    public RecapController(RecapService recapService,
                           RecapShareService recapShareService,
                           SeoService seoService,
                           Clock clock,
                           @Value("${app.base-url}") String baseUrl) {
        this.recapService = recapService;
        this.recapShareService = recapShareService;
        this.seoService = seoService;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/recap")
    public String currentYearRecap() {
        return "redirect:/recap/" + Year.now(clock).getValue();
    }

    @GetMapping("/recap/{year:\\d{4}}")
    public String recap(@PathVariable int year, Authentication authentication, Model model) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        RecapDto recap = recapService.buildRecap(userId, year);
        model.addAttribute("recap", recap);
        model.addAttribute("seo", seoService.getRecapSeo(year));
        return "recap/recap";
    }

    @PostMapping("/recap/{year:\\d{4}}/share")
    public String createShare(@PathVariable int year, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        RecapShare share = recapShareService.getOrCreateShare(userId, year);
        redirectAttributes.addFlashAttribute("shareUrl",
                baseUrl + "/recap/share/" + share.getToken());
        return "redirect:/recap/" + year;
    }

    @GetMapping("/recap/share/{token}")
    public String sharedRecap(@PathVariable String token, Model model) {
        RecapShare share = recapShareService.getByToken(token);
        RecapDto recap = recapService.buildRecap(share.getUserId(), share.getYear());
        model.addAttribute("recap", recap);
        model.addAttribute("seo", seoService.getRecapShareSeo(recap.nickname(), recap.year()));
        return "recap/share";
    }
}
```

`MyPageController.java`:

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import java.time.Clock;
import java.time.Year;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class MyPageController {

    private final OwnerService ownerService;
    private final SeoService seoService;
    private final Clock clock;

    public MyPageController(OwnerService ownerService, SeoService seoService, Clock clock) {
        this.ownerService = ownerService;
        this.seoService = seoService;
        this.clock = clock;
    }

    @GetMapping("/me")
    public String myPage(Authentication authentication, Model model) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        model.addAttribute("nickname",
                principal.getAttribute(KakaoOAuth2UserService.ATTR_APP_NICKNAME));
        model.addAttribute("profileImage",
                principal.getAttribute(KakaoOAuth2UserService.ATTR_APP_PROFILE_IMAGE));
        model.addAttribute("owners", ownerService.getOwnersOf(userId));
        model.addAttribute("currentYear", Year.now(clock).getValue());
        model.addAttribute("seo", seoService.getMyPageSeo());
        return "me/mypage";
    }
}
```

`SeoService.java` 에 추가 (연도 인자 메시지는 반드시 `{0,number,#}` 패턴 사용):

```java
    /** 마이페이지 (noindex). */
    public SeoMetadata getMyPageSeo() {
        String path = "/me";
        return SeoMetadata.builder()
            .title(m("seo.mypage.title"))
            .description(m("seo.mypage.description"))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }

    /** 연간 recap 본인 페이지 (noindex). */
    public SeoMetadata getRecapSeo(int year) {
        String path = "/recap/" + year;
        return SeoMetadata.builder()
            .title(m("seo.recap.title", year))
            .description(m("seo.recap.description", year))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }

    /** recap 공유 페이지 (공개 URL 이지만 개인 데이터 — noindex, OG 는 카카오톡 미리보기용). */
    public SeoMetadata getRecapShareSeo(String nickname, int year) {
        String path = "/recap/share";
        return SeoMetadata.builder()
            .title(m("seo.recapShare.title", nickname, year))
            .description(m("seo.recapShare.description", nickname, year))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("article")
            .ogTitle(m("seo.recapShare.title", nickname, year))
            .ogDescription(m("seo.recapShare.description", nickname, year))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }
```

- [ ] **Step 8: i18n 키 추가 (Task 4 Step 5 와 동일한 변환 방식)**

한국어 원문 (ko — 유니코드 이스케이프로 변환해 추가):

```
mypage.title=마이페이지
mypage.owners.title=내 일정 페이지
mypage.owners.empty=아직 연결된 일정 페이지가 없어요. 일정을 만들거나 대시보드에서 연결하세요.
mypage.recap.link={0,number,#} 리캡 보러 가기
recap.title={0,number,#} 모임 리캡
recap.intro={0}님의 {1,number,#} 모임 리캡
recap.created.label=만든 일정
recap.created.value={0,number,#}개의 일정, 연인원 {1,number,#}명과 함께
recap.participated.label=참여한 모임
recap.participated.value={0,number,#}개의 일정에서 {1,number,#}일을 골랐어요
recap.weekday.label=가장 많이 모인 요일
recap.month.label=가장 바빴던 달
recap.month.value={0,number,#}월
recap.votes.locations=내가 투표한 장소 TOP
recap.votes.menus=내가 투표한 메뉴 TOP
recap.companions.label=자주 함께한 사람
recap.empty=아직 {0,number,#}의 기록이 없어요. 첫 일정을 만들어보세요!
recap.share.button=공유 링크 만들기
recap.share.copy=링크 복사
recap.share.ready=공유 링크가 준비됐어요
recap.weekday.MONDAY=월요일
recap.weekday.TUESDAY=화요일
recap.weekday.WEDNESDAY=수요일
recap.weekday.THURSDAY=목요일
recap.weekday.FRIDAY=금요일
recap.weekday.SATURDAY=토요일
recap.weekday.SUNDAY=일요일
seo.mypage.title=마이페이지 - DateDate
seo.mypage.description=내 일정 페이지와 연간 리캡을 확인하세요.
seo.recap.title={0,number,#} 모임 리캡 - DateDate
seo.recap.description={0,number,#}에 만들고 참여한 모임을 한눈에 돌아보세요.
seo.recapShare.title={0}님의 {1,number,#} 모임 리캡 - DateDate
seo.recapShare.description={0}님이 {1,number,#}에 함께한 모임 기록을 확인해 보세요.
```

영어 (`messages_en.properties` 에 그대로):

```properties
mypage.title=My Page
mypage.owners.title=My schedule pages
mypage.owners.empty=No linked schedule pages yet. Create one or link from a dashboard.
mypage.recap.link=See my {0,number,#} recap
recap.title={0,number,#} Meetup Recap
recap.intro={0}'s {1,number,#} meetup recap
recap.created.label=Schedules created
recap.created.value={0,number,#} schedules with {1,number,#} participants in total
recap.participated.label=Meetups joined
recap.participated.value=Picked {1,number,#} days across {0,number,#} schedules
recap.weekday.label=Most popular weekday
recap.month.label=Busiest month
recap.month.value=Month {0,number,#}
recap.votes.locations=Top places I voted
recap.votes.menus=Top menus I voted
recap.companions.label=Frequent companions
recap.empty=No records for {0,number,#} yet. Create your first schedule!
recap.share.button=Create share link
recap.share.copy=Copy link
recap.share.ready=Your share link is ready
recap.weekday.MONDAY=Monday
recap.weekday.TUESDAY=Tuesday
recap.weekday.WEDNESDAY=Wednesday
recap.weekday.THURSDAY=Thursday
recap.weekday.FRIDAY=Friday
recap.weekday.SATURDAY=Saturday
recap.weekday.SUNDAY=Sunday
seo.mypage.title=My Page - DateDate
seo.mypage.description=See your schedule pages and yearly recap.
seo.recap.title={0,number,#} Meetup Recap - DateDate
seo.recap.description=Look back on the meetups you created and joined in {0,number,#}.
seo.recapShare.title={0}'s {1,number,#} Meetup Recap - DateDate
seo.recapShare.description=See the meetups {0} shared in {1,number,#}.
```

**주의:** `recap.intro` 처럼 인자 있는 메시지에서 작은따옴표가 필요하면 `''` 로 이스케이프 (영문 `{0}'s` 는 `{0}''s` 로 써야 함 — MessageFormat 적용 대상). 인자 없는 메시지는 `'` 그대로.

- [ ] **Step 9: 템플릿 3개 작성**

`src/main/resources/templates/recap/recap.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:lang="${#locale.language}">
<head th:replace="~{fragments/head :: head(${seo})}"></head>
<body>
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>

<main class="recap-page">
    <!-- 빈 상태 -->
    <section class="recap-card recap-empty" th:if="${recap.empty()}">
        <h1 th:text="#{recap.title(${recap.year()})}">2026 모임 리캡</h1>
        <p th:text="#{recap.empty(${recap.year()})}">아직 기록이 없어요.</p>
        <a th:href="${@localeLinks.href('/#start-form')}" class="recap-cta" th:text="#{nav.create}">일정 만들기</a>
    </section>

    <th:block th:unless="${recap.empty()}">
        <section class="recap-card recap-intro">
            <h1 th:text="#{recap.intro(${recap.nickname()}, ${recap.year()})}">지수님의 2026 모임 리캡</h1>
        </section>

        <section class="recap-card">
            <h2 th:text="#{recap.created.label}">만든 일정</h2>
            <p class="recap-big" th:text="#{recap.created.value(${recap.schedulesCreated()}, ${recap.totalParticipants()})}">3개의 일정</p>
        </section>

        <section class="recap-card">
            <h2 th:text="#{recap.participated.label}">참여한 모임</h2>
            <p class="recap-big" th:text="#{recap.participated.value(${recap.participationCount()}, ${recap.daysSelected()})}">2개의 일정</p>
        </section>

        <section class="recap-card" th:if="${recap.topWeekday() != null}">
            <h2 th:text="#{recap.weekday.label}">가장 많이 모인 요일</h2>
            <p class="recap-big" th:text="#{'recap.weekday.' + ${recap.topWeekday()}}">금요일</p>
        </section>

        <section class="recap-card" th:if="${recap.busiestMonth() != null}">
            <h2 th:text="#{recap.month.label}">가장 바빴던 달</h2>
            <p class="recap-big" th:text="#{recap.month.value(${recap.busiestMonth()})}">5월</p>
        </section>

        <section class="recap-card" th:if="${!recap.topLocations().isEmpty() or !recap.topMenus().isEmpty()}">
            <h2 th:text="#{recap.votes.locations}">투표한 장소 TOP</h2>
            <ol><li th:each="name : ${recap.topLocations()}" th:text="${name}">성수 카페</li></ol>
            <h2 th:text="#{recap.votes.menus}">투표한 메뉴 TOP</h2>
            <ol><li th:each="name : ${recap.topMenus()}" th:text="${name}">마라탕</li></ol>
        </section>

        <section class="recap-card" th:if="${!recap.topCompanions().isEmpty()}">
            <h2 th:text="#{recap.companions.label}">자주 함께한 사람</h2>
            <ol><li th:each="name : ${recap.topCompanions()}" th:text="${name}">민준</li></ol>
        </section>

        <section class="recap-card recap-share">
            <form th:action="@{'/recap/' + ${recap.year()} + '/share'}" method="post">
                <button type="submit" class="recap-cta" th:text="#{recap.share.button}">공유 링크 만들기</button>
            </form>
            <div th:if="${shareUrl}" class="recap-share-url">
                <p th:text="#{recap.share.ready}">공유 링크가 준비됐어요</p>
                <input type="text" id="shareUrlInput" th:value="${shareUrl}" readonly/>
                <button type="button" id="copyShareUrl" th:text="#{recap.share.copy}">링크 복사</button>
            </div>
        </section>
    </th:block>
</main>

<style>
    .recap-page { max-width: 480px; margin: 0 auto; padding: 2rem 1rem; }
    .recap-card { padding: 3rem 1.5rem; margin-bottom: 1rem; border-radius: 16px;
                  background: linear-gradient(160deg, #f7f4ff, #fff); text-align: center; }
    .recap-big { font-size: 1.5rem; font-weight: 700; margin-top: 0.75rem; }
    .recap-cta { display: inline-block; padding: 12px 20px; border-radius: 10px;
                 background: #6c5ce7; color: #fff; border: none; cursor: pointer; text-decoration: none; }
    .recap-share-url input { width: 100%; margin: 0.5rem 0; padding: 8px; }
</style>

<script>
    (function () {
        var btn = document.getElementById('copyShareUrl');
        if (!btn) return;
        btn.addEventListener('click', function () {
            navigator.clipboard.writeText(document.getElementById('shareUrlInput').value);
        });
    })();
</script>

<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>
</body>
</html>
```

`src/main/resources/templates/recap/share.html` — `recap.html` 과 동일 구조에서 다음만 다르다: (1) 공유 섹션(`recap-share`) 전체 제거, (2) 빈 상태 CTA 는 홈 링크(`/`)로, (3) 마지막에 서비스 유도 카드 추가:

```html
        <section class="recap-card">
            <a th:href="${@localeLinks.href('/')}" class="recap-cta" th:text="#{nav.create}">일정 만들기</a>
        </section>
```

`src/main/resources/templates/me/mypage.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:lang="${#locale.language}">
<head th:replace="~{fragments/head :: head(${seo})}"></head>
<body>
<div th:replace="~{fragments/header :: [th:fragment='header']}"></div>

<main class="mypage">
    <section class="mypage-profile">
        <img th:if="${profileImage != null}" th:src="${profileImage}" alt="" width="64" height="64"/>
        <h1 th:text="${nickname}">닉네임</h1>
        <a th:href="@{'/recap/' + ${currentYear}}" class="mypage-recap-link"
           th:text="#{mypage.recap.link(${currentYear})}">2026 리캡 보러 가기</a>
    </section>

    <section class="mypage-owners">
        <h2 th:text="#{mypage.owners.title}">내 일정 페이지</h2>
        <p th:if="${owners.isEmpty()}" th:text="#{mypage.owners.empty}">아직 연결된 페이지가 없어요.</p>
        <ul>
            <li th:each="owner : ${owners}">
                <a th:href="${@localeLinks.href('/' + owner.ownerId)}" th:text="${owner.ownerId}">my-crew</a>
            </li>
        </ul>
    </section>
</main>

<style>
    .mypage { max-width: 640px; margin: 0 auto; padding: 2rem 1rem; }
    .mypage-profile { text-align: center; margin-bottom: 2rem; }
    .mypage-profile img { border-radius: 50%; }
    .mypage-recap-link { display: inline-block; margin-top: 0.75rem; padding: 10px 16px;
                         border-radius: 10px; background: #6c5ce7; color: #fff; text-decoration: none; }
</style>

<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>
</body>
</html>
```

- [ ] **Step 10: 테스트 통과 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests \"*RecapControllerTest\" --tests \"*RecapServiceTest\" --tests \"*SeoServiceI18nTest\""`
Expected: 모두 PASS. `SeoServiceI18nTest` 로 연도 그룹화(`2,026`) 회귀 확인.

- [ ] **Step 11: git_commit.md 에 커밋 섹션 추가**

```
# Commit 78 — feat(datedate): 연간 recap 페이지·공유 토큰·마이페이지
git add src/main/java/me/singingsandhill/calendar/datedate/domain/recap/ src/main/java/me/singingsandhill/calendar/datedate/application/dto/RecapDto.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/InvalidRecapYearException.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/RecapShareNotFoundException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapService.java src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapShareService.java src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/RecapShareJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/RecapShareJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/RecapShareRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/MyPageController.java src/main/resources/templates/recap/ src/main/resources/templates/me/ src/main/resources/messages.properties src/main/resources/messages_en.properties src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapControllerTest.java docs/git_commit.md
git commit -m "feat(datedate): 연간 Wrapped 스타일 recap + 공개 공유 토큰 + 마이페이지" -m "오너 계열(일정·연인원·요일·월·동행)+활동 계열(참여·선택일·투표 TOP3) on-the-fly 집계, Clock 고정 테스트. (userId,year) 멱등 공유 토큰, OG noindex. i18n {0,number,#} 연도 그룹화 차단."
```

---

### Task 8: 문서 동기화 (ADR 2건 + CLAUDE.md) + 전체 검증

**Files:**
- Create: `docs/adr/common/security/0004-kakao-oauth2-login.md`
- Create: `docs/adr/datedate/domain/0005-user-activity-event-recap.md`
- Modify: `docs/adr/README.md` (색인 2건 추가 — 기존 형식 확인 후 동일 형식)
- Modify: `CLAUDE.md` (Security 표, DateDate 모듈 섹션)
- Modify: `src/main/java/me/singingsandhill/calendar/common/CLAUDE.md` (SecurityConfig 규칙 표)
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md` (신규 서비스)

**Interfaces:**
- Consumes: Task 1~7 의 구현 사실
- Produces: CLAUDE.md/ADR 동기화 규칙 충족 (결정 변경 → 새 ADR)

- [ ] **Step 1: ADR common/security/0004 작성**

`docs/adr/common/security/0004-kakao-oauth2-login.md` (기존 0003 의 섹션 구성을 따른다 — Status/Context/Decision/Consequences):

```markdown
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
   카카오는 CommonOAuth2Provider 에 없어 application.yaml 에 직접 등록한다.
2. **`client-authentication-method: client_secret_post`.** 카카오 토큰
   엔드포인트는 client_secret 을 POST body 로만 받는다 (Basic 헤더는 KOE010
   실패). `KakaoClientRegistrationTest` 가 회귀 가드.
3. **커스텀 `KakaoOAuth2UserService`** 가 `/v2/user/me` 를 파싱해 `AppUser`
   upsert 후 ROLE_USER 프린시펄을 만든다. 내부 userId 를 attributes 에 실어
   컨트롤러 재조회를 없앤다. scope 는 profile_nickname·profile_image 만
   (account_email 은 비즈 앱 전환 필요 — 범위 외).
4. **진입점 분리.** `/runners/admin/**`·`/trading*`·`/api/trading/**` 미인증은
   기존 어드민 로그인으로, 그 외 보호 경로(`/me`, `/recap/**`)는 `/login`
   (카카오 버튼)으로. 로그아웃도 2계열(`/runners/admin/logout` → `/runners`,
   `POST /logout` → `/`).
5. **접근 규칙 순서.** `/recap/share/**` permitAll → `/me`·`/recap/**`·
   `/api/me/**` ROLE_USER 를 포괄 permitAll(`/api/**`, `/*`) 보다 먼저 선언
   (ADR 0003 과 동일 원칙). 회귀 가드: `DatedateAuthSecurityTest`.

## Consequences

- 카카오 액세스 토큰은 저장·재사용하지 않는다 (로그인 시 1회 사용, 세션은
  서비스 자체 세션). 연결끊기(unlink)·탈퇴는 후속 과제.
- ROLE_USER 는 어드민 영역에 접근 불가(403). 어드민 세션은 datedate 사용자
  기능(/me 등)에 접근 불가 — 역할 상호 배타.
- 배포 전 카카오 콘솔에 운영 Redirect URI 등록 필수.
```

- [ ] **Step 2: ADR datedate/domain/0005 작성**

`docs/adr/datedate/domain/0005-user-activity-event-recap.md`:

```markdown
# 0005. 활동 이벤트 테이블 기반 연간 recap (voters 구조 무변경)

- Status: Accepted
- Date: 2026-07-11

## Context

연간 recap ("만든 일정·참여·투표 돌아보기") 에는 사용자별 기록이 필요하다.
그러나 기존 구조에서 참여자는 이름 문자열, 투표는 Location/Menu 의 voters
문자열 리스트라 사용자 연결 지점이 없다. 요구사항은 "카카오 로그인 상태에서
한 참여·투표 기록만 연결" (소급 없음, 익명 병행 유지).

## Decision

1. **append-only `UserActivity` 이벤트 테이블.** (userId, type, scheduleId,
   targetId, detail, occurredAt). 기존 voters/Participant 구조는 그대로 두고,
   로그인 세션의 컨트롤러 성공 경로에서만 1행 기록한다. 대안이었던 "기존
   테이블에 user_id 컬럼 추가"는 투표가 JSON/문자열 구조라 대수술이 필요해
   기각.
2. **중복 방지 키 (userId, type, targetId).** targetId 는
   PARTICIPATION=participantId, *_VOTE=location/menu id. participantId 를
   보존해 recap 이 Participant.selections 를 조인, "선택한 날짜 수"를 집계.
3. **기록 실패는 본 동작을 막지 않는다.** REQUIRES_NEW + 예외 삼킴(WARN).
4. **오너 연결은 first-claim.** ownerId 는 공개 슬러그로 소유 증명이 원천
   불가 → 선점 정책 수용, 타 유저 선점 시 409. 악의 선점 구제는 수동(DB).
5. **recap 은 on-the-fly 집계** (스냅샷 없음). 오너 계열(내 오너들의 해당
   연도 일정)+활동 계열(내 이벤트). 데이터 규모상 충분, 필요 시 캐시는 후속.

## Consequences

- 익명 사용자 경로는 바이트 단위로 무변경 (회귀 가드:
  `ParticipantApiActivityRecordingTest`).
- 로그인 이전 기록은 recap 에 나타나지 않는다 (의도된 제약).
- 공유 페이지(/recap/share/{token})는 공개 URL — 닉네임·집계 수치만 노출,
  프로필 이미지 미노출, noindex.
```

- [ ] **Step 3: CLAUDE.md 3곳 갱신**

1. 루트 `CLAUDE.md` Security 표에 행 추가:

```markdown
| `/me`, `/recap/**` (share 제외), `/api/me/**` | `ROLE_USER` (카카오 로그인, [ADR 0004](docs/adr/common/security/0004-kakao-oauth2-login.md)) |
| `/login`, `/oauth2/**`, `/login/oauth2/**`, `/recap/share/**` | permitAll |
```

같은 파일 DateDate 모듈 섹션에 추가:

```markdown
- 카카오 로그인 (선택적): `KakaoOAuth2UserService` → `AppUser` upsert, 오너 연결(first-claim), `UserActivity` 이벤트 기록
- `RecapService` + `RecapController` → `/recap/{year}` 연간 리캡, `/recap/share/{token}` 공개 공유 (ADR datedate/domain/0005)
```

2. `common/CLAUDE.md` SecurityConfig 표에 동일 행 추가 + 진입점 분리·로그아웃 2계열 한 줄 설명.
3. `datedate/application/CLAUDE.md` Services 목록에 `AppUserService`, `UserActivityService`, `RecapService`, `RecapShareService` 한 줄씩 추가.
4. `docs/adr/README.md` 색인에 ADR 2건 추가 (기존 색인 형식 그대로).

- [ ] **Step 4: 전체 테스트 스위트 실행**

Run: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"`
Expected: BUILD SUCCESSFUL. 실패 시 해당 태스크로 돌아가 수정 (특히 SecurityConfig 를 import 하는 다른 테스트의 mock 누락).

- [ ] **Step 5: 수동 QA (실제 카카오 키 필요 — .env 설정 후)**

```bash
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat bootRun"
```

체크리스트 (WSL 에서는 `cmd.exe /c curl` 사용):
1. `http://localhost:8081/` 헤더에 "카카오 로그인" 버튼 노출
2. 버튼 클릭 → kauth.kakao.com 동의 화면 → 동의 → `/me` 로 복귀, 닉네임 표시
3. 로그인 상태에서 `POST /start` 로 오너 생성 → `/me` 에 오너 목록 표시
4. 일정 생성 + 참여자 추가 + 장소/메뉴 투표 → `/recap/2026` 에 수치 반영
5. 공유 링크 생성 → 시크릿 창(비로그인)에서 공유 URL 열림 확인
6. 시크릿 창에서 `/me` → `/login` 리다이렉트 확인
7. 러너 어드민 로그인/로그아웃 기존 동작 확인
8. `?lang=en` 으로 영어 recap 확인

- [ ] **Step 6: git_commit.md 에 커밋 섹션 추가**

```
# Commit 79 — docs: ADR 0004(카카오 로그인)·0005(활동 이벤트 recap) + CLAUDE.md 동기화 — 마지막 커밋(git_commit.md 포함)
git add docs/adr/common/security/0004-kakao-oauth2-login.md docs/adr/datedate/domain/0005-user-activity-event-recap.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/superpowers/plans/2026-07-11-kakao-login-recap.md docs/git_commit.md
git commit -m "docs: ADR 0004(카카오 OAuth2)·0005(활동 이벤트 recap) + CLAUDE.md 동기화" -m "결정 변경 2건 기록: client_secret_post·진입점 분리·역할 배타 / append-only 이벤트·first-claim·on-the-fly 집계. CLAUDE.md Security 표·모듈 섹션 갱신."
```

---

## 계획 셀프 리뷰 노트 (작성 시 반영됨)

- **스펙 커버리지:** 스펙 §5.1(Task 1), §5.2(Task 2·5·6·7), §5.3(Task 3), §5.4(Task 4), §5.5~5.6(Task 5·7), §5.7(Task 6), §5.8(Task 7), §5.9(Task 2·5·7 예외), §6(각 태스크 테스트), §7(Task 8). 누락 없음.
- **orphanRemoval 함정:** Owner 저장 경로가 일정을 삭제할 수 있는 문제를 Task 5 Step 4 에서 명시적으로 처리.
- **@WebMvcTest + SecurityConfig 함정:** oauth2Login 도입 후 SecurityConfig 를 import 하는 모든 슬라이스 테스트는 `ClientRegistrationRepository`·`KakaoOAuth2UserService` mock 필요 — Task 3 Step 6, Task 6 Step 6 에 반영.
- **확인 후 조정 항목 (구현자가 실제 코드로 검증):** ① `ParticipantColor` 생성자 시그니처 (Task 6·7 테스트 픽스처), ② 러너 어드민 로그아웃의 GET/POST 여부 (Task 3 Step 5), ③ `SeoService` 의 `m()` 헬퍼 가시성(private 이면 동일 클래스 내 추가라 무관), ④ 기존 4개 API 컨트롤러 단위 테스트 파일 유무와 mock 추가 대상.
