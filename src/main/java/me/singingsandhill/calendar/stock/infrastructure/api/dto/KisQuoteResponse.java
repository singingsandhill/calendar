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
    @JsonProperty("cttr") BigDecimal contractStrength,
    /** 종목 상태 구분 (00:정상 51:관리 52:투자위험 53:투자경고 54:투자주의 58:거래정지 59:단기과열) */
    @JsonProperty("iscd_stat_cls_code") String statusCode,
    /** 임시정지 여부 (Y/N) — VI 발동에 따른 단일가 전환 구간 포함 */
    @JsonProperty("temp_stop_yn") String temporaryHalt,
    /** 시장경고 (00:없음 01:투자주의 02:투자경고 03:투자위험) */
    @JsonProperty("mrkt_warn_cls_code") String marketWarning,
    /** 정리매매 여부 (Y/N) */
    @JsonProperty("sltr_yn") String liquidationTrading
) {

    /**
     * 상태 필드 없이 시세만으로 생성 (상태 미상 → 거래 가능으로 간주).
     * 상태 필드를 제공하지 않는 경로·테스트용.
     */
    public KisQuoteResponse(String stockCode, BigDecimal currentPrice, BigDecimal openPrice,
                            BigDecimal highPrice, BigDecimal lowPrice, BigDecimal prevClosePrice,
                            BigDecimal priceChange, BigDecimal changeRate, Long volume,
                            BigDecimal tradeValue, BigDecimal marketCap, BigDecimal volumeTurnover,
                            BigDecimal totalSellVolume, BigDecimal totalBuyVolume,
                            BigDecimal contractStrength) {
        this(stockCode, currentPrice, openPrice, highPrice, lowPrice, prevClosePrice,
            priceChange, changeRate, volume, tradeValue, marketCap, volumeTurnover,
            totalSellVolume, totalBuyVolume, contractStrength, null, null, null, null);
    }

    /** 거래정지·단기과열 등 매매를 피해야 하는 종목 상태 코드. */
    private static final java.util.Set<String> BLOCKED_STATUS_CODES =
        java.util.Set.of("51", "52", "53", "58", "59");
    /** 투자경고·투자위험 (투자주의 01 은 허용). */
    private static final java.util.Set<String> BLOCKED_WARNING_CODES = java.util.Set.of("02", "03");

    /**
     * 매매 가능 종목인지 — VI(임시정지)·거래정지·관리·정리매매·투자경고/위험을 배제한다
     * (2026-07-24 리뷰 §6 / P2-3).
     *
     * <p>필드가 없거나 비어 있으면 <b>거래 가능</b>으로 본다. KIS 응답 스펙 차이로 필드가
     * 비었을 때 전 종목이 걸러지는 회귀를 막기 위함이며, 필드 존재 여부는 호출측 계측 로그로
     * 확인한다.
     */
    public boolean isTradable() {
        if (isYes(temporaryHalt) || isYes(liquidationTrading)) {
            return false;
        }
        if (statusCode != null && BLOCKED_STATUS_CODES.contains(statusCode.trim())) {
            return false;
        }
        return marketWarning == null || !BLOCKED_WARNING_CODES.contains(marketWarning.trim());
    }

    /** 상태 필드가 하나라도 채워져 있는지 — 계측용(필드명이 실제 응답과 맞는지 확인). */
    public boolean hasTradabilityFields() {
        return notBlank(statusCode) || notBlank(temporaryHalt)
            || notBlank(marketWarning) || notBlank(liquidationTrading);
    }

    /** 배제 사유 요약 (로그용). */
    public String tradabilityReason() {
        if (isYes(temporaryHalt)) return "임시정지(VI/거래정지)";
        if (isYes(liquidationTrading)) return "정리매매";
        if (statusCode != null && BLOCKED_STATUS_CODES.contains(statusCode.trim())) {
            return "종목상태=" + statusCode.trim();
        }
        if (marketWarning != null && BLOCKED_WARNING_CODES.contains(marketWarning.trim())) {
            return "시장경고=" + marketWarning.trim();
        }
        return "정상";
    }

    private static boolean isYes(String value) {
        return value != null && "Y".equalsIgnoreCase(value.trim());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
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
