package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_trades")
public class TradeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String uuid;

    @Column(name = "position_id")
    private Long positionId;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType;

    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal volume;

    @Column(name = "executed_price", precision = 20, scale = 8)
    private BigDecimal executedPrice;

    @Column(name = "executed_volume", precision = 20, scale = 8)
    private BigDecimal executedVolume;

    @Column(precision = 20, scale = 8)
    private BigDecimal fee;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "signal_score")
    private Integer signalScore;

    @Column(name = "signal_reason", columnDefinition = "TEXT")
    private String signalReason;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TradeJpaEntity() {}

    public TradeJpaEntity(String uuid, Long positionId, String market, String tradeType, String orderType,
                          BigDecimal price, BigDecimal volume, BigDecimal executedPrice, BigDecimal executedVolume,
                          BigDecimal fee, String status, Integer signalScore, String signalReason,
                          LocalDateTime orderedAt, LocalDateTime executedAt, LocalDateTime createdAt) {
        this.uuid = uuid;
        this.positionId = positionId;
        this.market = market;
        this.tradeType = tradeType;
        this.orderType = orderType;
        this.price = price;
        this.volume = volume;
        this.executedPrice = executedPrice;
        this.executedVolume = executedVolume;
        this.fee = fee;
        this.status = status;
        this.signalScore = signalScore;
        this.signalReason = signalReason;
        this.orderedAt = orderedAt;
        this.executedAt = executedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public void setExecutedPrice(BigDecimal executedPrice) { this.executedPrice = executedPrice; }
    public BigDecimal getExecutedVolume() { return executedVolume; }
    public void setExecutedVolume(BigDecimal executedVolume) { this.executedVolume = executedVolume; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSignalScore() { return signalScore; }
    public void setSignalScore(Integer signalScore) { this.signalScore = signalScore; }
    public String getSignalReason() { return signalReason; }
    public void setSignalReason(String signalReason) { this.signalReason = signalReason; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
