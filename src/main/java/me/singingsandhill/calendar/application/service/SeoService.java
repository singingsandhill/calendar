package me.singingsandhill.calendar.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import me.singingsandhill.calendar.presentation.dto.SeoMetadata;

/**
 * SEO 메타데이터 생성 서비스.
 * 각 페이지에 맞는 SEO 정보를 제공합니다.
 */
@Service
public class SeoService {

    @Value("${app.base-url:http://datedate.site}")
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
}
