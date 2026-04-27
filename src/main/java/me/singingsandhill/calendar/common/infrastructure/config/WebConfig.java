package me.singingsandhill.calendar.common.infrastructure.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import me.singingsandhill.calendar.datedate.infrastructure.security.OwnerPathInterceptor;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OwnerPathInterceptor ownerPathInterceptor;

    public WebConfig(OwnerPathInterceptor ownerPathInterceptor) {
        this.ownerPathInterceptor = ownerPathInterceptor;
    }

    private static final Set<String> PUBLIC_SEO_PATHS = Set.of(
            "/", "/guide", "/privacy", "/terms"
    );

    private static final Set<String> PUBLIC_SEO_PREFIXES = Set.of(
            "/use-cases", "/insights", "/runners"
    );

    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new CookieThenAcceptLanguageLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Bean
    public Filter contentLanguageFilter(LocaleResolver localeResolver) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                Locale locale = localeResolver.resolveLocale(request);
                response.setHeader("Content-Language", locale.toLanguageTag());
                filterChain.doFilter(request, response);
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(cacheControlInterceptor());
        registry.addInterceptor(ownerPathInterceptor)
                .addPathPatterns("/api/owners/**");
    }

    @Bean
    public HandlerInterceptor cacheControlInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Object handler,
                                   ModelAndView modelAndView) {
                String path = request.getRequestURI();
                if (path.startsWith("/runners/admin")) {
                    response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                } else if (isPublicSeoPage(path)) {
                    response.setHeader("Cache-Control", "public, max-age=3600, s-maxage=86400");
                    response.setHeader("Vary", "Cookie, Accept-Language");
                    response.setHeader("Pragma", "");
                }
            }
        };
    }

    private boolean isPublicSeoPage(String path) {
        if (PUBLIC_SEO_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_SEO_PREFIXES) {
            if (path.startsWith(prefix) && !path.startsWith("/runners/admin")) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }
}
