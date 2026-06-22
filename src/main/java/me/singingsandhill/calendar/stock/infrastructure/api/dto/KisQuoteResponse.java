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
    @JsonProperty("shnu_cntg_smtn") BigDecimal totalBuyVolume,
    @JsonProperty("cttr") BigDecimal contractStrength
) {
    /**
     * 체결강도.
     *
     * KIS 주식현재가 시세(FHKST01010100)는 체결강도를 {@code cttr} 필드로 직접 제공한다.
     * 과거 구현은 누적 매도/매수 체결량({@code seln_cntg_smtn}/{@code shnu_cntg_smtn})으로
     * 계산했으나, 해당 TR 응답이 두 필드를 주지 않아 체결강도가 항상 0 → 스크리닝/진입이
     * 전량 차단됐다. 따라서 {@code cttr} 를 우선 사용하고, 없을 때만 누적 체결량으로 폴백한다.
     */
    public BigDecimal calculateTradeStrength() {
        // KIS 가 직접 제공하는 체결강도(cttr) 우선.
        if (contractStrength != null && contractStrength.compareTo(BigDecimal.ZERO) > 0) {
            return contractStrength.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        // 폴백: 누적 체결량 기반 계산 (cttr 미제공 TR / 단위 테스트 호환).
        if (totalSellVolume == null || totalSellVolume.compareTo(BigDecimal.ZERO) == 0
                || totalBuyVolume == null) {
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
        if (openPrice == null || openPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return openPrice.subtract(prevClosePrice)
            .multiply(new BigDecimal("100"))
            .divide(prevClosePrice, 4, java.math.RoundingMode.HALF_UP);
    }
}
