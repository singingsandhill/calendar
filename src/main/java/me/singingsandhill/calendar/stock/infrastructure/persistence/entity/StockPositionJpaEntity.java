package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_positions",
       indexes = {
           @Index(name = "idx_position_date_status", columnList = "trading_date, status")
       })
public class StockPositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "trading_date", nullable = false)
    private LocalDate tradingDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "entry_price", precision = 15, scale = 2)
    private BigDecimal entryPrice;

    @Column(name = "entry_quantity")
    private Integer entryQuantity;

    @Column(name = "entry_amount", precision = 20, scale = 2)
    private BigDecimal entryAmount;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;

    @Column(name = "average_exit_price", precision = 15, scale = 2)
    private BigDecimal averageExitPrice;

    @Column(name = "tp1_executed")
    private Boolean tp1Executed = false;

    @Column(name = "tp2_executed")
    private Boolean tp2Executed = false;

    @Column(name = "tp3_executed")
    private Boolean tp3Executed = false;

    @Column(name = "day_high_price", precision = 15, scale = 2)
    private BigDecimal dayHighPrice;

    @Column(name = "stop_loss_price", precision = 15, scale = 2)
    private BigDecimal stopLossPrice;

    @Column(name = "trailing_high", precision = 15, scale = 2)
    private BigDecimal trailingHigh;

    @Column(name = "trailing_stop_price", precision = 15, scale = 2)
    private BigDecimal trailingStopPrice;

    @Column(name = "trailing_active")
    private Boolean trailingActive = false;

    @Column(name = "realized_pnl", precision = 15, scale = 2)
    private BigDecimal realizedPnl;

    @Column(name = "realized_pnl_percent", precision = 10, scale = 4)
    private BigDecimal realizedPnlPercent;

    @Column(name = "total_exit_amount", precision = 20, scale = 2)
    private BigDecimal totalExitAmount;

    @Column(name = "total_exit_quantity")
    private Integer totalExitQuantity;

    @Column(name = "close_reason", length = 30)
    private String closeReason;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected StockPositionJpaEntity() {}

    public StockPositionJpaEntity(String stockCode, LocalDate tradingDate, String status) {
        this.stockCode = stockCode;
        this.tradingDate = tradingDate;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public LocalDate getTradingDate() { return tradingDate; }
    public void setTradingDate(LocalDate tradingDate) { this.tradingDate = tradingDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public Integer getEntryQuantity() { return entryQuantity; }
    public void setEntryQuantity(Integer entryQuantity) { this.entryQuantity = entryQuantity; }
    public BigDecimal getEntryAmount() { return entryAmount; }
    public void setEntryAmount(BigDecimal entryAmount) { this.entryAmount = entryAmount; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public void setEnteredAt(LocalDateTime enteredAt) { this.enteredAt = enteredAt; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public BigDecimal getAverageExitPrice() { return averageExitPrice; }
    public void setAverageExitPrice(BigDecimal averageExitPrice) { this.averageExitPrice = averageExitPrice; }
    public Boolean getTp1Executed() { return tp1Executed; }
    public void setTp1Executed(Boolean tp1Executed) { this.tp1Executed = tp1Executed; }
    public Boolean getTp2Executed() { return tp2Executed; }
    public void setTp2Executed(Boolean tp2Executed) { this.tp2Executed = tp2Executed; }
    public Boolean getTp3Executed() { return tp3Executed; }
    public void setTp3Executed(Boolean tp3Executed) { this.tp3Executed = tp3Executed; }
    public BigDecimal getDayHighPrice() { return dayHighPrice; }
    public void setDayHighPrice(BigDecimal dayHighPrice) { this.dayHighPrice = dayHighPrice; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public BigDecimal getTrailingHigh() { return trailingHigh; }
    public void setTrailingHigh(BigDecimal trailingHigh) { this.trailingHigh = trailingHigh; }
    public BigDecimal getTrailingStopPrice() { return trailingStopPrice; }
    public void setTrailingStopPrice(BigDecimal trailingStopPrice) { this.trailingStopPrice = trailingStopPrice; }
    public Boolean getTrailingActive() { return trailingActive; }
    public void setTrailingActive(Boolean trailingActive) { this.trailingActive = trailingActive; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public BigDecimal getRealizedPnlPercent() { return realizedPnlPercent; }
    public void setRealizedPnlPercent(BigDecimal realizedPnlPercent) { this.realizedPnlPercent = realizedPnlPercent; }
    public BigDecimal getTotalExitAmount() { return totalExitAmount; }
    public void setTotalExitAmount(BigDecimal totalExitAmount) { this.totalExitAmount = totalExitAmount; }
    public Integer getTotalExitQuantity() { return totalExitQuantity; }
    public void setTotalExitQuantity(Integer totalExitQuantity) { this.totalExitQuantity = totalExitQuantity; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
