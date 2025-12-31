package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BithumbCandleResponse(
    @JsonProperty("market") String market,
    @JsonProperty("candle_date_time_utc") String candleDateTimeUtc,
    @JsonProperty("candle_date_time_kst") String candleDateTimeKst,
    @JsonProperty("opening_price") Double openingPrice,
    @JsonProperty("high_price") Double highPrice,
    @JsonProperty("low_price") Double lowPrice,
    @JsonProperty("trade_price") Double tradePrice,
    @JsonProperty("timestamp") Long timestamp,
    @JsonProperty("candle_acc_trade_price") Double candleAccTradePrice,
    @JsonProperty("candle_acc_trade_volume") Double candleAccTradeVolume,
    @JsonProperty("unit") Integer unit
) {}
