package me.singingsandhill.calendar.datedate.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;

/**
 * SEO 메타데이터 생성 서비스.
 * 각 페이지에 맞는 SEO 정보를 제공합니다.
 */
@Service
public class SeoService {

    @Value("${app.base-url:https://datedate.site}")
    private String baseUrl;

    private static final String DEFAULT_OG_IMAGE = "/og-image.png";
    private static final String BRAND_NAME = "DateDate";

    /**
     * 홈페이지(랜딩 페이지) SEO 메타데이터.
     * 유일하게 인덱싱되는 페이지입니다.
     */
    public SeoMetadata getHomeSeo() {
        String jsonLd = """
            {
                "@context": "https://schema.org",
                "@type": "WebApplication",
                "name": "DateDate",
                "alternateName": "약속 잡기",
                "description": "여러명이서 쉽게 날짜 조율하기 - Group scheduling made easy",
                "url": "%s",
                "applicationCategory": "SchedulingApplication",
                "operatingSystem": "All",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "KRW"
                },
                "inLanguage": "ko-KR",
                "publisher": {
                    "@type": "Organization",
                    "name": "DateDate",
                    "url": "%s"
                }
            }
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("DateDate - 약속 잡기 | 여러명이서 쉽게 날짜 조율하기")
            .description("DateDate로 그룹 일정을 쉽게 조율하세요. 참여자 초대, 가능한 날짜 선택, 최적의 약속 일정 찾기까지. 무료로 간편하게 사용하세요!")
            .keywords("약속 잡기, 일정 조율, 날짜 선택, 그룹 스케줄링, 캘린더, 모임 일정, date picker, scheduling, 회의 일정")
            .robots("index, follow")
            .canonical(baseUrl + "/")
            .ogType("website")
            .ogTitle("DateDate - 여러명이서 쉽게 날짜 조율하기")
            .ogDescription("그룹 일정 조율이 필요하세요? DateDate로 모두가 가능한 날짜를 쉽게 찾으세요. 무료 서비스!")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }

    /**
     * 시작 페이지 SEO 메타데이터.
     * 나만의 페이지 만들기 전용 페이지입니다.
     */
    public SeoMetadata getStartPageSeo() {
        return SeoMetadata.builder()
            .title("나만의 페이지 만들기 | " + BRAND_NAME)
            .description("DateDate에서 나만의 약속 조율 페이지를 만드세요. 회원가입 없이 간편하게 시작할 수 있습니다.")
            .keywords("약속 페이지 만들기, 일정 조율 페이지, 그룹 스케줄링, 무료 일정 관리")
            .robots("index, follow")
            .canonical(baseUrl + "/start")
            .ogType("website")
            .ogTitle("나만의 페이지 만들기 | " + BRAND_NAME)
            .ogDescription("DateDate로 그룹 일정을 쉽게 조율하세요. 무료로 간편하게!")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .build();
    }

    /**
     * 대시보드 페이지 SEO 메타데이터.
     * 사용자 생성 콘텐츠이므로 noindex 처리됩니다.
     */
    public SeoMetadata getDashboardSeo(String ownerId) {
        return SeoMetadata.builder()
            .title(ownerId + "님의 대시보드 | " + BRAND_NAME)
            .description(ownerId + "님의 일정 관리 페이지입니다.")
            .robots("noindex, nofollow")
            .canonical(baseUrl + "/" + ownerId)
            .ogType("website")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .build();
    }

    /**
     * 일정 뷰 페이지 SEO 메타데이터.
     * 사용자 생성 콘텐츠이므로 noindex 처리됩니다.
     */
    public SeoMetadata getScheduleSeo(String ownerId, int year, int month) {
        String title = String.format("%d년 %d월 일정 - %s | %s", year, month, ownerId, BRAND_NAME);
        String description = String.format("%s님의 %d년 %d월 일정 조율 페이지입니다. 참여자들이 가능한 날짜를 선택할 수 있습니다.",
            ownerId, year, month);

        return SeoMetadata.builder()
            .title(title)
            .description(description)
            .robots("noindex, nofollow")
            .canonical(baseUrl + "/" + ownerId + "/" + year + "/" + month)
            .ogType("article")
            .ogTitle(String.format("%s - %d년 %d월 일정", ownerId, year, month))
            .ogDescription("이 일정에 참여하여 가능한 날짜를 선택하세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .build();
    }

    /**
     * 인사이트 허브 페이지 SEO 메타데이터.
     * 통계와 트렌드 정보의 메인 허브 페이지입니다.
     */
    public SeoMetadata getInsightsHubSeo() {
        String jsonLd = """
            {
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "약속 인사이트 - DateDate",
                "description": "DateDate 사용자들의 약속 트렌드와 통계를 확인하세요",
                "url": "%s/insights",
                "isPartOf": {
                    "@type": "WebSite",
                    "name": "DateDate",
                    "url": "%s"
                }
            }
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("약속 인사이트 | " + BRAND_NAME + " - 트렌드와 통계")
            .description("DateDate 사용자들의 약속 트렌드를 확인하세요. 인기 장소, 인기 메뉴, 서비스 이용 통계를 한눈에 볼 수 있습니다. 약속 잡기의 최신 트렌드를 알아보세요.")
            .keywords("약속 트렌드, 인기 장소, 인기 메뉴, 모임 통계, 일정 분석, 약속 인사이트, 그룹 스케줄링 통계")
            .robots("index, follow")
            .canonical(baseUrl + "/insights")
            .ogType("website")
            .ogTitle("약속 인사이트 - 트렌드와 통계 | " + BRAND_NAME)
            .ogDescription("DateDate 사용자들의 약속 트렌드를 확인하세요. 인기 장소와 메뉴 순위를 알아보세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }

    /**
     * 인기 트렌드 페이지 SEO 메타데이터.
     * 인기 장소와 메뉴 순위를 보여주는 페이지입니다.
     */
    public SeoMetadata getInsightsTrendsSeo() {
        String jsonLd = """
            {
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "인기 트렌드 - DateDate",
                "description": "가장 인기 있는 약속 장소와 메뉴를 확인하세요",
                "url": "%s/insights/trends"
            }
            """.formatted(baseUrl);

        return SeoMetadata.builder()
            .title("인기 트렌드 | " + BRAND_NAME + " - 인기 장소 & 메뉴 순위")
            .description("DateDate에서 가장 인기 있는 약속 장소와 메뉴를 확인하세요. 실시간으로 업데이트되는 인기 순위와 투표 현황을 한눈에 볼 수 있습니다.")
            .keywords("인기 장소 순위, 인기 메뉴 순위, 약속 장소 추천, 모임 장소 추천, 맛집 순위, 트렌드 장소")
            .robots("index, follow")
            .canonical(baseUrl + "/insights/trends")
            .ogType("website")
            .ogTitle("인기 트렌드 - 장소 & 메뉴 순위 | " + BRAND_NAME)
            .ogDescription("DateDate에서 가장 인기 있는 약속 장소와 메뉴 TOP 10을 확인하세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }

    /**
     * 이용 현황 페이지 SEO 메타데이터.
     * 서비스 이용 통계를 보여주는 페이지입니다.
     */
    public SeoMetadata getInsightsStatsSeo() {
        String jsonLd = """
            {
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "이용 현황 - DateDate",
                "description": "DateDate 서비스 이용 통계를 확인하세요",
                "url": "%s/insights/stats"
            }
            """.formatted(baseUrl);

        return SeoMetadata.builder()
            .title("이용 현황 | " + BRAND_NAME + " - 서비스 통계")
            .description("DateDate 서비스 이용 현황을 확인하세요. 총 일정 수, 참여자 수, 장소 및 메뉴 투표 현황 등 다양한 통계 정보를 제공합니다.")
            .keywords("서비스 통계, 이용 현황, 약속 통계, 일정 통계, 참여자 통계, DateDate 통계")
            .robots("index, follow")
            .canonical(baseUrl + "/insights/stats")
            .ogType("website")
            .ogTitle("이용 현황 - 서비스 통계 | " + BRAND_NAME)
            .ogDescription("DateDate 서비스 이용 현황과 통계를 확인하세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }
}
