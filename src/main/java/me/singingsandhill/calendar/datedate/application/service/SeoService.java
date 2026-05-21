package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;

/**
 * 페이지별 SEO 메타데이터를 로케일에 맞춰 빌드한다.
 *
 * 모든 문자열은 {@code messages[_en].properties} 에서 읽어오며, 한국어 canonical URL 은
 * {@code baseUrl + path}, 영어 canonical URL 은 {@code baseUrl + path + "?lang=en"} 형태로
 * 구성되어 hreflang 이 중복 인덱싱 없이 언어별로 나뉘도록 한다.
 */
@Service
public class SeoService {

    @Value("${app.base-url:https://datedate.site}")
    private String baseUrl;

    private static final String DEFAULT_OG_IMAGE = "/og-image.png";
    private static final String BRAND_NAME = "DateDate";

    private final MessageSource messageSource;

    public SeoService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // ===== 로케일/메시지/URL 헬퍼 =====

    private Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }

    private boolean isEnglish() {
        return "en".equalsIgnoreCase(currentLocale().getLanguage());
    }

    private String m(String key) {
        return messageSource.getMessage(key, null, currentLocale());
    }

    private String m(String key, Object... args) {
        return messageSource.getMessage(key, args, currentLocale());
    }

    /** 키가 없거나 값이 빈 문자열이면 빈 문자열을 반환한다. JSON-LD 등 선택적 블록 빌드에 사용. */
    private String mOrEmpty(String key) {
        return messageSource.getMessage(key, null, "", currentLocale());
    }

    /** {@link #m(String, Object...)} 결과를 JSON 문자열에 안전하게 끼워넣을 수 있도록 이스케이프. */
    private String mJson(String key) {
        return jsonEscape(m(key));
    }

    private String mJson(String key, Object... args) {
        return jsonEscape(m(key, args));
    }

    /** JSON 문자열 값에 허용되지 않는 문자를 이스케이프한다 (RFC 8259 기준). */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private String canonicalKo(String path) {
        return baseUrl + path;
    }

    private String canonicalEn(String path) {
        String url = baseUrl + path;
        return url + (url.contains("?") ? "&" : "?") + "lang=en";
    }

    private String currentCanonical(String path) {
        return isEnglish() ? canonicalEn(path) : canonicalKo(path);
    }

    private String ogLocale() {
        return isEnglish() ? "en_US" : "ko_KR";
    }

    private String inLanguage() {
        return m("seo.common.inLanguage");
    }

    private String priceCurrency() {
        return m("seo.common.priceCurrency");
    }

    // ===== 페이지별 SEO =====

    /** 홈페이지 (랜딩). */
    public SeoMetadata getHomeSeo() {
        String path = "/";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebApplication",
                "name": "%s",
                "alternateName": ["약속 잡기", "Group Scheduling"],
                "description": "%s",
                "url": "%s",
                "applicationCategory": "SchedulingApplication",
                "operatingSystem": "All",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "%s"
                },
                "inLanguage": ["ko-KR", "en-US"],
                "publisher": {
                    "@type": "Organization",
                    "name": "%s",
                    "url": "%s"
                }
            },
            {
                "@context": "https://schema.org",
                "@type": "WebSite",
                "name": "%s",
                "alternateName": ["약속 잡기", "Group Scheduling"],
                "url": "%s",
                "inLanguage": ["ko-KR", "en-US"],
                "publisher": {
                    "@type": "Organization",
                    "name": "%s",
                    "url": "%s"
                }
            },
            {
                "@context": "https://schema.org",
                "@type": "Organization",
                "name": "%s",
                "url": "%s",
                "logo": "%s/og-image.png",
                "description": "%s"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    }
                ]
            },
            {
                "@context": "https://schema.org",
                "@type": "FAQPage",
                "mainEntity": [
                    %s
                ]
            }]
            """.formatted(
                mJson("seo.home.appName"),
                mJson("seo.home.appDescription"),
                baseUrl,
                priceCurrency(),
                mJson("seo.home.appName"),
                baseUrl,
                mJson("seo.home.appName"),
                baseUrl,
                mJson("seo.home.appName"),
                baseUrl,
                mJson("seo.home.appName"),
                baseUrl,
                baseUrl,
                mJson("seo.home.orgDescription"),
                mJson("seo.breadcrumb.home"),
                baseUrl,
                buildFaqMainEntity(6, "seo.home.faq")
            );

        return SeoMetadata.builder()
            .title(m("seo.home.title"))
            .description(m("seo.home.description"))
            .keywords(m("seo.home.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m("seo.home.ogTitle"))
            .ogDescription(m("seo.home.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .hreflangEnabled(true)
            .build();
    }

    /** 대시보드 (UGC, noindex). */
    public SeoMetadata getDashboardSeo(String ownerId) {
        String path = "/" + ownerId;
        return SeoMetadata.builder()
            .title(m("seo.dashboard.title", ownerId))
            .description(m("seo.dashboard.description", ownerId))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }

    /** 일정 뷰 (UGC, noindex). */
    public SeoMetadata getScheduleSeo(String ownerId, int year, int month) {
        String path = "/" + ownerId + "/" + year + "/" + month;
        return SeoMetadata.builder()
            .title(m("seo.schedule.title", ownerId, year, month))
            .description(m("seo.schedule.description", ownerId, year, month))
            .robots("noindex, nofollow")
            .canonical(canonicalKo(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("article")
            .ogTitle(m("seo.schedule.ogTitle", ownerId, year, month))
            .ogDescription(m("seo.schedule.ogDescription", ownerId, year, month))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .hreflangEnabled(false)
            .build();
    }

    /** 사용 가이드 (HowTo). */
    public SeoMetadata getGuideSeo() {
        String path = "/guide";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "HowTo",
                "name": "%s",
                "description": "%s",
                "totalTime": "PT2M",
                "tool": {
                    "@type": "HowToTool",
                    "name": "%s"
                },
                "step": [
                    {
                        "@type": "HowToStep",
                        "position": 1,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-1"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 2,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-2"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 3,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-3"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 4,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-4"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 5,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-5"
                    }
                ]
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                mJson("seo.guide.howTo.name"),
                mJson("seo.guide.howTo.description"),
                mJson("seo.guide.howTo.tool"),
                mJson("seo.guide.step1.name"), mJson("seo.guide.step1.text"), baseUrl,
                mJson("seo.guide.step2.name"), mJson("seo.guide.step2.text"), baseUrl,
                mJson("seo.guide.step3.name"), mJson("seo.guide.step3.text"), baseUrl,
                mJson("seo.guide.step4.name"), mJson("seo.guide.step4.text"), baseUrl,
                mJson("seo.guide.step5.name"), mJson("seo.guide.step5.text"), baseUrl,
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.guide")
            );

        return SeoMetadata.builder()
            .title(m("seo.guide.title"))
            .description(m("seo.guide.description"))
            .keywords(m("seo.guide.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("article")
            .ogTitle(m("seo.guide.ogTitle"))
            .ogDescription(m("seo.guide.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .hreflangEnabled(true)
            .build();
    }

    /** 활용 사례 (슬러그별). */
    public SeoMetadata getUseCaseSeo(String slug) {
        String path = "/use-cases/" + slug;
        String title = m("seo.useCase." + slug + ".title");
        String description = m("seo.useCase." + slug + ".description");
        String howToJsonLd = buildUseCaseHowToJsonLd(slug, title, description);

        // FAQPage 블록은 슬러그가 FAQ 콘텐츠를 갖고 있을 때만 추가. q1 이 비어 있으면 슬러그가 아직
        // 5섹션 톤 전환을 거치지 않은 상태로 보고 FAQPage 를 emit 하지 않는다 — 빈 Q&A 의 SEO 손상 방지.
        String faqJsonLd = buildUseCaseFaqJsonLd(slug);
        String trailingObjects = faqJsonLd.isEmpty()
                ? howToJsonLd
                : howToJsonLd + ",\n            " + faqJsonLd;

        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "%s | %s",
                "description": "%s",
                "url": "%s/use-cases/%s"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    },
                    {
                        "@type": "ListItem",
                        "position": 3,
                        "name": "%s"
                    }
                ]
            },
            %s]
            """.formatted(
                jsonEscape(title), BRAND_NAME,
                jsonEscape(description),
                baseUrl, slug,
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.useCases"),
                jsonEscape(title),
                trailingObjects
            );

        return SeoMetadata.builder()
            .title(title + " | " + BRAND_NAME)
            .description(description)
            .keywords(title + ", " + m("seo.home.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("article")
            .ogTitle(title + " | " + BRAND_NAME)
            .ogDescription(description)
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .hreflangEnabled(true)
            .build();
    }

    /**
     * 슬러그의 FAQ 콘텐츠가 있으면 FAQPage JSON-LD 객체를 반환, 없으면 빈 문자열.
     *
     * <p>q1 ~ q5 까지 5개 Q&A 의 첫 번째 (q1) 만 확인 — 콘텐츠 작성 시 q1~q5 / a1~a5 가 함께 채워지는 것을
     * 전제로 한다. 부분 채움은 콘텐츠 검수 단계에서 잡힌다.
     */
    private String buildUseCaseFaqJsonLd(String slug) {
        String prefix = "seo.useCase." + slug + ".section.faq";
        if (mOrEmpty(prefix + ".q1").isEmpty()) {
            return "";
        }
        return """
            {
                "@context": "https://schema.org",
                "@type": "FAQPage",
                "mainEntity": [
                    %s
                ]
            }""".formatted(buildFaqMainEntity(5, prefix));
    }

    private String buildUseCaseHowToJsonLd(String slug, String title, String description) {
        StringBuilder stepsJson = new StringBuilder();
        List<Integer> positions = List.of(1, 2, 3);
        for (int i = 0; i < positions.size(); i++) {
            int pos = positions.get(i);
            stepsJson.append("""
                {
                    "@type": "HowToStep",
                    "position": %d,
                    "name": "%s",
                    "text": "%s"
                }""".formatted(
                    pos,
                    mJson("seo.useCase." + slug + ".step" + pos + ".name"),
                    mJson("seo.useCase." + slug + ".step" + pos + ".text")
                ));
            if (i < positions.size() - 1) stepsJson.append(",\n            ");
        }

        return """
            {
                "@context": "https://schema.org",
                "@type": "HowTo",
                "name": "%s",
                "description": "%s",
                "step": [
                    %s
                ]
            }""".formatted(
                mJson("seo.useCase.howTo.nameFormat", title),
                jsonEscape(description),
                stepsJson
            );
    }

    /**
     * 인기 트렌드 & 이용 현황 — 데이터가 있는 일반 케이스.
     *
     * <p>레거시 호출자 호환을 위한 무인자 버전. 새 코드는 {@link #getInsightsTrendsSeo(boolean)} 를 써서
     * 빈 데이터 시 noindex 로 강등할 것.
     */
    public SeoMetadata getInsightsTrendsSeo() {
        return getInsightsTrendsSeo(true);
    }

    /**
     * 인기 트렌드 & 이용 현황.
     *
     * <p>{@code hasData=false} (인기 데이터·통계 모두 0) 인 경우 PP-Full 의 *콘텐츠가 거의 없는 화면*
     * 신호를 회피하기 위해 robots 를 {@code noindex, follow} 로, 광고를 OFF 로, hreflang 도 비활성으로
     * 강등한다. 데이터가 충분히 쌓인 다음 자연 색인을 유도.
     */
    public SeoMetadata getInsightsTrendsSeo(boolean hasData) {
        String path = "/insights/trends";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "%s",
                "description": "%s",
                "url": "%s/insights/trends"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                mJson("seo.insights.webPageName"),
                mJson("seo.insights.webPageDescription"),
                baseUrl,
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.insights")
            );

        return SeoMetadata.builder()
            .title(m("seo.insights.title"))
            .description(m("seo.insights.description"))
            .keywords(m("seo.insights.keywords"))
            .robots(hasData ? "index, follow" : "noindex, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m("seo.insights.ogTitle"))
            .ogDescription(m("seo.insights.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(hasData)
            .hreflangEnabled(hasData)
            .build();
    }

    /**
     * 서비스 소개 페이지 (/about).
     *
     * <p>JSON-LD 는 {@code AboutPage} + {@code Organization} (운영자/연락처/언어) +
     * {@code BreadcrumbList} 3개 객체를 emit. AdSense 의 *게시자 신원 명확성* 신호.
     * 광고는 OFF — 행동/안내 페이지.
     */
    public SeoMetadata getAboutSeo() {
        String path = "/about";
        String contactUrl = "https://docs.google.com/forms/d/e/1FAIpQLSd_CtragyTvcclHy7MRITgDgnh43pnnItONJRzxJ_kXJOBrnQ/viewform?usp=publish-editor";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "AboutPage",
                "name": "%s",
                "description": "%s",
                "url": "%s%s",
                "inLanguage": "%s",
                "mainEntity": {
                    "@type": "Organization",
                    "name": "%s",
                    "url": "%s",
                    "logo": "%s%s",
                    "description": "%s",
                    "contactPoint": {
                        "@type": "ContactPoint",
                        "url": "%s",
                        "contactType": "customer support",
                        "availableLanguage": ["Korean", "English"]
                    }
                }
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                mJson("seo.about.webPageName"),
                mJson("seo.about.webPageDescription"),
                baseUrl, path,
                inLanguage(),
                BRAND_NAME,
                baseUrl,
                baseUrl, DEFAULT_OG_IMAGE,
                mJson("seo.about.description"),
                contactUrl,
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.about")
            );

        return SeoMetadata.builder()
            .title(m("seo.about.title"))
            .description(m("seo.about.description"))
            .keywords(m("seo.about.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m("seo.about.ogTitle"))
            .ogDescription(m("seo.about.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(false)
            .hreflangEnabled(true)
            .build();
    }

    /** 개인정보처리방침. */
    public SeoMetadata getPrivacySeo() {
        return buildSimpleWebPageSeo(
            "/privacy",
            "seo.privacy",
            "seo.breadcrumb.privacy"
        );
    }

    /** 이용약관. */
    public SeoMetadata getTermsSeo() {
        return buildSimpleWebPageSeo(
            "/terms",
            "seo.terms",
            "seo.breadcrumb.terms"
        );
    }

    /** WebPage + BreadcrumbList 만 들어가는 간단 페이지 공통 빌더. */
    private SeoMetadata buildSimpleWebPageSeo(String path, String prefix, String breadcrumbKey) {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "%s",
                "description": "%s",
                "url": "%s%s"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                mJson(prefix + ".webPageName"),
                mJson(prefix + ".webPageDescription"),
                baseUrl, path,
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson(breadcrumbKey)
            );

        return SeoMetadata.builder()
            .title(m(prefix + ".title"))
            .description(m(prefix + ".description"))
            .keywords(m(prefix + ".keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m(prefix + ".ogTitle"))
            .ogDescription(m(prefix + ".ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            // privacy/terms 는 PP-Full 의 "행동 목적 화면" 보수적 해석에 따라 광고 OFF.
            .adsEnabled(false)
            .hreflangEnabled(true)
            .build();
    }

    /** FAQ 독립 페이지 (FAQPage 스키마). */
    public SeoMetadata getFaqSeo() {
        String path = "/faq";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "FAQPage",
                "mainEntity": [
                    %s
                ]
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                buildFaqMainEntity(6, "seo.faq"),
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.faq")
            );

        return SeoMetadata.builder()
            .title(m("seo.faq.title"))
            .description(m("seo.faq.description"))
            .keywords(m("seo.faq.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m("seo.faq.ogTitle"))
            .ogDescription(m("seo.faq.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .hreflangEnabled(true)
            .build();
    }

    /** FAQPage.mainEntity 배열 본문을 빌드한다 (공통). */
    private String buildFaqMainEntity(int count, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append("""
                {
                    "@type": "Question",
                    "name": "%s",
                    "acceptedAnswer": {
                        "@type": "Answer",
                        "text": "%s"
                    }
                }""".formatted(
                    mJson(prefix + ".q" + i),
                    mJson(prefix + ".a" + i)
                ));
            if (i < count) sb.append(",\n            ");
        }
        return sb.toString();
    }

    /** 날짜 계산기 도구. */
    public SeoMetadata getDateDiffSeo() {
        String path = "/tools/date-diff";
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebApplication",
                "name": "%s",
                "description": "%s",
                "url": "%s/tools/date-diff",
                "applicationCategory": "UtilitiesApplication",
                "operatingSystem": "All",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "%s"
                },
                "inLanguage": "%s"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "%s",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "%s"
                    },
                    {
                        "@type": "ListItem",
                        "position": 3,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(
                mJson("seo.dateDiff.appName"),
                mJson("seo.dateDiff.appDescription"),
                baseUrl,
                priceCurrency(),
                inLanguage(),
                mJson("seo.breadcrumb.home"), baseUrl,
                mJson("seo.breadcrumb.tools"),
                mJson("seo.breadcrumb.dateDiff")
            );

        return SeoMetadata.builder()
            .title(m("seo.dateDiff.title"))
            .description(m("seo.dateDiff.description"))
            .keywords(m("seo.dateDiff.keywords"))
            .robots("index, follow")
            .canonical(currentCanonical(path))
            .canonicalKo(canonicalKo(path))
            .canonicalEn(canonicalEn(path))
            .ogType("website")
            .ogTitle(m("seo.dateDiff.ogTitle"))
            .ogDescription(m("seo.dateDiff.ogDescription"))
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .ogLocale(ogLocale())
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .hreflangEnabled(true)
            .build();
    }
}
