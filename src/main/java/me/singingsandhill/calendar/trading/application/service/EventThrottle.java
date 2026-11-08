package me.singingsandhill.calendar.trading.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 같은 사유의 이벤트를 창(window)당 1회로 요약한다 — 1분 루프에서 같은 스킵·차단이 반복되면 이벤트 원장이 스팸으로
 * 덮인다(10/11 밤 REBALANCE_SKIPPED 162회). 인메모리라 재시작하면 첫 이벤트부터 다시 기록한다.
 */
final class EventThrottle {

    private final Duration window;
    private final Map<String, Instant> lastRecordedAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> suppressed = new ConcurrentHashMap<>();

    EventThrottle(Duration window) {
        this.window = window;
    }

    /**
     * @return 기록해야 하면 직전 기록 이후 생략된 횟수(0 이상), 창 안이라 생략하면 -1
     */
    synchronized int tryRecord(String key, Instant now) {
        Instant last = lastRecordedAt.get(key);
        if (last != null && now.isBefore(last.plus(window))) {
            suppressed.merge(key, 1, Integer::sum);
            return -1;
        }
        lastRecordedAt.put(key, now);
        Integer skipped = suppressed.remove(key);
        return skipped != null ? skipped : 0;
    }
}
