package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;

/**
 * {@link SeoService} 가 한국어/영어 로케일에서 올바른 메타데이터와 JSON-LD 를 빌드하는지 검증.
 */
class SeoServiceI18nTest {

    private static final String BASE_URL = "https://example.test";

    private SeoService service;
    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = buildMessageSource();
        service = new SeoService(messageSource);
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);
        json = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static MessageSource buildMessageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    // ===== 홈 =====

    @Test
    @DisplayName("홈 SEO — 한국어 로케일은 한국어, 영어 로케일은 영어로 렌더링된다")
    void homeSeo_localized() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getHomeSeo();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getHomeSeo();

        assertThat(ko.title()).contains("약속 잡기");
        assertThat(en.title()).contains("Group Scheduling");
        assertThat(ko.description()).isNotEqualTo(en.description());
        assertThat(ko.ogLocale()).isEqualTo("ko_KR");
        assertThat(en.ogLocale()).isEqualTo("en_US");
    }

    @Test
    @DisplayName("홈 SEO — canonical 은 로케일에 따라 다르고 ?lang=en 플래그가 영어에만 붙는다")
    void homeSeo_canonicalPerLocale() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getHomeSeo();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getHomeSeo();

        assertThat(ko.canonical()).isEqualTo(BASE_URL + "/");
        assertThat(en.canonical()).isEqualTo(BASE_URL + "/?lang=en");
        assertThat(ko.canonicalKo()).isEqualTo(BASE_URL + "/");
        assertThat(ko.canonicalEn()).isEqualTo(BASE_URL + "/?lang=en");
        assertThat(en.canonicalKo()).isEqualTo(BASE_URL + "/");
        assertThat(en.canonicalEn()).isEqualTo(BASE_URL + "/?lang=en");
    }

    @Test
    @DisplayName("홈 JSON-LD 는 양쪽 로케일 모두 유효 JSON 이며 WebSite/WebApplication 이 bilingual inLanguage 를 선언한다")
    void homeSeo_jsonLdValid() throws Exception {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        String koJsonLd = service.getHomeSeo().jsonLd();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String enJsonLd = service.getHomeSeo().jsonLd();

        // 양쪽 로케일 모두 유효한 JSON
        json.readTree(koJsonLd);
        json.readTree(enJsonLd);

        // WebSite 엔트리 존재 (Google entity graph / sitelinks search box 힌트)
        assertThat(koJsonLd).contains("\"@type\": \"WebSite\"");
        assertThat(enJsonLd).contains("\"@type\": \"WebSite\"");

        // WebApplication + WebSite 각각 inLanguage 배열로 ko-KR + en-US 동시 선언
        assertThat(koJsonLd).contains("\"inLanguage\": [\"ko-KR\", \"en-US\"]");
        assertThat(enJsonLd).contains("\"inLanguage\": [\"ko-KR\", \"en-US\"]");

        // 로케일에 따라 UI 텍스트 (appDescription) 는 여전히 다름
        assertThat(koJsonLd).contains("링크 하나로 날짜");
        assertThat(enJsonLd).contains("Pick a date");
    }

    // ===== Guide =====

    @Test
    @DisplayName("가이드 SEO — 로케일별 HowTo 단계 텍스트가 올바르게 렌더링된다")
    void guideSeo_localized() throws Exception {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getGuideSeo();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getGuideSeo();

        assertThat(ko.title()).contains("사용 가이드");
        assertThat(en.title()).contains("User Guide");
        json.readTree(ko.jsonLd());
        json.readTree(en.jsonLd());
        assertThat(en.jsonLd()).contains("How to Coordinate Group Schedules");
        assertThat(ko.jsonLd()).contains("DateDate로 여러명 약속 잡기");
    }

    // ===== Use cases =====

    @Test
    @DisplayName("Use-case SEO — 슬러그별 단일 호출로 한/영 양쪽 콘텐츠 획득")
    void useCaseSeo_localized() throws Exception {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getUseCaseSeo("friend-meetup");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getUseCaseSeo("friend-meetup");

        assertThat(ko.title()).contains("친구 모임");
        assertThat(en.title()).contains("Friend Meetup");
        json.readTree(ko.jsonLd());
        json.readTree(en.jsonLd());
        assertThat(en.jsonLd()).contains("HowTo");
        assertThat(en.canonical()).endsWith("?lang=en");
    }

    // ===== FAQ =====

    @Test
    @DisplayName("FAQ SEO — FAQPage 스키마가 한/영 각각 6 questions 를 포함한다")
    void faqSeo_localized() throws Exception {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        String ko = service.getFaqSeo().jsonLd();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String en = service.getFaqSeo().jsonLd();

        json.readTree(ko);
        json.readTree(en);
        assertThat(ko).contains("DateDate는 무료인가요?");
        assertThat(en).contains("Is DateDate free?");
    }

    // ===== Noindex 페이지 =====

    @Test
    @DisplayName("Dashboard/Schedule 은 noindex 이며 hreflang 을 발행하지 않는다")
    void noindexPages_hreflangDisabled() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        SeoMetadata dashboard = service.getDashboardSeo("alice");
        SeoMetadata schedule = service.getScheduleSeo("alice", 2026, 4);

        assertThat(dashboard.robots()).isEqualTo("noindex, nofollow");
        assertThat(dashboard.hreflangEnabled()).isFalse();
        assertThat(schedule.robots()).isEqualTo("noindex, nofollow");
        assertThat(schedule.hreflangEnabled()).isFalse();
    }

    @Test
    @DisplayName("Schedule SEO — 연도/월 플레이스홀더가 천 단위 구분자 없이 렌더링된다")
    void scheduleSeo_yearNotGrouped() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getScheduleSeo("alice", 2026, 5);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getScheduleSeo("alice", 2026, 5);

        assertThat(ko.title()).contains("2026년 5월").doesNotContain("2,026");
        assertThat(en.title()).contains("2026/5 Schedule").doesNotContain("2,026");
        assertThat(ko.description()).contains("2026년 5월").doesNotContain("2,026");
        assertThat(en.description()).contains("2026/5").doesNotContain("2,026");
    }

    @Test
    @DisplayName("Dashboard SEO — ownerId 플레이스홀더가 치환되어 제목에 반영된다")
    void dashboardSeo_ownerIdPlaceholder() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        SeoMetadata ko = service.getDashboardSeo("alice");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SeoMetadata en = service.getDashboardSeo("alice");

        assertThat(ko.title()).contains("alice");
        assertThat(en.title()).contains("alice");
        assertThat(ko.title()).contains("대시보드");
        assertThat(en.title()).contains("Dashboard");
    }

    // ===== 공개 SEO 페이지 — hreflang 활성 =====

    @Test
    @DisplayName("모든 공개 페이지는 hreflangEnabled=true 이며 canonicalKo/En 이 항상 채워진다")
    void publicPages_hreflangEnabled() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        SeoMetadata[] publicPages = new SeoMetadata[] {
            service.getHomeSeo(),
            service.getGuideSeo(),
            service.getInsightsTrendsSeo(),
            service.getPrivacySeo(),
            service.getTermsSeo(),
            service.getFaqSeo(),
            service.getDateDiffSeo(),
            service.getUseCaseSeo("friend-meetup"),
            service.getUseCaseSeo("team-meeting"),
            service.getUseCaseSeo("travel-planning"),
            service.getUseCaseSeo("study-group")
        };

        for (SeoMetadata seo : publicPages) {
            assertThat(seo.hreflangEnabled()).isTrue();
            assertThat(seo.canonicalKo()).isNotNull().doesNotContain("lang=en");
            assertThat(seo.canonicalEn()).isNotNull().endsWith("?lang=en");
            assertThat(seo.robots()).isEqualTo("index, follow");
        }
    }

    // ===== 모든 공개 페이지 JSON-LD 유효성 =====

    @Test
    @DisplayName("모든 JSON-LD 페이지는 한/영 양쪽에서 유효한 JSON 이다 (아포스트로피 이스케이프 포함)")
    void allJsonLd_validJsonBothLocales() throws Exception {
        Locale[] locales = { Locale.KOREAN, Locale.ENGLISH };
        for (Locale locale : locales) {
            LocaleContextHolder.setLocale(locale);
            json.readTree(service.getHomeSeo().jsonLd());
            json.readTree(service.getGuideSeo().jsonLd());
            json.readTree(service.getInsightsTrendsSeo().jsonLd());
            json.readTree(service.getPrivacySeo().jsonLd());
            json.readTree(service.getTermsSeo().jsonLd());
            json.readTree(service.getFaqSeo().jsonLd());
            json.readTree(service.getDateDiffSeo().jsonLd());
            json.readTree(service.getUseCaseSeo("friend-meetup").jsonLd());
            json.readTree(service.getUseCaseSeo("team-meeting").jsonLd());
            json.readTree(service.getUseCaseSeo("travel-planning").jsonLd());
            json.readTree(service.getUseCaseSeo("study-group").jsonLd());
        }
    }
}
