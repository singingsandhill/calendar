package me.singingsandhill.calendar.common.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.StreamUtils;

/**
 * sitemap.xml 의 HTTP 레벨 계약과 robots.txt 정합성 가드.
 *
 * <p>{@code SitemapServiceHreflangTest} / {@code SitemapServiceWhitelistTest} 는 생성된 XML
 * 문자열만 본다. 여기서는 그 XML 이 실제로 서빙되는지, 그리고 광고하는 URL 이 정말 존재하고
 * robots.txt 에 막히지 않는지를 확인한다 — 사이트맵이 404 나 차단된 URL 을 광고하는 것이
 * 이 저장소의 대표적 색인 사고 유형이었다 (ADR common/seo/0002, 0005).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SitemapEndpointTest {

    private static final Pattern LOC = Pattern.compile("<loc>(.*?)</loc>");

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.base-url}")
    private String baseUrl;

    @Test
    @DisplayName("GET /sitemap.xml → 200 application/xml + 24시간 캐시")
    void sitemapEndpointServesXml() throws Exception {
        MvcResult result = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType())
                .startsWith(MediaType.APPLICATION_XML_VALUE);
        assertThat(result.getResponse().getHeader("Cache-Control"))
                .contains("max-age=86400");
        assertThat(result.getResponse().getContentAsString())
                .contains("<urlset")
                .contains("<loc>");
    }

    @Test
    @DisplayName("sitemap 이 광고하는 모든 loc 이 실제로 2xx 를 응답한다")
    void everySitemapUrlIsReachable() throws Exception {
        List<String> locs = fetchLocs();
        assertThat(locs).isNotEmpty();

        for (String loc : locs) {
            assertThat(loc).as("loc 은 app.base-url 로 시작해야 한다").startsWith(baseUrl);

            String pathAndQuery = loc.substring(baseUrl.length());
            int q = pathAndQuery.indexOf('?');
            String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
            MockHttpServletRequestBuilder request = get(path.isEmpty() ? "/" : path);
            if (q >= 0) {
                for (String pair : pathAndQuery.substring(q + 1).split("&")) {
                    String[] kv = pair.split("=", 2);
                    request = request.param(kv[0], kv.length > 1 ? kv[1] : "");
                }
            }

            int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
            assertThat(status).as("%s 응답 상태", loc).isBetween(200, 299);
        }
    }

    @Test
    @DisplayName("robots.txt 가 sitemap 의 URL 을 차단하지 않는다")
    void robotsTxtDoesNotBlockSitemapUrls() throws Exception {
        String robots = readRobotsTxt();
        List<String> disallow = directives(robots, "Disallow:");
        List<String> allow = directives(robots, "Allow:");

        for (String loc : fetchLocs()) {
            String pathAndQuery = loc.substring(baseUrl.length());
            String path = pathAndQuery.isEmpty() ? "/" : pathAndQuery;
            assertThat(isBlocked(path, disallow, allow))
                    .as("robots.txt 가 %s 를 차단한다", loc)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("robots.txt 의 Sitemap: 줄이 app.base-url 과 일치한다")
    void robotsTxtSitemapLineMatchesBaseUrl() throws Exception {
        assertThat(readRobotsTxt())
                .as("호스트/프로토콜이 어긋나면 GSC '리디렉션이 포함된 페이지' 재발 (ADR common/seo/0002)")
                .contains("Sitemap: " + baseUrl + "/sitemap.xml");
    }

    private List<String> fetchLocs() throws Exception {
        String xml = mockMvc.perform(get("/sitemap.xml"))
                .andReturn().getResponse().getContentAsString();
        List<String> locs = new ArrayList<>();
        Matcher m = LOC.matcher(xml);
        while (m.find()) {
            locs.add(m.group(1).replace("&amp;", "&"));
        }
        return locs;
    }

    private static String readRobotsTxt() throws IOException {
        try (var in = new ClassPathResource("static/robots.txt").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    /** {@code User-agent: *} 그룹이 하나뿐이므로 지시어를 종류별로 모아서 본다. */
    private static List<String> directives(String robots, String prefix) {
        List<String> values = new ArrayList<>();
        for (String line : robots.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * Google 해석 기준 — Allow/Disallow 중 <em>더 긴 패턴</em> 이 이긴다.
     * 길이가 같으면 Allow 우선. 매칭되는 Disallow 가 없으면 default-allow.
     */
    private static boolean isBlocked(String path, List<String> disallow, List<String> allow) {
        return longestMatch(path, disallow) > longestMatch(path, allow);
    }

    private static int longestMatch(String path, List<String> patterns) {
        int longest = -1;
        for (String pattern : patterns) {
            if (matches(pattern, path)) {
                longest = Math.max(longest, pattern.length());
            }
        }
        return longest;
    }

    /** robots.txt 패턴 매칭 — 접두사 매칭에 와일드카드 {@code *} 와 끝 앵커 {@code $} 만 지원. */
    private static boolean matches(String pattern, String path) {
        boolean anchored = pattern.endsWith("$");
        String body = anchored ? pattern.substring(0, pattern.length() - 1) : pattern;

        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        if (anchored) {
            regex.append('$');
        }
        return Pattern.compile(regex.toString()).matcher(path).find();
    }
}
