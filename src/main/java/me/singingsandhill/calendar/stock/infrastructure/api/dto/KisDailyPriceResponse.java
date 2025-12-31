package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 일자별 시세 응답 (FHKST01010400)
 */
public record KisDailyPriceResponse(
    @JsonProperty("stck_bsop_date") String tradingDate,
    @JsonProperty("stck_oprc") BigDecimal openPrice,
    @JsonProperty("stck_hgpr") BigDecimal highPrice,
    @JsonProperty("stck_lwpr") BigDecimal lowPrice,
    @JsonProperty("stck_clpr") BigDecimal closePrice,
    @JsonProperty("acml_vol") Long volume,
    @JsonProperty("acml_tr_pbmn") BigDecimal tradeValue,
    @JsonProperty("prdy_vrss") BigDecimal priceChange,
    @JsonProperty("prdy_vrss_sign") String priceChangeSign,
    @JsonProperty("prdy_ctrt") BigDecimal changeRate
) {}
