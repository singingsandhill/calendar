package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.domain.guide.GuideSlug;
import me.singingsandhill.calendar.datedate.domain.guide.GuideSlugs;
import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * 색인 페이지 {@code <title>} 의 SERP 예산 가드 (ADR common/seo/0014).
 *
 * <p>Google 은 제목을 약 600px 에서 자른다. 한글 글자는 라틴 글자의 약 2배 폭이라 문자 수 상한을
 * 로케일별로 둔다 — 브랜드 접미 {@code " | DateDate"} 를 포함해 ko 35자 / en 60자. 기사처럼 h1 이 긴
 * 페이지는 {@code guides.article.<slug>.metaTitle} 로 SERP 제목만 따로 짧게 둔다.
 */
class SerpTitleBudgetTest {

    static final int KO_MAX = 35;
    static final int EN_MAX = 60;

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("모든 색인 페이지의 <title> 이 로케일별 SERP 예산 안이다 (ko 35자 / en 60자, 브랜드 접미 포함)")
    void indexablePages_titleWithinSerpBudget() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        SeoService service = new SeoService(ms);
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test");

        List<String> over = new ArrayList<>();
        for (Locale locale : new Locale[] {Locale.KOREAN, Locale.ENGLISH}) {
            LocaleContextHolder.setLocale(locale);
            int max = Locale.KOREAN.equals(locale) ? KO_MAX : EN_MAX;

            List<SeoMetadata> pages = new ArrayList<>(List.of(
                    service.getHomeSeo(),
                    service.getGuideSeo(),
                    service.getInsightsTrendsSeo(true),
                    service.getAboutSeo(),
                    service.getPrivacySeo(),
                    service.getTermsSeo(),
                    service.getFaqSeo(),
                    service.getDateDiffSeo(),
                    service.getUseCasesIndexSeo(),
                    service.getGuidesIndexSeo()));
            for (String slug : UseCaseSlugs.ALL) {
                pages.add(service.getUseCaseSeo(slug));
            }
            for (GuideSlug g : GuideSlugs.ALL) {
                pages.add(service.getGuideArticleSeo(g.slug()));
            }

            for (SeoMetadata seo : pages) {
                if (seo.title().length() > max) {
                    over.add("[%s] %d > %d  %s  (%s)".formatted(
                            locale, seo.title().length(), max, seo.title(), seo.canonical()));
                }
            }
        }

        assertThat(over).as("SERP 예산 초과 <title>").isEmpty();
    }
}
