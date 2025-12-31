package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 호가 응답 (FHKST01010200)
 */
public record KisOrderbookResponse(
    @JsonProperty("stck_shrn_iscd") String stockCode,
    @JsonProperty("askp1") BigDecimal ask1Price,
    @JsonProperty("askp2") BigDecimal ask2Price,
    @JsonProperty("askp3") BigDecimal ask3Price,
    @JsonProperty("bidp1") BigDecimal bid1Price,
    @JsonProperty("bidp2") BigDecimal bid2Price,
    @JsonProperty("bidp3") BigDecimal bid3Price,
    @JsonProperty("askp_rsqn1") Long ask1Volume,
    @JsonProperty("askp_rsqn2") Long ask2Volume,
    @JsonProperty("askp_rsqn3") Long ask3Volume,
    @JsonProperty("bidp_rsqn1") Long bid1Volume,
    @JsonProperty("bidp_rsqn2") Long bid2Volume,
    @JsonProperty("bidp_rsqn3") Long bid3Volume,
    @JsonProperty("total_askp_rsqn") Long totalAskVolume,
    @JsonProperty("total_bidp_rsqn") Long totalBidVolume
) {
    /**
     * 스프레드 계산 ((매도1호가 - 매수1호가) / 현재가 * 100)
     */
    public BigDecimal calculateSpreadPercent() {
        if (bid1Price == null || bid1Price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return ask1Price.subtract(bid1Price)
            .multiply(new BigDecimal("100"))
            .divide(bid1Price, 4, RoundingMode.HALF_UP);
    }

    /**
     * 호가 불균형 비율 (매수총잔량 / 매도총잔량)
     */
    public BigDecimal calculateOrderImbalance() {
        if (totalAskVolume == null || totalAskVolume == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(totalBidVolume)
            .divide(new BigDecimal(totalAskVolume), 4, RoundingMode.HALF_UP);
    }
}
