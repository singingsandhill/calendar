package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 매수가능조회 응답 (TTTC8908R)
 */
public record KisBuyingPowerResponse(
    @JsonProperty("output") BuyingPower output
) {
    public record BuyingPower(
        @JsonProperty("ord_psbl_cash") BigDecimal orderableCash,
        @JsonProperty("ord_psbl_sbst") BigDecimal orderableSubstitute,
        @JsonProperty("ruse_psbl_amt") BigDecimal reusableAmount,
        @JsonProperty("max_buy_amt") BigDecimal maxBuyAmount,
        @JsonProperty("max_buy_qty") Integer maxBuyQuantity,
        @JsonProperty("nrcvb_buy_amt") BigDecimal receivableBuyAmount,
        @JsonProperty("nrcvb_buy_qty") Integer receivableBuyQuantity
    ) {}

    public BigDecimal getMaxBuyAmount() {
        return output != null ? output.maxBuyAmount() : BigDecimal.ZERO;
    }

    public Integer getMaxBuyQuantity() {
        return output != null ? output.maxBuyQuantity() : 0;
    }
}
