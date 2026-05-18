package me.singingsandhill.calendar.common.application.service;

import me.singingsandhill.calendar.common.application.dto.SitemapEntry;
import me.singingsandhill.calendar.common.infrastructure.config.IndexNowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IndexNowServiceTest {

    private static final String ENDPOINT = "https://api.indexnow.org/indexnow";
    private static final String HOST = "datedate.site";
    private static final String KEY = "1dfcb4404e1d4f6fae3423fd163f97b8";
    private static final String KEY_LOCATION = "https://datedate.site/" + KEY + ".txt";

    private SitemapService sitemapService;
    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        sitemapService = mock(SitemapService.class);
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
    }

    private IndexNowService newService(boolean enabled) {
        IndexNowProperties props = new IndexNowProperties(enabled, KEY, KEY_LOCATION, HOST, ENDPOINT);
        return new IndexNowService(props, sitemapService, builder.build());
    }

    @Test
    @DisplayName("enabled=false 면 HTTP 호출 없이 0 반환")
    void disabledNoOp() {
        IndexNowService service = newService(false);

        int submitted = service.submit(List.of("https://datedate.site/"));

        assertThat(submitted).isZero();
        mockServer.verify(); // 어떤 요청도 발생하지 않음
    }

    @Test
    @DisplayName("200 응답 시 제출 URL 수 반환 + 페이로드에 host/key/keyLocation/urlList 포함")
    void successReturnsCount() {
        mockServer.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.host").value(HOST))
                .andExpect(jsonPath("$.key").value(KEY))
                .andExpect(jsonPath("$.keyLocation").value(KEY_LOCATION))
                .andExpect(jsonPath("$.urlList[0]").value("https://datedate.site/a"))
                .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

        IndexNowService service = newService(true);

        int submitted = service.submit(List.of(
                "https://datedate.site/a",
                "https://datedate.site/b"
        ));

        assertThat(submitted).isEqualTo(2);
        mockServer.verify();
    }

    @Test
    @DisplayName("4xx 응답 시 0 반환 (fail-soft, throw 없음)")
    void clientErrorIsSwallowed() {
        mockServer.expect(requestTo(ENDPOINT))
                .andRespond(withRawStatus(HttpStatus.FORBIDDEN.value()));

        IndexNowService service = newService(true);

        int submitted = service.submit(List.of("https://datedate.site/a"));

        assertThat(submitted).isZero();
        mockServer.verify();
    }

    @Test
    @DisplayName("호스트가 다른 URL 은 필터링되고, 모두 다르면 호출 없이 0 반환")
    void otherHostUrlsFiltered() {
        IndexNowService service = newService(true);

        int submitted = service.submit(List.of(
                "https://other.example.com/a",
                "https://evil.com/b"
        ));

        assertThat(submitted).isZero();
        mockServer.verify(); // 호출 자체가 발생하지 않음
    }

    @Test
    @DisplayName("submitAll 은 SitemapService 의 bilingual 엔트리를 ko/en 두 URL 로 확장한다")
    void submitAllExpandsBilingual() {
        when(sitemapService.getSitemapEntries()).thenReturn(List.of(
                new SitemapEntry("https://datedate.site/", OffsetDateTime.now(), "monthly", "1.0", true),
                new SitemapEntry("https://datedate.site/runners", OffsetDateTime.now(), "weekly", "0.8", false)
        ));

        mockServer.expect(requestTo(ENDPOINT))
                .andExpect(jsonPath("$.urlList.length()").value(3))
                .andExpect(jsonPath("$.urlList[0]").value("https://datedate.site/"))
                .andExpect(jsonPath("$.urlList[1]").value("https://datedate.site/?lang=en"))
                .andExpect(jsonPath("$.urlList[2]").value("https://datedate.site/runners"))
                .andRespond(withSuccess());

        IndexNowService service = newService(true);

        int submitted = service.submitAll();

        assertThat(submitted).isEqualTo(3);
        mockServer.verify();
    }

    @Test
    @DisplayName("빈 URL 리스트는 호출 없이 0 반환")
    void emptyListNoOp() {
        IndexNowService service = newService(true);

        assertThat(service.submit(List.of())).isZero();
        mockServer.verify();
    }
}
