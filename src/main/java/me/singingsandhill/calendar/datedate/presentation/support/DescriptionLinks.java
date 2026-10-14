package me.singingsandhill.calendar.datedate.presentation.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.singingsandhill.calendar.datedate.presentation.dto.response.TextSegment;

/**
 * 일정 설명에서 http(s) URL 을 찾아 평문·링크 조각으로 나눈다 (ADR datedate/domain/0011).
 *
 * <p>URL 은 RFC 3986 의 ASCII 문자만 이어지는 구간이라 한글 조사가 바로 붙어도("https://x.so/a에서") 조사 앞에서 끊긴다.
 * 끝의 문장부호와 짝 없는 닫는 괄호는 링크에서 빼고 다음 평문 조각으로 넘긴다. 다른 스킴은 링크가 되지 않는다.
 */
public final class DescriptionLinks {

    private static final Pattern URL = Pattern.compile("(?i)https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    private static final String TRAILING_PUNCTUATION = ".,;:!?'";

    private DescriptionLinks() {
    }

    public static List<TextSegment> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = URL.matcher(text);
        int cursor = 0;
        while (matcher.find()) {
            String url = trimTrailing(matcher.group());
            if (!hasHost(url)) {
                continue;
            }
            if (matcher.start() > cursor) {
                segments.add(new TextSegment(text.substring(cursor, matcher.start()), null));
            }
            segments.add(new TextSegment(url, url));
            cursor = matcher.start() + url.length();
        }
        if (cursor < text.length()) {
            segments.add(new TextSegment(text.substring(cursor), null));
        }
        return segments;
    }

    private static String trimTrailing(String url) {
        String trimmed = url;
        while (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            boolean unbalancedParen = last == ')' && count(trimmed, '(') < count(trimmed, ')');
            if (TRAILING_PUNCTUATION.indexOf(last) < 0 && !unbalancedParen) {
                break;
            }
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean hasHost(String url) {
        int schemeEnd = url.indexOf("://") + 3;
        return url.length() > schemeEnd;
    }

    private static int count(String value, char target) {
        int n = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                n++;
            }
        }
        return n;
    }
}
