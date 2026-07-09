# Spring Security `@WebMvcTest` 슬라이스 테스트 트러블슈팅

`SecurityConfig` 의 경로별 접근 규칙을 `@WebMvcTest` 슬라이스로 검증할 때(예: `/api/trading/**`
관리자 전용 회귀 테스트) 마주친 두 가지 함정과 해결. 환경: Spring Boot 4.0, Spring Security 6.x,
JUnit 5, `spring-security-test`.

> 이 문서의 예시는 [ADR common/security/0003](../adr/common/security/0003-admin-only-trading-control-api.md)
> (트레이딩 제어 API 관리자 전용, P0-1) 의 회귀 테스트 `TradingApiSecurityTest` 작성 중 발생.

---

## 증상 1 — 컨텍스트 로딩 실패: `NoSuchBeanDefinitionException`

보안 규칙만 검증하려고 컨트롤러 하나만 로드하는 슬라이스를 만들었는데, 테스트가 **어서션이 아니라
컨텍스트 로딩 단계에서** 실패했다.

```java
@WebMvcTest(BotControlApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class TradingApiSecurityTest {
    @MockitoBean TradingBotService tradingBotService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean PasswordEncoder passwordEncoder;
    // ...
}
```

### 에러 로그

```
java.lang.IllegalStateException ... Failed to load ApplicationContext
Caused by: UnsatisfiedDependencyException: Error creating bean with name 'webConfig' ...
  Unsatisfied dependency expressed through constructor parameter 0:
  Error creating bean with name 'ownerPathInterceptor' ...
  Caused by: NoSuchBeanDefinitionException: No qualifying bean of type
  'me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository' available
```

### 원인

`@WebMvcTest` 는 지정한 컨트롤러뿐 아니라 **웹 계층 인프라 빈 전체**를 로드한다. 여기엔
`WebMvcConfigurer` 구현체인 `common/.../config/WebConfig` 도 포함되고, `WebConfig` 는
`datedate` 의 `OwnerPathInterceptor` 를 등록한다. `OwnerPathInterceptor` 는 생성자에서
`OwnerRepository` 를 요구하는데, 슬라이스에는 그 빈이 없어 컨텍스트 생성이 실패한다.

즉 **테스트 대상 컨트롤러의 의존성만 목킹해서는 부족**하고, 슬라이스가 끌어오는 `WebConfig` →
인터셉터의 전이(transitive) 의존성까지 목킹해야 한다.

### 해결

전이 의존성을 `@MockitoBean` 으로 채운다. 기존 `common/.../config/CorsConfigTest` 가 이미
`OwnerRepository` 를 목킹하는 것과 같은 이유다.

```java
@MockitoBean OwnerRepository ownerRepository;   // WebConfig → OwnerPathInterceptor 가 요구
```

> **팁:** 새 보안 슬라이스 테스트를 쓸 때는 `CorsConfigTest` 의 `@MockitoBean` 목록을 그대로
> 시작점으로 삼으면(= `OwnerRepository`, `AdminRepository`, `PasswordEncoder`) 이 함정을 피한다.
> 폼 로그인용 `RunnerUserDetailsService` 는 `@Service` 라 `@WebMvcTest` 가 스캔하지 않으므로,
> 인증을 실제로 수행하지 않는 테스트(아래 증상 2의 post-processor 방식)에서는 목킹이 불필요하다.

---

## 증상 2 — `@WithMockUser` 가 무시되고 인증 사용자가 302(로그인 리다이렉트)로 처리됨

컨텍스트가 뜬 뒤, `@WithMockUser(roles = "ADMIN")` 을 붙인 요청이 200/403 이 아니라 **302 리다이렉트**
로 나왔다. 즉 보안 필터가 해당 요청을 **익명(미인증)** 으로 판정했다.

