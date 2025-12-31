package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .authorizeHttpRequests(auth -> auth
                // 기존 앱 경로 - 모두 허용
                .requestMatchers("/", "/start", "/index.html").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/image/**", "/favicon.*", "/manifest.json", "/robots.txt", "/sitemap.xml", "/ads.txt").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // 러너 공개 경로
                .requestMatchers("/runners").permitAll()
                .requestMatchers("/runners/announce").permitAll()
                .requestMatchers("/runners/runs", "/runners/runs/**").permitAll()
                .requestMatchers("/runners/members", "/runners/members/**").permitAll()
                .requestMatchers("/runners/css/**", "/runners/js/**", "/runners/images/**").permitAll()
                .requestMatchers("/runners/admin/login").permitAll()

                // 러너 관리자 경로 - ADMIN 역할 필요
                .requestMatchers("/runners/admin", "/runners/admin/**").hasRole("ADMIN")

                // 주식 트레이딩 봇 경로
                .requestMatchers("/stock", "/stock/**").permitAll()
                .requestMatchers("/api/stock/**").permitAll()

                // 기존 동적 경로 (owner 페이지 등)
                .requestMatchers("/{ownerId}").permitAll()
                .requestMatchers("/{ownerId}/{year}/{month}").permitAll()

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
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
