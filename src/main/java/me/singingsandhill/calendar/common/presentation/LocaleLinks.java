package me.singingsandhill.calendar.common.presentation;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Builds locale-aware internal URLs.
 *
 * 사용처: Thymeleaf (`${@localeLinks.href('/path')}`) 와 컨트롤러 redirect
 * (`localeLinks.redirect('/path')`).
 *
 * 규칙:
 * - 기본 locale (ko) 이면 lang 쿼리 추가 안 함
 * - http(s)://, //, mailto: 등 외부/스킴 링크는 그대로 반환
 * - 이미 lang= 쿼리가 있는 URL 도 그대로 반환
 * - fragment(#...) 가 있으면 lang 을 그 앞에 삽입
 */
@Component("localeLinks")
public class LocaleLinks {

    static final String DEFAULT_LANGUAGE = "ko";
    private static final Pattern LANG_PARAM = Pattern.compile("[?&]lang=[^&#]*");

    public String href(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        if (isExternal(url)) {
            return url;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return href(url, locale);
    }

    public String href(String url, Locale locale) {
        if (url == null || url.isEmpty() || isExternal(url)) {
            return url;
        }
        String lang = locale != null ? locale.getLanguage() : DEFAULT_LANGUAGE;
        if (DEFAULT_LANGUAGE.equals(lang)) {
            return url;
        }
        if (LANG_PARAM.matcher(url).find()) {
            return url;
        }

        int hashIdx = url.indexOf('#');
        String base = hashIdx >= 0 ? url.substring(0, hashIdx) : url;
        String fragment = hashIdx >= 0 ? url.substring(hashIdx) : "";
        String separator = base.indexOf('?') >= 0 ? "&" : "?";
        return base + separator + "lang=" + lang + fragment;
    }

    public String redirect(String path) {
        return "redirect:" + href(path);
    }

    private static boolean isExternal(String url) {
        return url.startsWith("http://")
                || url.startsWith("https://")
                || url.startsWith("//")
                || url.startsWith("mailto:")
                || url.startsWith("tel:");
    }
}
