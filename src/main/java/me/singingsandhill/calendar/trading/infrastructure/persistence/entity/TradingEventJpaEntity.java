package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_events", indexes = {
        @Index(name = "idx_trading_events_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_trading_events_level_created", columnList = "level, created_at DESC")
})
public class TradingEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(length = 20)
    private String market;

    @Column(nullable = false, length = 500)
    private String message;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TradingEventJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
