package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주식잔고조회 응답 (TTTC8434R)
 */
public record KisBalanceResponse(
    @JsonProperty("output1") List<HoldingStock> holdings,
    @JsonProperty("output2") List<AccountSummary> summary
) {
    public record HoldingStock(
        @JsonProperty("pdno") String stockCode,
        @JsonProperty("prdt_name") String stockName,
        @JsonProperty("hldg_qty") Integer quantity,
        @JsonProperty("pchs_avg_pric") BigDecimal averagePrice,
        @JsonProperty("prpr") BigDecimal currentPrice,
        @JsonProperty("evlu_amt") BigDecimal evaluationAmount,
        @JsonProperty("evlu_pfls_amt") BigDecimal profitLoss,
        @JsonProperty("evlu_pfls_rt") BigDecimal profitLossRate,
        @JsonProperty("pchs_amt") BigDecimal purchaseAmount
    ) {}

    public record AccountSummary(
        @JsonProperty("dnca_tot_amt") BigDecimal totalDeposit,
        @JsonProperty("nxdy_excc_amt") BigDecimal availableDeposit,
        @JsonProperty("prvs_rcdl_excc_amt") BigDecimal withdrawableAmount,
        @JsonProperty("tot_evlu_amt") BigDecimal totalEvaluationAmount,
        @JsonProperty("evlu_pfls_smtl_amt") BigDecimal totalProfitLoss
    ) {}
}
