package me.singingsandhill.calendar.trading.domain.trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {

    private Long id;
    // §8-B: 선영속화 시점엔 거래소 uuid 가 없어 client_order_id 를 대신 담고, 체결 확인 시 거래소 uuid 로 갱신
    private String uuid;
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
    // §8-B: 클라이언트 부여 멱등키 (1~36자, [A-Za-z0-9-_]). 응답 유실 시 이 키로 거래소 재조회.
    private final String clientOrderId;

    public Trade(Long id, String uuid, Long positionId, String market,
                 TradeType tradeType, String orderType, BigDecimal price, BigDecimal volume,
                 BigDecimal executedPrice, BigDecimal executedVolume, BigDecimal fee,
                 TradeStatus status, Integer signalScore, String signalReason,
                 LocalDateTime orderedAt, LocalDateTime executedAt, LocalDateTime createdAt) {
        this(id, uuid, positionId, market, tradeType, orderType, price, volume,
                executedPrice, executedVolume, fee, status, signalScore, signalReason,
                orderedAt, executedAt, createdAt, null);
    }

    public Trade(Long id, String uuid, Long positionId, String market,
                 TradeType tradeType, String orderType, BigDecimal price, BigDecimal volume,
                 BigDecimal executedPrice, BigDecimal executedVolume, BigDecimal fee,
                 TradeStatus status, Integer signalScore, String signalReason,
                 LocalDateTime orderedAt, LocalDateTime executedAt, LocalDateTime createdAt,
                 String clientOrderId) {
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
        this.clientOrderId = clientOrderId;
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

    /**
     * §8-B: 주문 전송 전 선영속화용 매수 Trade. 거래소 uuid 가 아직 없어 client_order_id 를 uuid 자리에
     * 담는다(NOT NULL·unique 충족) — 체결 확인 시 {@link #assignExchangeUuid} 로 갱신. 체결가 미정이라 price=0.
     */
    public static Trade createSubmittedBuy(String clientOrderId, String market, BigDecimal amount,
                                           Integer signalScore, String signalReason) {
        return new Trade(null, clientOrderId, null, market, TradeType.BUY, "market",
                BigDecimal.ZERO, amount, null, null, null, TradeStatus.SUBMITTED,
                signalScore, signalReason, LocalDateTime.now(), null, LocalDateTime.now(), clientOrderId);
    }

    /**
     * §8-B 매도 확장: 주문 전송 전 선영속화용 매도 Trade. positionId 를 처음부터 연결해 스윕이
     * 체결 확인 시 해당 Position 을 청산할 수 있게 한다. 체결가 미정이라 price=0.
     */
    public static Trade createSubmittedSell(String clientOrderId, Long positionId, String market,
                                            BigDecimal volume, Integer signalScore, String signalReason) {
        return new Trade(null, clientOrderId, positionId, market, TradeType.SELL, "market",
                BigDecimal.ZERO, volume, null, null, null, TradeStatus.SUBMITTED,
                signalScore, signalReason, LocalDateTime.now(), null, LocalDateTime.now(), clientOrderId);
    }

    /**
     * §8-B: 선영속화 단계에서 client_order_id 로 채워 둔 uuid 를 거래소 uuid 로 교체한다.
     */
    public void assignExchangeUuid(String exchangeUuid) {
        if (exchangeUuid != null && !exchangeUuid.isBlank()) {
            this.uuid = exchangeUuid;
        }
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

    public void markFailed(String errorMessage) {
        this.status = TradeStatus.FAILED;
        if (errorMessage != null && !errorMessage.isEmpty()) {
            String truncatedError = errorMessage.length() > 100
                    ? errorMessage.substring(0, 100) + "..."
                    : errorMessage;
            this.signalReason = (this.signalReason != null ? this.signalReason : "")
                    + " | FAILED: " + truncatedError;
        }
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
    public String getClientOrderId() { return clientOrderId; }
}
