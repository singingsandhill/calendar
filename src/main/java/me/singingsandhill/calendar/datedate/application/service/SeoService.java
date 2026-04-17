package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

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
            .adsEnabled(true)
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
            .adsEnabled(true)
            .build();
    }

    /**
     * 활용 사례 페이지 SEO 메타데이터.
     */
    public SeoMetadata getUseCaseSeo(String slug, String title, String description) {
        String howToJsonLd = buildHowToJsonLd(slug, title, description);
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
                        "name": "활용 사례"
                    },
                    {
                        "@type": "ListItem",
                        "position": 3,
                        "name": "%s"
                    }
                ]
            },
            %s]
            """.formatted(title, description, baseUrl, slug, baseUrl, title, howToJsonLd);

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
            .adsEnabled(true)
            .build();
    }

    private String buildHowToJsonLd(String slug, String title, String description) {
        record HowToStep(int position, String name, String text) {}

        List<HowToStep> steps = switch (slug) {
            case "friend-meetup" -> List.of(
                new HowToStep(1, "DateDate 페이지 만들기",
                    "datedate.site에 접속해 그룹 ID(예: college-friends)를 입력하고 시작하기를 클릭합니다."),
                new HowToStep(2, "친구들에게 링크 공유하기",
                    "생성된 페이지 링크를 카카오톡, 문자 등으로 친구들에게 공유합니다. 앱 설치나 가입 없이 바로 참여할 수 있습니다."),
                new HowToStep(3, "모두가 가능한 날짜 확인하기",
                    "친구들이 각자 이름을 입력하고 가능한 날짜를 클릭하면, 가장 많이 겹치는 날짜가 자동으로 하이라이트됩니다.")
            );
            case "team-meeting" -> List.of(
                new HowToStep(1, "팀 일정 페이지 만들기",
                    "datedate.site에 접속해 팀 ID(예: dev-team)를 입력하고 시작하기를 클릭합니다."),
                new HowToStep(2, "팀원에게 링크 공유하기",
                    "생성된 링크를 슬랙, 이메일 등 업무 채널로 공유합니다. 팀원은 별도 가입 없이 참여 가능합니다."),
                new HowToStep(3, "최적 미팅 날짜 결정하기",
                    "팀원들이 가능한 날짜를 선택하면 공통 가능 날짜가 자동으로 강조됩니다. 장소와 회의실도 투표로 정할 수 있습니다.")
            );
            case "travel-planning" -> List.of(
                new HowToStep(1, "여행 일정 페이지 만들기",
                    "datedate.site에 접속해 여행 ID(예: trip-2025)를 입력하고 시작하기를 클릭합니다."),
                new HowToStep(2, "여행 멤버에게 링크 공유하기",
                    "생성된 링크를 여행 멤버들에게 공유합니다. 가입 없이 누구나 바로 일정에 참여할 수 있습니다."),
                new HowToStep(3, "모두가 가능한 여행 날짜 잡기",
                    "멤버들이 각자 가능한 날짜를 선택하면 최적 여행 날짜가 강조됩니다. 여행지와 맛집도 함께 투표해 결정할 수 있습니다.")
            );
            case "study-group" -> List.of(
                new HowToStep(1, "스터디 일정 페이지 만들기",
                    "datedate.site에 접속해 스터디 ID(예: java-study)를 입력하고 시작하기를 클릭합니다."),
                new HowToStep(2, "스터디원에게 링크 공유하기",
                    "생성된 링크를 스터디원들에게 공유합니다. 앱 설치나 로그인 없이 바로 일정에 참여할 수 있습니다."),
                new HowToStep(3, "정기 모임 날짜 조율하기",
                    "스터디원들이 가능한 날짜를 선택하면 공통 가능 날짜가 자동 표시됩니다. 장소와 공부할 메뉴도 함께 정할 수 있습니다.")
            );
            default -> List.of(
                new HowToStep(1, "DateDate 페이지 만들기",
                    "datedate.site에 접속해 원하는 ID를 입력하고 시작하기를 클릭합니다."),
                new HowToStep(2, "링크 공유하기",
                    "생성된 페이지 링크를 참여자들에게 공유합니다. 가입 없이 바로 참여할 수 있습니다."),
                new HowToStep(3, "날짜 선택 후 결과 확인하기",
                    "참여자들이 가능한 날짜를 선택하면 가장 많이 겹치는 날짜가 자동으로 강조됩니다.")
            );
        };

        StringBuilder stepsJson = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            HowToStep step = steps.get(i);
            stepsJson.append("""
                {
                    "@type": "HowToStep",
                    "position": %d,
                    "name": "%s",
                    "text": "%s"
                }""".formatted(step.position(), step.name(), step.text()));
            if (i < steps.size() - 1) stepsJson.append(",\n            ");
        }

        return """
            {
                "@context": "https://schema.org",
                "@type": "HowTo",
                "name": "%s 방법 - DateDate",
                "description": "%s",
                "step": [
                    %s
                ]
            }""".formatted(title, description, stepsJson);
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
            .adsEnabled(true)
            .build();
    }

    /**
     * 개인정보처리방침 페이지 SEO 메타데이터.
     */
    public SeoMetadata getPrivacySeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "개인정보처리방침 - DateDate",
                "description": "DateDate 개인정보처리방침",
                "url": "%s/privacy"
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
                        "name": "개인정보처리방침"
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("개인정보처리방침 | " + BRAND_NAME)
            .description("DateDate 개인정보처리방침. 수집하는 정보, 사용 목적, 제3자 광고, 쿠키 정책 등을 안내합니다.")
            .keywords("개인정보처리방침, 프라이버시, 개인정보보호")
            .robots("index, follow")
            .canonical(baseUrl + "/privacy")
            .ogType("website")
            .ogTitle("개인정보처리방침 | " + BRAND_NAME)
            .ogDescription("DateDate 개인정보처리방침")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .build();
    }

    /**
     * 이용약관 페이지 SEO 메타데이터.
     */
    public SeoMetadata getTermsSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebPage",
                "name": "이용약관 - DateDate",
                "description": "DateDate 서비스 이용약관",
                "url": "%s/terms"
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
                        "name": "이용약관"
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("이용약관 | " + BRAND_NAME)
            .description("DateDate 서비스 이용약관. 서비스 이용 조건, 면책 사항, 데이터 보관 정책 등을 안내합니다.")
            .keywords("이용약관, 서비스 약관, 이용 조건")
            .robots("index, follow")
            .canonical(baseUrl + "/terms")
            .ogType("website")
            .ogTitle("이용약관 | " + BRAND_NAME)
            .ogDescription("DateDate 서비스 이용약관")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .build();
    }

    /**
     * FAQ 독립 페이지 SEO 메타데이터.
     */
    public SeoMetadata getFaqSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "FAQPage",
                "mainEntity": [
                    {
                        "@type": "Question",
                        "name": "DateDate는 무료인가요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "완전 무료입니다. 가입, 설치, 결제 없이 URL 하나로 바로 사용할 수 있습니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "몇 명까지 참여할 수 있나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "일정 하나당 최대 8명이 참여할 수 있습니다. 각 참여자는 고유한 색상으로 표시됩니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "링크를 받은 참여자는 어떻게 하나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "링크를 열면 바로 일정 페이지로 연결됩니다. 이름을 선택하거나 추가한 뒤 캘린더에서 가능한 날짜를 클릭하고 저장하면 됩니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "데이터는 얼마나 보관되나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "생성된 일정 데이터는 서비스 서버에 보관됩니다. 불필요한 일정은 대시보드에서 직접 삭제할 수 있습니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "모바일에서도 사용할 수 있나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "네, 모든 기기에서 사용할 수 있습니다. 별도 앱 설치 없이 모바일 브라우저에서 바로 이용 가능합니다."
                        }
                    },
                    {
                        "@type": "Question",
                        "name": "장소와 메뉴 투표는 어떻게 하나요?",
                        "acceptedAnswer": {
                            "@type": "Answer",
                            "text": "일정 페이지 하단에 장소/메뉴 투표 섹션이 있습니다. 장소나 메뉴를 제안하고 투표 버튼을 눌러 참여하세요."
                        }
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
                        "name": "자주 묻는 질문"
                    }
                ]
            }]
            """.formatted(baseUrl);

        return SeoMetadata.builder()
            .title("자주 묻는 질문 (FAQ) | " + BRAND_NAME)
            .description("DateDate 사용법에 대한 자주 묻는 질문입니다. 무료 여부, 참여 방법, 데이터 보관, 모바일 사용 등을 안내합니다.")
            .keywords("DateDate FAQ, 약속 잡기 앱 질문, 그룹 일정 조율 방법, 날짜 선택 앱")
            .robots("index, follow")
            .canonical(baseUrl + "/faq")
            .ogType("website")
            .ogTitle("자주 묻는 질문 | " + BRAND_NAME)
            .ogDescription("DateDate 사용법, 무료 여부, 참여 방법 등 자주 묻는 질문을 모았습니다.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .build();
    }

    /**
     * 날짜 계산기 도구 페이지 SEO 메타데이터.
     */
    public SeoMetadata getDateDiffSeo() {
        String jsonLd = """
            [{
                "@context": "https://schema.org",
                "@type": "WebApplication",
                "name": "날짜 계산기 - DateDate",
                "description": "두 날짜 사이의 일수, 주수, 개월수를 계산합니다. 디데이(D-Day) 계산, 기념일까지 남은 날 계산에 활용하세요.",
                "url": "%s/tools/date-diff",
                "applicationCategory": "UtilitiesApplication",
                "operatingSystem": "All",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "KRW"
                }
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
                        "name": "도구"
                    },
                    {
                        "@type": "ListItem",
                        "position": 3,
                        "name": "날짜 계산기"
                    }
                ]
            }]
            """.formatted(baseUrl, baseUrl);

        return SeoMetadata.builder()
            .title("날짜 계산기 — 두 날짜 사이 일수 계산 | " + BRAND_NAME)
            .description("두 날짜 사이의 일수, 주수, 개월수를 무료로 계산하세요. 디데이(D-Day), 기념일, 프로젝트 기간 계산에 활용하세요.")
            .keywords("날짜 계산기, 디데이 계산, D-Day, 날짜 차이, 일수 계산, 기념일 계산, 두 날짜 사이")
            .robots("index, follow")
            .canonical(baseUrl + "/tools/date-diff")
            .ogType("website")
            .ogTitle("날짜 계산기 | " + BRAND_NAME)
            .ogDescription("두 날짜 사이의 일수, 주수, 개월수를 무료로 계산하세요.")
            .ogImage(baseUrl + DEFAULT_OG_IMAGE)
            .jsonLd(jsonLd)
            .adsEnabled(true)
            .build();
    }
}
