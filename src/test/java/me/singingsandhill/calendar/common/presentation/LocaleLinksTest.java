package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class LocaleLinksTest {

    private LocaleLinks links;

    @BeforeEach
    void setUp() {
        links = new LocaleLinks();
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Default locale (ko) → no lang query appended")
    void href_skipsLangForDefaultLocale() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        assertThat(links.href("/")).isEqualTo("/");
        assertThat(links.href("/insights/trends")).isEqualTo("/insights/trends");
        assertThat(links.href("/sweet-bear-55/2026/5")).isEqualTo("/sweet-bear-55/2026/5");
    }

    @Test
    @DisplayName("Non-default locale (en) → lang=en appended")
    void href_appendsLangForNonDefaultLocale() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href("/")).isEqualTo("/?lang=en");
        assertThat(links.href("/insights/trends")).isEqualTo("/insights/trends?lang=en");
    }

    @Test
    @DisplayName("URL with existing query → uses & separator")
    void href_usesAmpersandWhenQueryExists() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href("/path?foo=bar")).isEqualTo("/path?foo=bar&lang=en");
    }

    @Test
    @DisplayName("URL with fragment → lang inserted before #")
    void href_insertsLangBeforeFragment() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href("/#start-form")).isEqualTo("/?lang=en#start-form");
        assertThat(links.href("/path?x=1#frag")).isEqualTo("/path?x=1&lang=en#frag");
    }

    @Test
    @DisplayName("URL already has lang param → unchanged")
    void href_idempotent_whenLangAlreadyPresent() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href("/?lang=en")).isEqualTo("/?lang=en");
        assertThat(links.href("/?lang=ko")).isEqualTo("/?lang=ko");
        assertThat(links.href("/path?x=1&lang=en")).isEqualTo("/path?x=1&lang=en");
    }

    @Test
    @DisplayName("External URLs (http/https/protocol-relative/mailto/tel) → unchanged")
    void href_leavesExternalUrlsUntouched() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href("https://example.com/foo")).isEqualTo("https://example.com/foo");
        assertThat(links.href("http://example.com")).isEqualTo("http://example.com");
        assertThat(links.href("//cdn.example.com/style.css")).isEqualTo("//cdn.example.com/style.css");
        assertThat(links.href("mailto:foo@bar.com")).isEqualTo("mailto:foo@bar.com");
        assertThat(links.href("tel:+82-2-1234-5678")).isEqualTo("tel:+82-2-1234-5678");
    }

    @Test
    @DisplayName("Null/empty input → returned as-is")
    void href_handlesNullAndEmpty() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.href(null)).isNull();
        assertThat(links.href("")).isEqualTo("");
    }

    @Test
    @DisplayName("redirect() produces 'redirect:' prefix and preserves lang")
    void redirect_prefixesAndPreservesLang() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(links.redirect("/sweet-bear-55"))
                .isEqualTo("redirect:/sweet-bear-55?lang=en");
        assertThat(links.redirect("/")).isEqualTo("redirect:/?lang=en");

        LocaleContextHolder.setLocale(Locale.KOREAN);
        assertThat(links.redirect("/")).isEqualTo("redirect:/");
    }
}
