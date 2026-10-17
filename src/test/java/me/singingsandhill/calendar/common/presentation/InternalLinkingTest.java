package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import me.singingsandhill.calendar.common.application.service.SitemapService;

/**
 * 색인 페이지 사이의 내부 링크 가드 (고아 페이지 방지).
 *
 * <p>사이트맵에만 있고 어떤 페이지에서도 링크되지 않는 URL 은 크롤러가 사이트맵으로만 발견하고
 * 사용자·AdSense 리뷰어는 도달할 수 없다. 2026-09-24 점검에서 {@code /faq}·{@code /use-cases}·
 * {@code /guides} 가 정확히 이 상태로 발견됐다 (docs/audit/seo-comprehensive-audit-2026-09-24.md N1).
 * 새 색인 페이지를 추가하면서 링크를 빠뜨리는 회귀도 여기서 잡힌다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalLinkingTest {

    private static final Pattern LOC = Pattern.compile("<loc>(.*?)</loc>");
    /** {@code <a>} 의 href 만 — head 의 canonical·hreflang {@code <link>} 는 내비게이션 링크가 아니다. */
    private static final Pattern HREF = Pattern.compile("<a\\s[^>]*?href=\"([^\"]*)\"");
    private static final Pattern NAV_MENU =
            Pattern.compile("(?s)<div class=\"nav-menu\" id=\"navMenu\">.*?<div class=\"nav-auth\">");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SitemapService sitemapService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Test
    @DisplayName("사이트맵의 모든 URL 은 다른 사이트맵 페이지에서 1회 이상 링크된다")
    void everySitemapUrlHasInboundLinkFromAnotherSitemapPage() throws Exception {
        List<String> urls = sitemapUrls();
        assertThat(urls).isNotEmpty();

        Map<String, Set<String>> inbound = new HashMap<>();
        for (String url : urls) {
            for (String target : internalLinks(url, render(url))) {
                if (!target.equals(url)) {
                    inbound.computeIfAbsent(target, k -> new HashSet<>()).add(url);
                }
            }
        }

        List<String> orphans = urls.stream().filter(u -> !inbound.containsKey(u)).toList();
        assertThat(orphans).as("다른 사이트맵 페이지에서 내부 링크가 하나도 없는 URL").isEmpty();
    }

    @Test
    @DisplayName("en 사이트맵 페이지의 내부 링크는 lang=en 을 유지한다 (로케일 누수 방지)")
    void englishPagesLinkEnglishUrls() throws Exception {
        List<String> leaks = new ArrayList<>();
        for (String url : sitemapUrls()) {
            if (!url.contains("lang=en")) {
                continue;
            }
            for (String target : internalLinks(url, render(url))) {
                boolean languageExplicit = target.contains("lang=");      // en 유지 또는 ?lang=ko 토글
                boolean oauthStart = target.startsWith(baseUrl + "/oauth2/"); // 카카오 인증 시작 — 로케일 무관
                if (!languageExplicit && !oauthStart) {
                    leaks.add(url + " -> " + target);
                }
            }
        }
        assertThat(leaks).as("en 페이지에서 ko URL 로 새는 내부 링크").isEmpty();
    }

    @Test
    @DisplayName("헤더 내비(홈 오버레이·일반 양쪽)가 모임 노하우 허브를 로케일별로 링크한다")
    void headerNavLinksGuidesHub() throws Exception {
        // 홈 = 오버레이(navOverlay), /insights/trends = 일반 — 같은 header fragment 의 두 렌더 분기
        for (String path : List.of("/", "/insights/trends")) {
            assertThat(navMenu(render(baseUrl + path))).as("%s ko", path)
                    .contains("href=\"/guides\"");
            assertThat(navMenu(render(baseUrl + path + "?lang=en"))).as("%s en", path)
                    .contains("href=\"/guides?lang=en\"");
        }
    }

    /** 사이트맵 XML 이 실제로 광고하는 loc 전량 (ko + {@code ?lang=en}). */
    private List<String> sitemapUrls() {
        List<String> urls = new ArrayList<>();
        Matcher m = LOC.matcher(sitemapService.generateSitemapXml());
        while (m.find()) {
            urls.add(m.group(1).replace("&amp;", "&"));
        }
        return urls;
    }

    private String render(String url) throws Exception {
        String pathAndQuery = url.substring(baseUrl.length());
        int q = pathAndQuery.indexOf('?');
        String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
        MockHttpServletRequestBuilder request = get(path.isEmpty() ? "/" : path);
        if (q >= 0) {
            for (String pair : pathAndQuery.substring(q + 1).split("&")) {
                String[] kv = pair.split("=", 2);
                request = request.param(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** 페이지의 href 를 절대 URL 로 정규화한 내부 링크 집합 (프래그먼트 제거, 외부·스킴 링크 제외). */
    private Set<String> internalLinks(String pageUrl, String html) {
        String pagePath = pageUrl.contains("?") ? pageUrl.substring(0, pageUrl.indexOf('?')) : pageUrl;
        Set<String> links = new HashSet<>();
        Matcher m = HREF.matcher(html);
        while (m.find()) {
            String href = m.group(1).replace("&amp;", "&");
            int hash = href.indexOf('#');
            if (hash >= 0) {
                href = href.substring(0, hash);
            }
            if (href.isEmpty()) {
                continue;
            }
            if (href.startsWith("?")) {
                links.add(pagePath + href);          // 언어 토글 @{''(lang=..)} — 현재 경로 기준
            } else if (href.startsWith("/") && !href.startsWith("//")) {
                links.add(baseUrl + href);
            } else if (href.startsWith(baseUrl)) {
                links.add(href);
            }
        }
        return links;
    }

    private static String navMenu(String html) {
        Matcher m = NAV_MENU.matcher(html);
        return m.find() ? m.group() : "";
    }
}
