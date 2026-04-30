package me.singingsandhill.calendar.stock.application.observability;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 주식 봇 라이트 메트릭.
 *
 * - apiCallsLast5min: 최근 5분 KIS API 호출 수 (sliding window)
 * - lastTradingTickAt: 트레이딩 루프 마지막 실행 시각
 * - lastScreeningResult: 마지막 스크리닝 요약
 *
 * 외부 모니터링이 없으므로 BotStatus API 응답으로 노출해
 * "봇이 죽었나, 단지 후보가 없나"를 구분 가능하게 한다.
 */
@Component
public class StockBotMetrics {

    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Deque<Instant> apiCallTimestamps = new ArrayDeque<>();
    private final AtomicReference<Instant> lastTradingTickAt = new AtomicReference<>();
    private final AtomicReference<ScreeningSnapshot> lastScreeningResult = new AtomicReference<>();

    public synchronized void recordApiCall() {
        Instant now = Instant.now();
        apiCallTimestamps.addLast(now);
        evictOldLocked(now);
    }

    public synchronized int apiCallsLast5min() {
        evictOldLocked(Instant.now());
        return apiCallTimestamps.size();
    }

    public void recordTradingTick() {
        lastTradingTickAt.set(Instant.now());
    }

    public Instant getLastTradingTickAt() {
        return lastTradingTickAt.get();
    }

    public void recordScreeningResult(int total, int floorPassed, int selected,
                                       int dataInsufficient, int gapFiltered) {
        lastScreeningResult.set(new ScreeningSnapshot(
            Instant.now(), total, floorPassed, selected, dataInsufficient, gapFiltered));
    }

    public ScreeningSnapshot getLastScreeningResult() {
        return lastScreeningResult.get();
    }

    private void evictOldLocked(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!apiCallTimestamps.isEmpty() && apiCallTimestamps.peekFirst().isBefore(cutoff)) {
            apiCallTimestamps.pollFirst();
        }
    }

    public record ScreeningSnapshot(
        Instant at,
        int total,
        int floorPassed,
        int selected,
        int dataInsufficient,
        int gapFiltered
    ) {}
}
