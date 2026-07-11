package me.singingsandhill.calendar.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieThenAcceptLanguageLocaleResolverTest {

    private CookieThenAcceptLanguageLocaleResolver resolver;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        resolver = new CookieThenAcceptLanguageLocaleResolver();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("No cookie and no Accept-Language → Korean fallback")
    void resolveLocale_defaultsToKorean() {
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("ko");
    }

    @Test
    @DisplayName("Accept-Language 'en-US' → English when no cookie")
    void resolveLocale_usesAcceptLanguage_whenNoCookie() {
        request.addHeader("Accept-Language", "en-US,en;q=0.9");

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Cookie 'lang=en' beats Accept-Language 'ko'")
    void resolveLocale_cookieTakesPrecedenceOverAcceptLanguage() {
        request.setCookies(new Cookie("lang", "en"));
        request.addHeader("Accept-Language", "ko-KR,ko;q=0.9");

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Unsupported cookie value → falls through to Accept-Language")
    void resolveLocale_ignoresUnsupportedCookieValue() {
        request.setCookies(new Cookie("lang", "fr"));
        request.addHeader("Accept-Language", "en");

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("setLocale writes cookie with 1-year max-age, path /, SameSite=Lax")
    void setLocale_writesPersistentCookie() {
        resolver.setLocale(request, response, Locale.ENGLISH);

        Cookie cookie = response.getCookie("lang");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("en");
        assertThat(cookie.getMaxAge()).isEqualTo(60 * 60 * 24 * 365);
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    @DisplayName("setLocale caches locale on current request (LocaleChangeInterceptor round-trip)")
    void setLocale_cachesOnRequest_soSameRequestResolvesNewLocale() {
        request.setCookies(new Cookie("lang", "ko"));

        resolver.setLocale(request, response, Locale.ENGLISH);

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Unsupported locale passed to setLocale → defaults to ko cookie")
    void setLocale_fallsBackToKorean_forUnsupportedLocale() {
        resolver.setLocale(request, response, Locale.FRENCH);

        Cookie cookie = response.getCookie("lang");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("ko");
    }
}
