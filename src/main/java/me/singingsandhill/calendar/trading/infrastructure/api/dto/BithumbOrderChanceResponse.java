package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BithumbOrderChanceResponse(
    @JsonProperty("bid_fee") String bidFee,
    @JsonProperty("ask_fee") String askFee,
    @JsonProperty("maker_bid_fee") String makerBidFee,
    @JsonProperty("maker_ask_fee") String makerAskFee,
    @JsonProperty("market") MarketInfo market,
    @JsonProperty("bid_account") AccountInfo bidAccount,
    @JsonProperty("ask_account") AccountInfo askAccount
) {
    public record MarketInfo(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("order_types") List<String> orderTypes,
        @JsonProperty("order_sides") List<String> orderSides,
        @JsonProperty("bid_types") List<String> bidTypes,
        @JsonProperty("ask_types") List<String> askTypes,
        @JsonProperty("bid") Constraint bid,
        @JsonProperty("ask") Constraint ask,
        @JsonProperty("max_total") String maxTotal,
        @JsonProperty("state") String state
    ) {}

    public record Constraint(
        @JsonProperty("currency") String currency,
        @JsonProperty("price_unit") String priceUnit,
        @JsonProperty("min_total") String minTotal
    ) {}

    public record AccountInfo(
        @JsonProperty("currency") String currency,
        @JsonProperty("balance") String balance,
        @JsonProperty("locked") String locked,
        @JsonProperty("avg_buy_price") String avgBuyPrice,
        @JsonProperty("avg_buy_price_modified") Boolean avgBuyPriceModified,
        @JsonProperty("unit_currency") String unitCurrency
    ) {}
}
