package me.singingsandhill.calendar.common.infrastructure.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Locale resolution order:
 *   1. Cookie "lang" (ko | en)
 *   2. Accept-Language header (first supported language wins)
 *   3. Default: ko
 *
 * When setLocale() is called (by LocaleChangeInterceptor via ?lang=xx),
 * the chosen locale is persisted in a cookie for 1 year.
 */
public class CookieThenAcceptLanguageLocaleResolver implements LocaleResolver {

    private static final String COOKIE_NAME = "lang";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365;
    private static final List<String> SUPPORTED = List.of("ko", "en");

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 1. Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName()) && SUPPORTED.contains(c.getValue())) {
                    return Locale.forLanguageTag(c.getValue());
                }
            }
        }

        // 2. Accept-Language header
        String header = request.getHeader("Accept-Language");
        if (header != null && !header.isBlank()) {
            try {
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(header)) {
                    String lang = range.getRange().toLowerCase();
                    String primary = lang.contains("-") ? lang.split("-")[0] : lang;
                    if (SUPPORTED.contains(primary)) {
                        return Locale.forLanguageTag(primary);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // malformed Accept-Language header — fall through to default
            }
        }

        // 3. Default: Korean
        return Locale.KOREAN;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        String lang = (locale != null && SUPPORTED.contains(locale.getLanguage()))
                ? locale.getLanguage() : "ko";
        Cookie cookie = new Cookie(COOKIE_NAME, lang);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
