package me.singingsandhill.calendar.stock.application.concurrency;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 종목 코드별 ReentrantLock 발급기.
 *
 * 5초 폴링 루프와 사용자 트리거(긴급 청산) 가 동시에 동일 종목에 매수/매도를
 * 시도하는 race 를 막기 위함이다. {@link #withLock} 사용 권장.
 */
@Component
public class StockCodeLocks {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock get(String stockCode) {
        return locks.computeIfAbsent(stockCode, k -> new ReentrantLock());
    }

    public <T> T withLock(String stockCode, java.util.function.Supplier<T> action) {
        ReentrantLock lock = get(stockCode);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void withLock(String stockCode, Runnable action) {
        ReentrantLock lock = get(stockCode);
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
