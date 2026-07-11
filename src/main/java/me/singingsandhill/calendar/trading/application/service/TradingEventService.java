package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.event.TradingEvent;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.event.TradingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TradingEventService {

    private static final Logger log = LoggerFactory.getLogger(TradingEventService.class);

    private final TradingEventRepository repository;

    public TradingEventService(TradingEventRepository repository) {
        this.repository = repository;
    }

    public void record(TradingEventLevel level, String eventType, String market, String message) {
        record(level, eventType, market, message, null);
    }

    /**
     * 매매/리스크/리밸런싱 분기에서 호출. 이벤트 저장 실패는 절대 비즈니스 로직에 영향 주지 않음.
     * 새 트랜잭션으로 저장하여 호출자 트랜잭션 롤백 시에도 이벤트는 남음.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(TradingEventLevel level, String eventType, String market, String message, String payload) {
        try {
            repository.save(TradingEvent.create(level, eventType, market, message, payload));
        } catch (Exception e) {
            log.warn("Failed to record trading event ({} {}): {}", level, eventType, e.getMessage());
        }
    }

    public List<TradingEvent> findRecent(int limit) {
        return repository.findRecent(limit);
    }

    public List<TradingEvent> findRecentByMinLevel(TradingEventLevel minLevel, int limit) {
        return repository.findRecentByMinLevel(minLevel, limit);
    }
}
