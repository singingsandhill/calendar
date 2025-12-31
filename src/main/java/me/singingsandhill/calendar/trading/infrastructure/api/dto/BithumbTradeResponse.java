package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BithumbTradeResponse(
    @JsonProperty("market") String market,
    @JsonProperty("trade_date_utc") String tradeDateUtc,
    @JsonProperty("trade_time_utc") String tradeTimeUtc,
    @JsonProperty("timestamp") Long timestamp,
    @JsonProperty("trade_price") Double tradePrice,
    @JsonProperty("trade_volume") Double tradeVolume,
    @JsonProperty("prev_closing_price") Double prevClosingPrice,
    @JsonProperty("change_price") Double changePrice,
    @JsonProperty("ask_bid") String askBid,
    @JsonProperty("sequential_id") Long sequentialId
) {}
