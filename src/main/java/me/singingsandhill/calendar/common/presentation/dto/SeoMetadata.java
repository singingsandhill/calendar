package me.singingsandhill.calendar.common.presentation.dto;

/**
 * SEO 메타데이터를 캡슐화하는 DTO.
 * Thymeleaf 템플릿에서 메타 태그 생성에 사용됩니다.
 *
 * 다국어:
 * - {@code canonical} 은 현재 요청 로케일에 맞춰 빌더에서 canonicalKo 또는 canonicalEn 으로 설정됨
 * - {@code canonicalKo}/{@code canonicalEn} 은 hreflang 태그 렌더링에 사용됨 (양쪽 항상 채움)
 * - {@code hreflangEnabled} 는 인덱싱 가능한 공개 페이지에만 true; noindex 페이지에선 false
 *
 * og:image 치수:
 * - {@code ogImageWidth}/{@code ogImageHeight} 는 ogImage 실물과 일치해야 함 (스크래퍼가 선언을 신뢰).
 *   빌더 기본값 1490×780 은 기본 이미지 og-image.png 의 실측 치수 — 다른 이미지를 쓰는 페이지는
 *   반드시 함께 오버라이드 (runner 페이지의 crew_logo.png 는 1280×720)
 */
public record SeoMetadata(
    String title,
    String description,
    String keywords,
    String robots,
    String canonical,
    String canonicalKo,
    String canonicalEn,
    String ogType,
    String ogTitle,
    String ogDescription,
    String ogImage,
    int ogImageWidth,
    int ogImageHeight,
    String ogLocale,
    String jsonLd,
    boolean adsEnabled,
    boolean hreflangEnabled
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String keywords;
        private String robots = "index, follow";
        private String canonical;
        private String canonicalKo;
        private String canonicalEn;
        private String ogType = "website";
        private String ogTitle;
        private String ogDescription;
        private String ogImage;
        private int ogImageWidth = 1490;
        private int ogImageHeight = 780;
        private String ogLocale;
        private String jsonLd;
        private boolean adsEnabled = false;
        private boolean hreflangEnabled = true;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder robots(String robots) {
            this.robots = robots;
            return this;
        }

        public Builder canonical(String canonical) {
            this.canonical = canonical;
            return this;
        }

        public Builder canonicalKo(String canonicalKo) {
            this.canonicalKo = canonicalKo;
            return this;
        }

        public Builder canonicalEn(String canonicalEn) {
            this.canonicalEn = canonicalEn;
            return this;
        }

        public Builder ogType(String ogType) {
            this.ogType = ogType;
            return this;
        }

        public Builder ogTitle(String ogTitle) {
            this.ogTitle = ogTitle;
            return this;
        }

        public Builder ogDescription(String ogDescription) {
            this.ogDescription = ogDescription;
            return this;
        }

        public Builder ogImage(String ogImage) {
            this.ogImage = ogImage;
            return this;
        }

        public Builder ogImageWidth(int ogImageWidth) {
            this.ogImageWidth = ogImageWidth;
            return this;
        }

        public Builder ogImageHeight(int ogImageHeight) {
            this.ogImageHeight = ogImageHeight;
            return this;
        }

        public Builder ogLocale(String ogLocale) {
            this.ogLocale = ogLocale;
            return this;
        }

        public Builder jsonLd(String jsonLd) {
            this.jsonLd = jsonLd;
            return this;
        }

        public Builder adsEnabled(boolean adsEnabled) {
            this.adsEnabled = adsEnabled;
            return this;
        }

        public Builder hreflangEnabled(boolean hreflangEnabled) {
            this.hreflangEnabled = hreflangEnabled;
            return this;
        }

        public SeoMetadata build() {
            return new SeoMetadata(
                title,
                description,
                keywords,
                robots,
                canonical,
                canonicalKo,
                canonicalEn,
                ogType,
                ogTitle != null ? ogTitle : title,
                ogDescription != null ? ogDescription : description,
                ogImage,
                ogImageWidth,
                ogImageHeight,
                ogLocale,
                jsonLd,
                adsEnabled,
                hreflangEnabled
            );
        }
    }
}
