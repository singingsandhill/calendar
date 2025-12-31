package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BithumbOrderbookResponse(
    @JsonProperty("market") String market,
    @JsonProperty("timestamp") Long timestamp,
    @JsonProperty("total_ask_size") Double totalAskSize,
    @JsonProperty("total_bid_size") Double totalBidSize,
    @JsonProperty("orderbook_units") List<OrderbookUnit> orderbookUnits
) {
    public record OrderbookUnit(
        @JsonProperty("ask_price") Double askPrice,
        @JsonProperty("bid_price") Double bidPrice,
        @JsonProperty("ask_size") Double askSize,
        @JsonProperty("bid_size") Double bidSize
    ) {}
}
