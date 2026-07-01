package me.singingsandhill.calendar.common.infrastructure.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 설정 — 앱인토스(Apps in Toss) WebView 미니앱이 다른 origin 에서 {@code /api/**} 를
 * 브라우저 fetch 로 호출할 수 있도록 허용한다.
 *
 * <p>{@code /api/**} 는 인증 없는 공개 엔드포인트이고 쿠키를 사용하지 않으므로
 * {@code allowCredentials=false} 로 두고 origin 패턴을 폭넓게 허용한다. 운영 미니앱의 정확한
 * origin 이 확정되면 {@code setAllowedOriginPatterns} 를 좁힌다.
 *
 * <p>결정 근거: docs/adr/common/security/0002-cors-for-apps-in-toss-miniapp.md
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
