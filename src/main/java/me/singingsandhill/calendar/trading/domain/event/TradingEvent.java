package me.singingsandhill.calendar.trading.domain.event;

import java.time.LocalDateTime;

public class TradingEvent {

    private Long id;
    private final TradingEventLevel level;
    private final String eventType;
    private final String market;
    private final String message;
    private final String payload;
    private final LocalDateTime createdAt;

    public TradingEvent(Long id, TradingEventLevel level, String eventType, String market,
                        String message, String payload, LocalDateTime createdAt) {
        this.id = id;
        this.level = level;
        this.eventType = eventType;
        this.market = market;
        this.message = message;
        this.payload = payload;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static TradingEvent create(TradingEventLevel level, String eventType, String market,
                                      String message, String payload) {
        return new TradingEvent(null, level, eventType, market, message, payload, LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TradingEventLevel getLevel() { return level; }
    public String getEventType() { return eventType; }
    public String getMarket() { return market; }
    public String getMessage() { return message; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
