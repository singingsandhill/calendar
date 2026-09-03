package me.singingsandhill.calendar.common.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 공개 SEO 페이지 캐시 헤더 커버리지 가드 (AdSense 저가치 콘텐츠 진단 2026-08-17).
 *
 * <p>로케일 적응형(쿠키/Accept-Language 로 본문 언어가 갈리는) 공개 페이지는 공유 캐시가
 * 잘못된 언어 본문을 서빙하지 않도록 {@code Vary: Cookie, Accept-Language} 가 필수다.
 * {@code WebConfig.cacheControlInterceptor} 의 경로 집합에서 /about·/faq·/tools/date-diff
 * 가 누락돼 있던 회귀를 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicSeoCacheHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/guide", "/about", "/faq", "/tools/date-diff", "/guides", "/guides/how-to-pick-a-date"})
    @DisplayName("공개 SEO 페이지 → Vary: Cookie, Accept-Language + public 캐시")
    void publicSeoPage_hasVaryAndPublicCache(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "Cookie, Accept-Language"))
                .andExpect(header().string("Cache-Control", "public, max-age=3600, s-maxage=86400"));
    }
}
