package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_trades",
       indexes = {
           @Index(name = "idx_trade_position", columnList = "position_id"),
           @Index(name = "idx_trade_order_id", columnList = "order_id"),
           @Index(name = "idx_trade_ordered_at", columnList = "ordered_at")
       })
public class StockTradeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "position_id")
    private Long positionId;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType;

    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "order_price", precision = 15, scale = 2)
    private BigDecimal orderPrice;

    @Column(name = "executed_quantity")
    private Integer executedQuantity;

    @Column(name = "executed_price", precision = 15, scale = 2)
    private BigDecimal executedPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "exit_reason", length = 30)
    private String exitReason;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StockTradeJpaEntity() {}

    public StockTradeJpaEntity(String orderId, String stockCode, String tradeType,
                                String orderType, Integer quantity, BigDecimal orderPrice) {
        this.orderId = orderId;
        this.stockCode = stockCode;
        this.tradeType = tradeType;
        this.orderType = orderType;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.status = "PENDING";
        this.orderedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getOrderPrice() { return orderPrice; }
    public void setOrderPrice(BigDecimal orderPrice) { this.orderPrice = orderPrice; }
    public Integer getExecutedQuantity() { return executedQuantity; }
    public void setExecutedQuantity(Integer executedQuantity) { this.executedQuantity = executedQuantity; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public void setExecutedPrice(BigDecimal executedPrice) { this.executedPrice = executedPrice; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
