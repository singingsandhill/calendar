package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문체결조회 응답 (TTTC0081R)
 */
public record KisOrderDetailResponse(
    @JsonProperty("rt_cd") String resultCode,
    @JsonProperty("msg_cd") String messageCode,
    @JsonProperty("msg1") String message,
    @JsonProperty("output1") List<OrderDetail> orders
) {
    public record OrderDetail(
        @JsonProperty("odno") String orderId,
        @JsonProperty("pdno") String stockCode,
        @JsonProperty("prdt_name") String stockName,
        @JsonProperty("sll_buy_dvsn_cd") String orderSide,
        @JsonProperty("ord_qty") Integer orderQuantity,
        @JsonProperty("ord_unpr") BigDecimal orderPrice,
        @JsonProperty("tot_ccld_qty") Integer filledQuantity,
        @JsonProperty("avg_prvs") BigDecimal filledPrice,
        @JsonProperty("ord_tmd") String orderTime,
        @JsonProperty("ord_gno_brno") String orderBranch,
        @JsonProperty("orgn_odno") String originalOrderId,
        @JsonProperty("ord_dvsn_name") String orderTypeName,
        @JsonProperty("ccld_cndt_name") String orderStatusName
    ) {
        public boolean isBuy() {
            return "02".equals(orderSide);
        }

        public boolean isSell() {
            return "01".equals(orderSide);
        }

        public boolean isFilled() {
            return filledQuantity != null && filledQuantity > 0;
        }

        public boolean isFullyFilled() {
            return orderQuantity != null && filledQuantity != null
                && filledQuantity.equals(orderQuantity);
        }
    }

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
