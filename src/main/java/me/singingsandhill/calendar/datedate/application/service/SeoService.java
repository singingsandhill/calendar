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
     */
    public SeoMetadata getHomeSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebApplication",
                "name": "DateDate",
                "alternateName": "약속 잡기",
                "description": "링크 하나로 날짜, 장소, 메뉴까지 한번에 정하세요. 가입 없이 무료로 그룹 일정을 조율할 수 있습니다.",
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
            },
            {
                "@context": "https://schema.org",
                "@type": "Organization",
                "name": "DateDate",
                "url": "%s",
                "logo": "%s/og-image.png",
                "description": "여러명이서 쉽게 날짜 조율하기. 링크 하나로 날짜, 장소, 메뉴까지 한번에 정하세요."
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "홈",
                        "item": "%s/"
                    }
                ]
            },
            {
                "@context": "https://schema.org",
                "@type": "FAQPage",
                "mainEntity": [
                    {
                        "@type": "Question",
                        "name": "무료인가요? 가입이 필요한가요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "완전 무료, 가입 없이 바로 사용할 수 있습니다. ID 입력만으로 페이지가 생성되고, 참여자도 링크만으로 참여합니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "참여자가 날짜를 수정할 수 있나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "네. 드롭다운에서 이름을 선택하고 날짜를 다시 고른 뒤 저장하면 됩니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "장소나 메뉴 투표는 어떻게 하나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "일정 페이지 하단에서 장소·메뉴를 제안하고 투표하세요. 중복 투표 가능하며, 메뉴에는 배달앱 링크도 첨부할 수 있습니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "일정 데이터는 얼마나 보관되나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "삭제하지 않는 한 계속 보관됩니다. 대시보드에서 언제든 확인·관리 가능합니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "내 페이지 ID가 이미 사용 중이면?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "해당 페이지의 대시보드로 이동합니다. 숫자나 하이픈을 조합해 고유한 ID를 만드세요. 예: my-team-2025"
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "링크를 공유하면 누구나 참여할 수 있나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "네, 링크를 아는 사람은 누구나 참여할 수 있습니다. 신뢰할 수 있는 사람에게만 공유하세요."
                        }
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("DateDate - 약속 잡기 | 여러명이서 쉽게 날짜 조율하기")
            .description("링크 하나로 날짜, 장소, 메뉴까지 한번에 정하세요. 가입 없이 무료로 그룹 일정을 조율할 수 있습니다.")
            .keywords("약속 잡기, 일정 조율, 날짜 선택, 그룹 스케줄링, 캘린더, 모임 일정, date picker, scheduling, 회의 일정")
            .robots("index, follow")
            .canonical(baseUrl + "/")
            .ogType("website")
            .ogTitle("DateDate - 여러명이서 쉽게 날짜 조율하기")
            .ogDescription("카톡방에서 날짜 조율 그만. 링크 하나로 날짜·장소·메뉴를 한번에 정하세요.")
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

    /**
     * 사용 가이드 페이지 SEO 메타데이터.
     * HowTo 스키마로 Google 리치 결과 대상.
     */
    public SeoMetadata getGuideSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "HowTo",
                "name": "DateDate로 여러명 약속 잡기",
                "description": "DateDate를 사용하여 그룹 일정을 조율하는 방법을 5단계로 안내합니다.",
                "totalTime": "PT2M",
                "tool": {
                    "@type": "HowToTool",
                    "name": "웹 브라우저"
                },
                "step": [
                    {
                        "@type": "HowToStep",
                        "position": 1,
                        "name": "페이지 만들기",
                        "text": "datedate.site에 접속하여 원하는 ID를 입력하고 시작하기를 클릭합니다. 영문, 숫자, 하이픈으로 2~20자의 고유 ID를 만드세요.",
                        "url": "%s/guide#step-1"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 2,
                        "name": "일정 생성",
                        "text": "대시보드에서 월을 선택하면 해당 월의 일정 페이지가 자동으로 생성됩니다.",
                        "url": "%s/guide#step-2"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 3,
                        "name": "링크 공유",
                        "text": "생성된 일정 페이지 링크를 카카오톡, 슬랙, 이메일 등으로 참여자에게 공유합니다.",
                        "url": "%s/guide#step-3"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 4,
                        "name": "날짜 선택 & 투표",
                        "text": "참여자들이 이름을 입력하고 가능한 날짜를 클릭하여 선택합니다. 장소와 메뉴도 제안하고 투표할 수 있습니다.",
                        "url": "%s/guide#step-4"
                    },
                    {
                        "@type": "HowToStep",
                        "position": 5,
                        "name": "결과 확인 & 결정",
                        "text": "가장 많은 사람이 가능한 날짜, 가장 인기있는 장소와 메뉴를 확인하고 최종 약속을 결정합니다.",
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
                        "name": "홈",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "사용 가이드"
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("사용 가이드 - 여러명 약속 잡기 방법 | " + BRAND_NAME)
            .description("DateDate로 여러명이 약속을 잡는 방법을 5단계로 안내합니다. 무료 일정 조율 사이트로 모임 날짜, 장소, 메뉴를 한번에 정하세요.")
            .keywords("여러명 약속 잡기 방법, 무료 일정 조율 사이트, 모임 날짜 맞추기, 그룹 스케줄링 사용법, 약속 잡기 도구")
            .robots("index, follow")
            .canonical(baseUrl + "/guide")
            .ogType("article")
            .ogTitle("사용 가이드 - 여러명 약속 잡기 방법 | " + BRAND_NAME)
            .ogDescription("5단계로 쉽게! 링크 하나로 날짜·장소·메뉴까지 한번에 정하는 방법을 알아보세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }

    /**
     * 활용 사례 페이지 SEO 메타데이터.
     */
    public SeoMetadata getUseCaseSeo(String slug, String title, String description) {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "%s | DateDate",
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
                        "name": "홈",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "활용 사례",
                        "item": "%s/use-cases/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 3,
                        "name": "%s"
                    }
                ]
            }]
            """.formatted(title, description, baseUrl, slug, baseUrl, baseUrl, title);

        return SeoMetadata.builder()
            .title(title + " | " + BRAND_NAME + " 활용 사례")
            .description(description)
            .keywords(title + ", 약속 잡기, 일정 조율, " + BRAND_NAME)
            .robots("index, follow")
            .canonical(baseUrl + "/use-cases/" + slug)
            .ogType("article")
            .ogTitle(title + " | " + BRAND_NAME)
            .ogDescription(description)
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }

    /**
     * 인기 트렌드 & 이용 현황 통합 페이지 SEO 메타데이터.
     * 인기 장소/메뉴 순위와 서비스 이용 통계를 함께 보여주는 페이지입니다.
     */
    public SeoMetadata getInsightsTrendsSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "인기 트렌드 & 이용 현황 - DateDate",
                "description": "가장 인기 있는 약속 장소와 메뉴, 서비스 이용 통계를 확인하세요",
                "url": "%s/insights/trends"
            },
            {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "name": "홈",
                        "item": "%s/"
                    },
                    {
                        "@type": "ListItem",
                        "position": 2,
                        "name": "인기 트렌드 & 이용 현황"
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("인기 트렌드 & 이용 현황 | " + BRAND_NAME + " - 인기 장소, 메뉴 순위, 서비스 통계")
            .description("DateDate에서 가장 인기 있는 약속 장소와 메뉴 TOP 10을 확인하고, 서비스 이용 통계도 한눈에 살펴보세요. 실시간으로 업데이트되는 인기 순위와 투표 현황을 제공합니다.")
            .keywords("인기 장소 순위, 인기 메뉴 순위, 약속 장소 추천, 모임 장소 추천, 맛집 순위, 서비스 통계, 이용 현황")
            .robots("index, follow")
            .canonical(baseUrl + "/insights/trends")
            .ogType("website")
            .ogTitle("인기 트렌드 & 이용 현황 | " + BRAND_NAME)
            .ogDescription("DateDate에서 가장 인기 있는 약속 장소와 메뉴 TOP 10, 서비스 이용 통계를 확인하세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .build();
    }
}
