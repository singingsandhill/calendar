package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.event.TradingEvent;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.event.TradingEventRepository;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.TradingEventJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.TradingEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class TradingEventRepositoryAdapter implements TradingEventRepository {

    private final TradingEventJpaRepository jpaRepository;

    public TradingEventRepositoryAdapter(TradingEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public TradingEvent save(TradingEvent event) {
        TradingEventJpaEntity entity = toEntity(event);
        TradingEventJpaEntity saved = jpaRepository.save(entity);
        event.setId(saved.getId());
        return event;
    }

    @Override
    public List<TradingEvent> findRecent(int limit) {
        return jpaRepository.findRecent(PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<TradingEvent> findRecentByMinLevel(TradingEventLevel minLevel, int limit) {
        List<String> levels = Arrays.stream(TradingEventLevel.values())
                .filter(l -> l.atLeast(minLevel))
                .map(Enum::name)
                .toList();
        return jpaRepository.findRecentByLevelIn(levels, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    private TradingEventJpaEntity toEntity(TradingEvent event) {
        TradingEventJpaEntity entity = new TradingEventJpaEntity();
        if (event.getId() != null) entity.setId(event.getId());
        entity.setLevel(event.getLevel().name());
        entity.setEventType(event.getEventType());
        entity.setMarket(event.getMarket());
        entity.setMessage(truncate(event.getMessage(), 500));
        entity.setPayload(event.getPayload());
        entity.setCreatedAt(event.getCreatedAt());
        return entity;
    }

    private TradingEvent toDomain(TradingEventJpaEntity e) {
        return new TradingEvent(
                e.getId(),
                TradingEventLevel.valueOf(e.getLevel()),
                e.getEventType(),
                e.getMarket(),
                e.getMessage(),
                e.getPayload(),
                e.getCreatedAt()
        );
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
