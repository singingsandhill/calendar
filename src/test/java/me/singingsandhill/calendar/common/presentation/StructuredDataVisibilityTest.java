package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.singingsandhill.calendar.common.application.service.SitemapService;

/**
 * 구조화 데이터-화면 정합 가드.
 *
 * <p>Google 구조화 데이터 정책: 화면에 보이지 않는 콘텐츠를 마크업하지 않는다(위반은 직접 조치 대상).
 * 2026-10-13 AdSense 진단에서 use-case 10 URL 이 FAQPage(질문·답 5쌍)와 HowTo(3단계)를 내보내면서
 * 화면에는 하나도 없었다(129 항목). 사이트맵의 모든 URL 을 실제로 렌더해, JSON-LD 의 FAQ 질문·답과
 * HowTo 단계 이름이 화면 텍스트에 있는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructuredDataVisibilityTest {

    private static final Pattern JSON_LD =
            Pattern.compile("(?is)<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>");
    private static final Pattern LOC = Pattern.compile("<loc>(.*?)</loc>");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SitemapService sitemapService;

    @Test
    @DisplayName("사이트맵 전 URL(ko·en) 에서 FAQPage 질문·답과 HowTo 단계 이름이 화면 텍스트에 있다")
    void faqAndHowToMarkupIsVisibleOnEverySitemapPage() throws Exception {
        // getSitemapEntries() 는 ko 기준 항목만 준다 — en(?lang=en) 은 XML 생성 때 덧붙으므로 발행되는 <loc> 을 그대로 쓴다
        List<String> locs = new ArrayList<>();
        Matcher loc = LOC.matcher(sitemapService.generateSitemapXml());
        while (loc.find()) {
            locs.add(HtmlUtils.htmlUnescape(loc.group(1)));
        }
        List<String> invisible = new ArrayList<>();

        for (String url : locs) {
            URI uri = URI.create(url);
            String html = mockMvc.perform(request(uri)).andReturn().getResponse().getContentAsString();
            String visible = visibleText(html);

            for (JsonNode node : jsonLdObjects(html)) {
                String type = node.path("@type").asText();
                if ("FAQPage".equals(type)) {
                    for (JsonNode qa : node.path("mainEntity")) {
                        requireVisible(visible, qa.path("name").asText(), uri, "FAQ 질문", invisible);
                        requireVisible(visible, qa.path("acceptedAnswer").path("text").asText(), uri, "FAQ 답", invisible);
                    }
                } else if ("HowTo".equals(type)) {
                    for (JsonNode step : node.path("step")) {
                        requireVisible(visible, step.path("name").asText(), uri, "HowTo 단계", invisible);
                    }
                }
            }
        }

        assertThat(locs).anyMatch(url -> url.endsWith("lang=en"));
        assertThat(invisible).isEmpty();
    }

    private static MockHttpServletRequestBuilder request(URI uri) {
        MockHttpServletRequestBuilder request = get(uri.getPath());
        if (uri.getQuery() != null) {
            for (String pair : uri.getQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                request.param(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        return request;
    }

    private List<JsonNode> jsonLdObjects(String html) throws Exception {
        List<JsonNode> objects = new ArrayList<>();
        Matcher m = JSON_LD.matcher(html);
        while (m.find()) {
            collect(objectMapper.readTree(m.group(1)), objects);
        }
        return objects;
    }

    private static void collect(JsonNode node, List<JsonNode> out) {
        if (node.isArray()) {
            node.forEach(child -> collect(child, out));
        } else if (node.has("@graph")) {
            collect(node.get("@graph"), out);
        } else if (node.isObject()) {
            out.add(node);
        }
    }

    private static void requireVisible(String visible, String marked, URI uri, String kind, List<String> invisible) {
        String text = normalize(HtmlUtils.htmlUnescape(marked.replaceAll("(?s)<[^>]+>", " ")));
        if (!text.isEmpty() && !visible.contains(text)) {
            invisible.add(uri + " " + kind + ": " + text);
        }
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return normalize(HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")));
    }

    private static String normalize(String s) {
        return s.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }
}
