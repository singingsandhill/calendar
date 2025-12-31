package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BithumbOrderResponse(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("side") String side,
    @JsonProperty("ord_type") String ordType,
    @JsonProperty("price") String price,
    @JsonProperty("state") String state,
    @JsonProperty("market") String market,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("volume") String volume,
    @JsonProperty("remaining_volume") String remainingVolume,
    @JsonProperty("reserved_fee") String reservedFee,
    @JsonProperty("remaining_fee") String remainingFee,
    @JsonProperty("paid_fee") String paidFee,
    @JsonProperty("locked") String locked,
    @JsonProperty("executed_volume") String executedVolume,
    @JsonProperty("trades_count") Integer tradesCount,
    @JsonProperty("trades") List<TradeDetail> trades
) {
    public record TradeDetail(
        @JsonProperty("market") String market,
        @JsonProperty("uuid") String uuid,
        @JsonProperty("price") String price,
        @JsonProperty("volume") String volume,
        @JsonProperty("funds") String funds,
        @JsonProperty("side") String side,
        @JsonProperty("created_at") String createdAt
    ) {}
}
