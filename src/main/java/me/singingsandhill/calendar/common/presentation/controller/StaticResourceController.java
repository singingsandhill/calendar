package me.singingsandhill.calendar.common.presentation.controller;

import me.singingsandhill.calendar.common.application.service.SitemapService;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * SEO 정적 파일 제공 컨트롤러.
 * /{ownerId} 라우트보다 높은 우선순위로 SEO 관련 정적 파일을 제공합니다.
 */
@RestController
public class StaticResourceController {

    private final SitemapService sitemapService;
    private final MessageSource messageSource;

    public StaticResourceController(SitemapService sitemapService, MessageSource messageSource) {
        this.sitemapService = sitemapService;
        this.messageSource = messageSource;
    }

    @GetMapping(value = "/robots.txt")
    public ResponseEntity<Resource> robotsTxt() {
        Resource resource = new ClassPathResource("static/robots.txt");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .body(resource);
    }

    @GetMapping(value = "/ads.txt")
    public ResponseEntity<Resource> adsTxt() {
        Resource resource = new ClassPathResource("static/ads.txt");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .body(resource);
    }

    @GetMapping(value = "/1dfcb4404e1d4f6fae3423fd163f97b8.txt")
    public ResponseEntity<Resource> indexNowKey() {
        Resource resource = new ClassPathResource("static/1dfcb4404e1d4f6fae3423fd163f97b8.txt");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(resource);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapXml() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .body(sitemapService.generateSitemapXml());
    }

    @GetMapping(value = "/manifest.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> manifestJson(Locale locale) {
        String lang = "en".equals(locale.getLanguage()) ? "en-US" : "ko-KR";
        Map<String, Object> manifest = Map.ofEntries(
                Map.entry("name", messageSource.getMessage("seo.home.appName", null, locale)),
                Map.entry("short_name", messageSource.getMessage("seo.home.appAlternateName", null, locale)),
                Map.entry("description", messageSource.getMessage("seo.home.description", null, locale)),
                Map.entry("start_url", "/"),
                Map.entry("display", "standalone"),
                Map.entry("background_color", "#f5f5f5"),
                Map.entry("theme_color", "#3498db"),
                Map.entry("lang", lang),
                Map.entry("orientation", "portrait-primary"),
                Map.entry("categories", new String[]{"productivity", "utilities"}),
                Map.entry("icons", new Object[]{Map.of(
                        "src", "/favicon.svg",
                        "sizes", "any",
                        "type", "image/svg+xml",
                        "purpose", "any maskable"
                )})
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("Vary", "Accept-Language, Cookie")
                .body(manifest);
    }

    @GetMapping(value = "/favicon.svg", produces = "image/svg+xml")
    public Resource faviconSvg() {
        return new ClassPathResource("static/favicon.svg");
    }

    @GetMapping(value = "/favicon.ico")
    public ResponseEntity<Resource> faviconIco() {
        Resource resource = new ClassPathResource("static/favicon.svg");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(resource);
    }

    @GetMapping(value = "/og-image.svg")
    public ResponseEntity<Resource> ogImageSvg() {
        Resource resource = new ClassPathResource("static/og-image.svg");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(resource);
    }

    @GetMapping(value = "/apple-touch-icon.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> appleTouchIcon() {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(new ClassPathResource("static/apple-touch-icon.png"));
    }
}
