package me.singingsandhill.calendar.common.application.service;

import me.singingsandhill.calendar.common.application.dto.SitemapEntry;
import me.singingsandhill.calendar.common.infrastructure.config.IndexNowProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IndexNow 프로토콜 (Bing/Yandex/Naver 등 호환) 으로 사이트맵 URL 변경을 능동 통보.
 *
 * <p>fail-soft: 어떤 실패도 throw 하지 않고 WARN 로그만. 스케줄러/배포 영향 차단.
 * Google 은 IndexNow 미참여이므로 Search Console 사이트맵과 별개의 보조 채널로만 동작.
 */
@Service
public class IndexNowService {

    private static final Logger log = LoggerFactory.getLogger(IndexNowService.class);

    private final IndexNowProperties properties;
    private final SitemapService sitemapService;
    private final RestClient restClient;

    public IndexNowService(IndexNowProperties properties,
                           SitemapService sitemapService,
                           RestClient indexNowRestClient) {
        this.properties = properties;
        this.sitemapService = sitemapService;
        this.restClient = indexNowRestClient;
    }

    /** 사이트맵의 모든 공개 URL (bilingual 은 ko/en 양쪽) 을 한 번에 제출. 제출 시도 URL 수 반환. */
    public int submitAll() {
        if (!properties.enabled()) {
            return 0;
        }
        List<String> urls = collectSitemapUrls();
        return submit(urls);
    }

    /** 외부 호출용. enabled 체크 + 호스트 필터링 후 한 번 POST. */
    public int submit(List<String> urls) {
        if (!properties.enabled()) {
            return 0;
        }
        if (urls == null || urls.isEmpty()) {
            return 0;
        }
        List<String> filtered = urls.stream()
                .filter(this::sameHost)
                .distinct()
                .toList();
        if (filtered.isEmpty()) {
            log.warn("IndexNow: no URL matches configured host={}, skipping", properties.host());
            return 0;
        }

        Map<String, Object> payload = Map.of(
                "host", properties.host(),
                "key", properties.key(),
                "keyLocation", properties.keyLocation(),
                "urlList", filtered
        );

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(URI.create(properties.endpoint()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { /* never throw */ })
                    .toBodilessEntity();

            int status = response.getStatusCode().value();
            logResponse(status, filtered.size());
            return status >= 200 && status < 300 ? filtered.size() : 0;
        } catch (Exception e) {
            log.warn("IndexNow submission failed: {}", e.toString());
            return 0;
        }
    }

    private List<String> collectSitemapUrls() {
        Set<String> urls = new LinkedHashSet<>();
        for (SitemapEntry entry : sitemapService.getSitemapEntries()) {
            urls.add(entry.loc());
            if (entry.bilingual()) {
                String loc = entry.loc();
                urls.add(loc + (loc.contains("?") ? "&" : "?") + "lang=en");
            }
        }
        return List.copyOf(urls);
    }

    private boolean sameHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && host.equalsIgnoreCase(properties.host());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void logResponse(int status, int count) {
        switch (status) {
            case 200, 202 -> log.info("IndexNow submitted {} urls, status={}", count, status);
            case 400 -> log.warn("IndexNow 400 Bad Request — payload format invalid");
            case 403 -> log.warn("IndexNow 403 Forbidden — key file not found or key mismatch at {}", properties.keyLocation());
            case 422 -> log.warn("IndexNow 422 Unprocessable — host or schema mismatch (host={})", properties.host());
            case 429 -> log.warn("IndexNow 429 Too Many Requests — back off submission frequency");
            default -> log.warn("IndexNow unexpected status={} for {} urls", status, count);
        }
    }
}
