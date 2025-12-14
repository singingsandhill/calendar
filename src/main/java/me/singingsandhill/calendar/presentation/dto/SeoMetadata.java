package me.singingsandhill.calendar.presentation.dto;

/**
 * SEO 메타데이터를 캡슐화하는 DTO.
 * Thymeleaf 템플릿에서 메타 태그 생성에 사용됩니다.
 */
public record SeoMetadata(
    String title,
    String description,
    String keywords,
    String robots,
    String canonical,
    String ogType,
    String ogTitle,
    String ogDescription,
    String ogImage,
    String jsonLd
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String keywords = "약속 잡기, 일정 조율, 날짜 선택, 그룹 스케줄링, 캘린더, 모임 일정, date picker, scheduling";
        private String robots = "index, follow";
        private String canonical;
        private String ogType = "website";
        private String ogTitle;
        private String ogDescription;
        private String ogImage;
        private String jsonLd;

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

        public Builder jsonLd(String jsonLd) {
            this.jsonLd = jsonLd;
            return this;
        }

        public SeoMetadata build() {
            return new SeoMetadata(
                title,
                description,
                keywords,
                robots,
                canonical,
                ogType,
                ogTitle != null ? ogTitle : title,
                ogDescription != null ? ogDescription : description,
                ogImage,
                jsonLd
            );
        }
    }
}
