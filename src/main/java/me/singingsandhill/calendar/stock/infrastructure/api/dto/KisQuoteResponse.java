package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 주식현재가 시세 응답 (FHKST01010100)
 */
public record KisQuoteResponse(
    @JsonProperty("stck_shrn_iscd") String stockCode,
    @JsonProperty("stck_prpr") BigDecimal currentPrice,
    @JsonProperty("stck_oprc") BigDecimal openPrice,
    @JsonProperty("stck_hgpr") BigDecimal highPrice,
    @JsonProperty("stck_lwpr") BigDecimal lowPrice,
    @JsonProperty("stck_sdpr") BigDecimal prevClosePrice,
    @JsonProperty("prdy_vrss") BigDecimal priceChange,
    @JsonProperty("prdy_ctrt") BigDecimal changeRate,
    @JsonProperty("acml_vol") Long volume,
    @JsonProperty("acml_tr_pbmn") BigDecimal tradeValue,
    @JsonProperty("hts_avls") BigDecimal marketCap,
    @JsonProperty("vol_tnrt") BigDecimal volumeTurnover,
    @JsonProperty("seln_cntg_smtn") BigDecimal totalSellVolume,
    @JsonProperty("shnu_cntg_smtn") BigDecimal totalBuyVolume
) {
    /**
     * 체결강도 계산 (매수체결량 / 매도체결량 * 100)
     */
    public BigDecimal calculateTradeStrength() {
        if (totalSellVolume == null || totalSellVolume.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalBuyVolume.multiply(new BigDecimal("100"))
            .divide(totalSellVolume, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 갭 비율 계산 ((시가 - 전일종가) / 전일종가 * 100)
     */
    public BigDecimal calculateGapPercent() {
        if (prevClosePrice == null || prevClosePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return openPrice.subtract(prevClosePrice)
            .multiply(new BigDecimal("100"))
            .divide(prevClosePrice, 4, java.math.RoundingMode.HALF_UP);
    }
}
