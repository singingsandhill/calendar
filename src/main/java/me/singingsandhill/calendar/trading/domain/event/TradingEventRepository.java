package me.singingsandhill.calendar.trading.domain.event;

import java.util.List;

public interface TradingEventRepository {
    TradingEvent save(TradingEvent event);
    List<TradingEvent> findRecent(int limit);
    List<TradingEvent> findRecentByMinLevel(TradingEventLevel minLevel, int limit);
}
