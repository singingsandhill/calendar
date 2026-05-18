package me.singingsandhill.calendar.common.infrastructure.scheduler;

import me.singingsandhill.calendar.common.application.service.IndexNowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 03:30 KST 사이트맵 URL 들을 IndexNow 로 제출.
 *
 * <p>{@code indexnow.enabled=true} 일 때만 빈 등록.
 * 사이트맵은 변경 빈도가 낮고 IndexNow 는 동일 URL 재제출에 관대하므로 일 1회로 충분.
 */
@Component
@ConditionalOnProperty(name = "indexnow.enabled", havingValue = "true")
public class IndexNowScheduler {

    private final IndexNowService indexNowService;

    public IndexNowScheduler(IndexNowService indexNowService) {
        this.indexNowService = indexNowService;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void submitDaily() {
        indexNowService.submitAll();
    }
}
