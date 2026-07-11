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
                // catch-all 매핑으로 등록해야 admin 매처가 우선 평가된다.
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
