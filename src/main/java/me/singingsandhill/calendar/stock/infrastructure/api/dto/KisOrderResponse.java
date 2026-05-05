package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 주문 응답 (TTTC0012U/TTTC0011U)
 */
public record KisOrderResponse(
    @JsonProperty("rt_cd") String resultCode,
    @JsonProperty("msg_cd") String messageCode,
    @JsonProperty("msg1") String message,
    @JsonProperty("output") OrderOutput output
) {
    public record OrderOutput(
        @JsonProperty("ODNO") String orderId,
        @JsonProperty("ORD_TMD") String orderTime
    ) {}

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    public String getOrderId() {
        return output != null ? output.orderId() : null;
    }

    /**
     * PAPER/BACKTEST 모드용 가짜 주문 응답 생성.
     */
    public static KisOrderResponse simulated(String orderId) {
        return new KisOrderResponse("0", "SIM", "simulated",
            new OrderOutput(orderId, ""));
    }
}
