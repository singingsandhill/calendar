package me.singingsandhill.calendar.common.presentation.controller;

import me.singingsandhill.calendar.common.application.service.SitemapService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * SEO 정적 파일 제공 컨트롤러.
 * /{ownerId} 라우트보다 높은 우선순위로 SEO 관련 정적 파일을 제공합니다.
 */
@RestController
public class StaticResourceController {

    private final SitemapService sitemapService;

    public StaticResourceController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public Resource robotsTxt() {
        return new ClassPathResource("static/robots.txt");
    }

    @GetMapping(value = "/ads.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public Resource adsTxt() {
        return new ClassPathResource("static/ads.txt");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapXml() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .body(sitemapService.generateSitemapXml());
    }

    @GetMapping(value = "/manifest.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Resource manifestJson() {
        return new ClassPathResource("static/manifest.json");
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
}
