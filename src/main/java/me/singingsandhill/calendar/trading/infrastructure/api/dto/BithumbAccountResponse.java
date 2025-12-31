package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BithumbAccountResponse(
    @JsonProperty("currency") String currency,
    @JsonProperty("balance") String balance,
    @JsonProperty("locked") String locked,
    @JsonProperty("avg_buy_price") String avgBuyPrice,
    @JsonProperty("avg_buy_price_modified") Boolean avgBuyPriceModified,
    @JsonProperty("unit_currency") String unitCurrency
) {}
