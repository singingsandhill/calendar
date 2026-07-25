package me.singingsandhill.calendar.stock.domain.trade;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 거래 엔티티
 */
public class StockTrade {

    /** 주문 전송 전 선영속화 시 부여하는 임시 주문번호 접두사 (브로커 번호 확보 시 교체). */
    public static final String PENDING_ORDER_ID_PREFIX = "PENDING-";

    private Long id;
    private String orderId;
    private Long positionId;
    private final String stockCode;
    private final StockTradeType tradeType;
    private final StockOrderType orderType;
    private final Integer quantity;
    private final BigDecimal orderPrice;

    private Integer executedQuantity;
    private BigDecimal executedPrice;
    private BigDecimal fee;
    private StockTradeStatus status;
    private StockCloseReason exitReason;

    private final LocalDateTime orderedAt;
    private LocalDateTime executedAt;
    private final LocalDateTime createdAt;

    private StockTrade(String orderId, String stockCode, StockTradeType tradeType,
                       StockOrderType orderType, Integer quantity, BigDecimal orderPrice) {
        this.orderId = orderId;
        this.stockCode = stockCode;
        this.tradeType = tradeType;
        this.orderType = orderType;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.status = StockTradeStatus.PENDING;
        this.orderedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 매수 주문 생성
     */
    public static StockTrade createBuyOrder(String orderId, String stockCode,
                                             Integer quantity, BigDecimal price,
                                             boolean isMarketOrder) {
        return new StockTrade(
            orderId,
            stockCode,
            StockTradeType.BUY,
            isMarketOrder ? StockOrderType.MARKET : StockOrderType.LIMIT,
            quantity,
            price
        );
    }

    /**
     * 매도 주문 생성
     */
    public static StockTrade createSellOrder(String orderId, String stockCode,
                                              Integer quantity, BigDecimal price,
                                              boolean isMarketOrder, StockCloseReason reason) {
        StockTrade trade = new StockTrade(
            orderId,
            stockCode,
            StockTradeType.SELL,
            isMarketOrder ? StockOrderType.MARKET : StockOrderType.LIMIT,
            quantity,
            price
        );
        trade.exitReason = reason;
        return trade;
    }

    /**
     * 브로커 주문번호(ODNO) 부착 — 선영속화된 PENDING 거래에 응답 도착 후 부여.
     */
    public void assignBrokerOrderId(String brokerOrderId) {
        if (brokerOrderId != null && !brokerOrderId.isBlank()) {
            this.orderId = brokerOrderId;
        }
    }

    /** 주문번호가 아직 브로커 값으로 교체되지 않은(=접수 여부 미확인) 거래인지. */
    public boolean isUnconfirmedOrder() {
        return orderId != null && orderId.startsWith(PENDING_ORDER_ID_PREFIX);
    }

    public boolean isPending() {
        return status == StockTradeStatus.PENDING;
    }

    /**
     * 체결 처리
     */
    public void markFilled(BigDecimal executedPrice, Integer executedQuantity, BigDecimal fee) {
        this.executedPrice = executedPrice;
        this.executedQuantity = executedQuantity;
        this.fee = fee;
        this.status = executedQuantity.equals(this.quantity)
            ? StockTradeStatus.FILLED
            : StockTradeStatus.PARTIAL;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 취소 처리
     */
    public void markCancelled() {
        this.status = StockTradeStatus.CANCELLED;
    }

    /**
     * 총 거래금액 계산
     */
    public BigDecimal getTotalAmount() {
        if (executedPrice != null && executedQuantity != null) {
            return executedPrice.multiply(BigDecimal.valueOf(executedQuantity));
        }
        return orderPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean isBuy() {
        return tradeType == StockTradeType.BUY;
    }

    public boolean isSell() {
        return tradeType == StockTradeType.SELL;
    }

    public boolean isFilled() {
        return status == StockTradeStatus.FILLED;
    }

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getStockCode() { return stockCode; }
    public StockTradeType getTradeType() { return tradeType; }
    public StockOrderType getOrderType() { return orderType; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getOrderPrice() { return orderPrice; }
    public Integer getExecutedQuantity() { return executedQuantity; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public BigDecimal getFee() { return fee; }
    public StockTradeStatus getStatus() { return status; }
    public StockCloseReason getExitReason() { return exitReason; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
