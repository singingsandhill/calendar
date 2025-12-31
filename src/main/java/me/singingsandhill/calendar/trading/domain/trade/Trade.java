package me.singingsandhill.calendar.trading.domain.trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {

    private Long id;
    private final String uuid;
    private Long positionId;
    private final String market;
    private final TradeType tradeType;
    private final String orderType;
    private final BigDecimal price;
    private final BigDecimal volume;
    private BigDecimal executedPrice;
    private BigDecimal executedVolume;
    private BigDecimal fee;
    private TradeStatus status;
    private Integer signalScore;
    private String signalReason;
    private final LocalDateTime orderedAt;
    private LocalDateTime executedAt;
    private final LocalDateTime createdAt;

    public Trade(Long id, String uuid, Long positionId, String market,
                 TradeType tradeType, String orderType, BigDecimal price, BigDecimal volume,
                 BigDecimal executedPrice, BigDecimal executedVolume, BigDecimal fee,
                 TradeStatus status, Integer signalScore, String signalReason,
                 LocalDateTime orderedAt, LocalDateTime executedAt, LocalDateTime createdAt) {
        this.id = id;
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
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Trade createBuyOrder(String uuid, String market, BigDecimal price, BigDecimal volume,
                                        String orderType, Integer signalScore, String signalReason) {
        return new Trade(null, uuid, null, market, TradeType.BUY, orderType, price, volume,
                null, null, null, TradeStatus.WAIT, signalScore, signalReason,
                LocalDateTime.now(), null, LocalDateTime.now());
    }

    public static Trade createSellOrder(String uuid, Long positionId, String market, BigDecimal price, BigDecimal volume,
                                         String orderType, Integer signalScore, String signalReason) {
        return new Trade(null, uuid, positionId, market, TradeType.SELL, orderType, price, volume,
                null, null, null, TradeStatus.WAIT, signalScore, signalReason,
                LocalDateTime.now(), null, LocalDateTime.now());
    }

    public void markExecuted(BigDecimal executedPrice, BigDecimal executedVolume, BigDecimal fee) {
        this.executedPrice = executedPrice;
        this.executedVolume = executedVolume;
        this.fee = fee;
        this.status = TradeStatus.DONE;
        this.executedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = TradeStatus.CANCEL;
    }

    public BigDecimal getTotalAmount() {
        if (executedPrice != null && executedVolume != null) {
            return executedPrice.multiply(executedVolume);
        }
        return price.multiply(volume);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuid() { return uuid; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getMarket() { return market; }
    public TradeType getTradeType() { return tradeType; }
    public String getOrderType() { return orderType; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getVolume() { return volume; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public BigDecimal getExecutedVolume() { return executedVolume; }
    public BigDecimal getFee() { return fee; }
    public TradeStatus getStatus() { return status; }
    public Integer getSignalScore() { return signalScore; }
    public String getSignalReason() { return signalReason; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
