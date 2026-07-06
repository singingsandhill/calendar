package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS: /api/** 를 앱인토스 미니앱(다른 origin)에서 호출 가능하게 함 (CorsConfig 빈 사용).
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // 트레이딩 봇 제어·실주문 API 및 제어 대시보드는 관리자 전용 (P0-1).
                // 반드시 아래 /api/** · /* 포괄 permitAll 규칙보다 먼저 선언해야 매칭 우선순위가 보장된다.
                .requestMatchers("/api/trading/**").hasRole("ADMIN")
                .requestMatchers("/trading", "/trading/**").hasRole("ADMIN")

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
            .logout(logout -> logout
                .logoutUrl("/runners/admin/logout")
                .logoutSuccessUrl("/runners")
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
