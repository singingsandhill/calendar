package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_positions")
public class PositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "entry_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "entry_volume", nullable = false, precision = 20, scale = 8)
    private BigDecimal entryVolume;

    @Column(name = "entry_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal entryAmount;

    @Column(name = "exit_price", precision = 20, scale = 8)
    private BigDecimal exitPrice;

    @Column(name = "exit_volume", precision = 20, scale = 8)
    private BigDecimal exitVolume;

    @Column(name = "exit_amount", precision = 20, scale = 8)
    private BigDecimal exitAmount;

    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "realized_pnl_pct", precision = 10, scale = 4)
    private BigDecimal realizedPnlPct;

    @Column(name = "stop_loss_price", precision = 20, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(name = "take_profit_price", precision = 20, scale = 8)
    private BigDecimal takeProfitPrice;

    @Column(name = "trailing_stop_price", precision = 20, scale = 8)
    private BigDecimal trailingStopPrice;

    @Column(name = "high_water_mark", precision = 20, scale = 8)
    private BigDecimal highWaterMark;

    @Column(name = "trailing_stop_active")
    private boolean trailingStopActive;

    @Column(name = "close_reason", length = 50)
    private String closeReason;

    @Column(name = "entry_fee", precision = 20, scale = 8)
    private BigDecimal entryFee;

    @Column(name = "exit_fee", precision = 20, scale = 8)
    private BigDecimal exitFee;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PositionJpaEntity() {}

    public PositionJpaEntity(String market, String status, BigDecimal entryPrice, BigDecimal entryVolume,
                              BigDecimal entryAmount, BigDecimal exitPrice, BigDecimal exitVolume, BigDecimal exitAmount,
                              BigDecimal realizedPnl, BigDecimal realizedPnlPct, BigDecimal stopLossPrice,
                              BigDecimal takeProfitPrice, BigDecimal trailingStopPrice, BigDecimal highWaterMark,
                              boolean trailingStopActive, String closeReason, LocalDateTime openedAt,
                              LocalDateTime closedAt, LocalDateTime createdAt,
                              BigDecimal entryFee, BigDecimal exitFee, BigDecimal totalFees) {
        this.market = market;
        this.status = status;
        this.entryPrice = entryPrice;
        this.entryVolume = entryVolume;
        this.entryAmount = entryAmount;
        this.exitPrice = exitPrice;
        this.exitVolume = exitVolume;
        this.exitAmount = exitAmount;
        this.realizedPnl = realizedPnl;
        this.realizedPnlPct = realizedPnlPct;
        this.stopLossPrice = stopLossPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.trailingStopPrice = trailingStopPrice;
        this.highWaterMark = highWaterMark;
        this.trailingStopActive = trailingStopActive;
        this.closeReason = closeReason;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.entryFee = entryFee;
        this.exitFee = exitFee;
        this.totalFees = totalFees;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getEntryVolume() { return entryVolume; }
    public void setEntryVolume(BigDecimal entryVolume) { this.entryVolume = entryVolume; }
    public BigDecimal getEntryAmount() { return entryAmount; }
    public void setEntryAmount(BigDecimal entryAmount) { this.entryAmount = entryAmount; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public void setExitPrice(BigDecimal exitPrice) { this.exitPrice = exitPrice; }
    public BigDecimal getExitVolume() { return exitVolume; }
    public void setExitVolume(BigDecimal exitVolume) { this.exitVolume = exitVolume; }
    public BigDecimal getExitAmount() { return exitAmount; }
    public void setExitAmount(BigDecimal exitAmount) { this.exitAmount = exitAmount; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public BigDecimal getRealizedPnlPct() { return realizedPnlPct; }
    public void setRealizedPnlPct(BigDecimal realizedPnlPct) { this.realizedPnlPct = realizedPnlPct; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public BigDecimal getTakeProfitPrice() { return takeProfitPrice; }
    public void setTakeProfitPrice(BigDecimal takeProfitPrice) { this.takeProfitPrice = takeProfitPrice; }
    public BigDecimal getTrailingStopPrice() { return trailingStopPrice; }
    public void setTrailingStopPrice(BigDecimal trailingStopPrice) { this.trailingStopPrice = trailingStopPrice; }
    public BigDecimal getHighWaterMark() { return highWaterMark; }
    public void setHighWaterMark(BigDecimal highWaterMark) { this.highWaterMark = highWaterMark; }
    public boolean isTrailingStopActive() { return trailingStopActive; }
    public void setTrailingStopActive(boolean trailingStopActive) { this.trailingStopActive = trailingStopActive; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BigDecimal getEntryFee() { return entryFee; }
    public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }
    public BigDecimal getExitFee() { return exitFee; }
    public void setExitFee(BigDecimal exitFee) { this.exitFee = exitFee; }
    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
}
