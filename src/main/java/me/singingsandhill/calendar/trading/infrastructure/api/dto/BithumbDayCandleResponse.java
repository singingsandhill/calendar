package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 일(Day) 캔들 — GET /v1/candles/days. 봉 경계는 KST 자정이고(candle_date_time_kst = T00:00:00), 응답의 첫 원소는
 * 형성 중인 당일봉이다 (2026-10-22 실측). prev_closing_price·change_* 등 쓰지 않는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BithumbDayCandleResponse(
    @JsonProperty("market") String market,
    @JsonProperty("candle_date_time_utc") String candleDateTimeUtc,
    @JsonProperty("candle_date_time_kst") String candleDateTimeKst,
    @JsonProperty("opening_price") Double openingPrice,
    @JsonProperty("high_price") Double highPrice,
    @JsonProperty("low_price") Double lowPrice,
    @JsonProperty("trade_price") Double tradePrice
) {}