```java
@Test
@WithMockUser(roles = "ADMIN")   // 적용되지 않음
void adminCanReachEmergencyClose() throws Exception {
    mockMvc.perform(post("/api/trading/bot/emergency-close"))
            .andExpect(status().isOk());   // 실패: Status expected:<200> but was:<302>
}
```

### 에러 로그

```
java.lang.AssertionError: Status expected:<200> but was:<302>   // ADMIN 케이스
java.lang.AssertionError: Status expected:<403> but was:<302>   // USER 케이스
MockHttpServletResponse: Status = 302   // 로그인 페이지로 리다이렉트 = 익명 취급
```

### 원인

`@WithMockUser` 가 심어주는 `SecurityContext` 를 MockMvc 요청이 집어가려면 MockMvc 에
`SecurityMockMvcConfigurers.springSecurity()` 가 적용돼 있어야 한다. 이 슬라이스 설정에서는
보안 **필터 체인 자체는 동작**(미인증 요청이 302 로 리다이렉트되는 것으로 확인)했지만,
`@WithMockUser` 의 테스트 컨텍스트가 요청에 전파되지 않아 인증이 붙지 않았다. (원인은
`@WithMockUser` 애노테이션과 MockMvc 자동설정 간 전파 경로 문제이며, 필터 체인 미적용은 아님.)

### 해결

애노테이션 대신 **요청 단위 post-processor** `SecurityMockMvcRequestPostProcessors.user(...)` 로
인증을 요청에 직접 부착한다. 전파 경로에 의존하지 않아 슬라이스에서 안정적이다.

```java
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.user;

mockMvc.perform(post("/api/trading/bot/emergency-close").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk());          // ADMIN → 통과

mockMvc.perform(post("/api/trading/bot/emergency-close").with(user("op").roles("USER")))
        .andExpect(status().isForbidden());   // 비관리자 → 403

mockMvc.perform(post("/api/trading/bot/emergency-close"))   // post-processor 없음 = 미인증
        .andExpect(status().is3xxRedirection());            // 로그인으로 리다이렉트
```

### 함정: 상태 코드 의미 구분

- **미인증** → `302` (폼 로그인 진입점이 로그인 페이지로 리다이렉트). `is3xxRedirection()`.
- **인증됐으나 권한 부족** → `403` (`AccessDeniedException`). `isForbidden()`.
- 두 경우를 혼동해 미인증에 403 을 기대하면 테스트가 어긋난다. (API/XHR 클라이언트에는 302 보다
  깔끔한 401 JSON 응답이 낫지만, `/api/trading/**` 전용 `AuthenticationEntryPoint` 추가는 별도
  후속 과제 — P0-1 의 핵심(무인증 차단)은 302 로도 충족.)

### CSRF 상호작용 주의

`/api/trading/**` 는 `/api/**` CSRF 비활성 하위라 POST 에 CSRF 토큰이 불필요했다. 만약 대상 경로가
CSRF 활성이면 `.with(csrf())` 를 추가해야 하며, 없으면 인가 평가 **이전에** CSRF 필터가 403 을
던져 "권한" 테스트가 오염된다.

---

## 요약 체크리스트 (보안 슬라이스 테스트 작성 시)

- [ ] 컨텍스트 로딩 실패 → 테스트 대상 컨트롤러 의존성 **+ `WebConfig` 전이 의존성**(`OwnerRepository`
      등)을 `@MockitoBean`. `CorsConfigTest` 의 목킹 목록을 시작점으로.
- [ ] 인증 사용자 케이스는 `@WithMockUser` 대신 `.with(user(...).roles(...))` post-processor 사용.
- [ ] 상태 코드: 미인증 `302`(리다이렉트), 비권한 `403`, 정상 `200` 을 구분해서 단언.
- [ ] CSRF 활성 경로면 `.with(csrf())` 추가.
- [ ] RED 을 실제로 관찰(규칙 제거 시 미인증/비관리자가 `200` 으로 통과하는지)해 회귀 탐지력 확인.
