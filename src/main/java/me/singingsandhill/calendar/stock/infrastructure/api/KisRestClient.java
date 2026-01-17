package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.infrastructure.api.dto.*;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 한국투자증권 REST API 클라이언트
 */
@Component
public class KisRestClient {

    private static final Logger log = LoggerFactory.getLogger(KisRestClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient webClient;
    private final KisAuthService authService;
    private final StockProperties stockProperties;

    public KisRestClient(WebClient.Builder webClientBuilder,
                         KisAuthService authService,
                         StockProperties stockProperties) {
        this.authService = authService;
        this.stockProperties = stockProperties;
        this.webClient = webClientBuilder
            .baseUrl(stockProperties.getKis().getBaseUrl())
            .build();
    }

    // ========== 시세 조회 APIs ==========

    /**
     * 주식현재가 시세 조회 (FHKST01010100)
     */
    public KisQuoteResponse getQuote(String stockCode) {
        log.debug("Fetching quote for {}", stockCode);

        if (!authService.isConfigured()) {
            log.warn("KIS API not configured, skipping quote fetch");
            return null;
        }

        Map<String, String> headers = authService.buildAuthHeaders("FHKST01010100");

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                return mapToQuoteResponse(output, stockCode);
            }
            return null;
        } catch (WebClientResponseException e) {
            logApiError("fetching quote for " + stockCode, e);
            return null;
        } catch (Exception e) {
            log.error("Error fetching quote for {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    private KisQuoteResponse mapToQuoteResponse(Map<String, Object> output, String stockCode) {
        return new KisQuoteResponse(
            stockCode,
            parseBigDecimal(output.get("stck_prpr")),
            parseBigDecimal(output.get("stck_oprc")),
            parseBigDecimal(output.get("stck_hgpr")),
            parseBigDecimal(output.get("stck_lwpr")),
            parseBigDecimal(output.get("stck_sdpr")),
            parseBigDecimal(output.get("prdy_vrss")),
            parseBigDecimal(output.get("prdy_ctrt")),
            parseLong(output.get("acml_vol")),
            parseBigDecimal(output.get("acml_tr_pbmn")),
            parseBigDecimal(output.get("hts_avls")),
            parseBigDecimal(output.get("vol_tnrt")),
            parseBigDecimal(output.get("seln_cntg_smtn")),
            parseBigDecimal(output.get("shnu_cntg_smtn"))
        );
    }

    /**
     * 호가 조회 (FHKST01010200)
     */
    public KisOrderbookResponse getOrderbook(String stockCode) {
        log.debug("Fetching orderbook for {}", stockCode);

        if (!authService.isConfigured()) {
            return null;
        }

        Map<String, String> headers = authService.buildAuthHeaders("FHKST01010200");

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output1");
                return mapToOrderbookResponse(output, stockCode);
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching orderbook for {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    private KisOrderbookResponse mapToOrderbookResponse(Map<String, Object> output, String stockCode) {
        if (output == null) {
            log.error("Cannot map orderbook response: output is null for stock {}", stockCode);
            return null;
        }

        return new KisOrderbookResponse(
            stockCode,
            parseBigDecimal(output.get("askp1")),
            parseBigDecimal(output.get("askp2")),
            parseBigDecimal(output.get("askp3")),
            parseBigDecimal(output.get("bidp1")),
            parseBigDecimal(output.get("bidp2")),
            parseBigDecimal(output.get("bidp3")),
            parseLong(output.get("askp_rsqn1")),
            parseLong(output.get("askp_rsqn2")),
            parseLong(output.get("askp_rsqn3")),
            parseLong(output.get("bidp_rsqn1")),
            parseLong(output.get("bidp_rsqn2")),
            parseLong(output.get("bidp_rsqn3")),
            parseLong(output.get("total_askp_rsqn")),
            parseLong(output.get("total_bidp_rsqn"))
        );
    }

    /**
     * 일자별 시세 조회 (FHKST01010400)
     */
    public List<KisDailyPriceResponse> getDailyPrices(String stockCode, int days) {
        log.debug("Fetching {} days of daily prices for {}", days, stockCode);

        if (!authService.isConfigured()) {
            return Collections.emptyList();
        }

        Map<String, String> headers = authService.buildAuthHeaders("FHKST01010400");

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                    .queryParam("FID_ORG_ADJ_PRC", "0")
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) response.get("output");
                if (outputs != null) {
                    return outputs.stream()
                        .limit(days)
                        .map(this::mapToDailyPriceResponse)
                        .toList();
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching daily prices for {}: {}", stockCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    private KisDailyPriceResponse mapToDailyPriceResponse(Map<String, Object> output) {
        return new KisDailyPriceResponse(
            (String) output.get("stck_bsop_date"),
            parseBigDecimal(output.get("stck_oprc")),
            parseBigDecimal(output.get("stck_hgpr")),
            parseBigDecimal(output.get("stck_lwpr")),
            parseBigDecimal(output.get("stck_clpr")),
            parseLong(output.get("acml_vol")),
            parseBigDecimal(output.get("acml_tr_pbmn")),
            parseBigDecimal(output.get("prdy_vrss")),
            (String) output.get("prdy_vrss_sign"),
            parseBigDecimal(output.get("prdy_ctrt"))
        );
    }

    // ========== 계좌 조회 APIs ==========

    /**
     * 주식잔고조회 (TTTC8434R)
     */
    public KisBalanceResponse getBalance() {
        log.debug("Fetching account balance");

        if (!authService.isConfigured()) {
            return null;
        }

        String trId = stockProperties.getKis().isProduction() ? "TTTC8434R" : "VTTC8434R";
        Map<String, String> headers = authService.buildAuthHeaders(trId);

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/trading/inquire-balance")
                    .queryParam("CANO", authService.getAccountNumber())
                    .queryParam("ACNT_PRDT_CD", authService.getAccountProductCode())
                    .queryParam("AFHR_FLPR_YN", "N")
                    .queryParam("INQR_DVSN", "02")
                    .queryParam("UNPR_DVSN", "01")
                    .queryParam("FUND_STTL_ICLD_YN", "N")
                    .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                    .queryParam("PRCS_DVSN", "00")
                    .queryParam("CTX_AREA_FK100", "")
                    .queryParam("CTX_AREA_NK100", "")
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                return mapToBalanceResponse(response);
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching balance: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private KisBalanceResponse mapToBalanceResponse(Map<String, Object> response) {
        List<Map<String, Object>> output1 = (List<Map<String, Object>>) response.get("output1");
        List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");

        List<KisBalanceResponse.HoldingStock> holdings = output1 != null
            ? output1.stream().map(this::mapToHoldingStock).toList()
            : Collections.emptyList();

        List<KisBalanceResponse.AccountSummary> summaries = output2 != null
            ? output2.stream().map(this::mapToAccountSummary).toList()
            : Collections.emptyList();

        return new KisBalanceResponse(holdings, summaries);
    }

    private KisBalanceResponse.HoldingStock mapToHoldingStock(Map<String, Object> output) {
        return new KisBalanceResponse.HoldingStock(
            (String) output.get("pdno"),
            (String) output.get("prdt_name"),
            parseInteger(output.get("hldg_qty")),
            parseBigDecimal(output.get("pchs_avg_pric")),
            parseBigDecimal(output.get("prpr")),
            parseBigDecimal(output.get("evlu_amt")),
            parseBigDecimal(output.get("evlu_pfls_amt")),
            parseBigDecimal(output.get("evlu_pfls_rt")),
            parseBigDecimal(output.get("pchs_amt"))
        );
    }

    private KisBalanceResponse.AccountSummary mapToAccountSummary(Map<String, Object> output) {
        return new KisBalanceResponse.AccountSummary(
            parseBigDecimal(output.get("dnca_tot_amt")),
            parseBigDecimal(output.get("nxdy_excc_amt")),
            parseBigDecimal(output.get("prvs_rcdl_excc_amt")),
            parseBigDecimal(output.get("tot_evlu_amt")),
            parseBigDecimal(output.get("evlu_pfls_smtl_amt"))
        );
    }

    /**
     * 매수가능조회 (TTTC8908R)
     */
    public KisBuyingPowerResponse getBuyingPower(String stockCode, BigDecimal price) {
        log.debug("Fetching buying power for {} at price {}", stockCode, price);

        if (!authService.isConfigured()) {
            return null;
        }

        String trId = stockProperties.getKis().isProduction() ? "TTTC8908R" : "VTTC8908R";
        Map<String, String> headers = authService.buildAuthHeaders(trId);

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/trading/inquire-psbl-order")
                    .queryParam("CANO", authService.getAccountNumber())
                    .queryParam("ACNT_PRDT_CD", authService.getAccountProductCode())
                    .queryParam("PDNO", stockCode)
                    .queryParam("ORD_UNPR", price.toPlainString())
                    .queryParam("ORD_DVSN", "01")
                    .queryParam("CMA_EVLU_AMT_ICLD_YN", "N")
                    .queryParam("OVRS_ICLD_YN", "N")
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                return mapToBuyingPowerResponse(output);
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching buying power: {}", e.getMessage());
            return null;
        }
    }

    private KisBuyingPowerResponse mapToBuyingPowerResponse(Map<String, Object> output) {
        KisBuyingPowerResponse.BuyingPower power = new KisBuyingPowerResponse.BuyingPower(
            parseBigDecimal(output.get("ord_psbl_cash")),
            parseBigDecimal(output.get("ord_psbl_sbst")),
            parseBigDecimal(output.get("ruse_psbl_amt")),
            parseBigDecimal(output.get("max_buy_amt")),
            parseInteger(output.get("max_buy_qty")),
            parseBigDecimal(output.get("nrcvb_buy_amt")),
            parseInteger(output.get("nrcvb_buy_qty"))
        );
        return new KisBuyingPowerResponse(power);
    }

    // ========== 주문 APIs ==========

    /**
     * 매수주문 (TTTC0012U)
     */
    public KisOrderResponse placeBuyOrder(String stockCode, int quantity, BigDecimal price, boolean isMarketOrder) {
        log.info("Placing buy order: {} x {} @ {}", stockCode, quantity, isMarketOrder ? "market" : price);

        String trId = stockProperties.getKis().isProduction() ? "TTTC0802U" : "VTTC0802U";
        String orderType = isMarketOrder ? "01" : "00";

        return executeOrder(trId, stockCode, quantity, price, orderType);
    }

    /**
     * 매도주문 (TTTC0011U)
     */
    public KisOrderResponse placeSellOrder(String stockCode, int quantity, BigDecimal price, boolean isMarketOrder) {
        log.info("Placing sell order: {} x {} @ {}", stockCode, quantity, isMarketOrder ? "market" : price);

        String trId = stockProperties.getKis().isProduction() ? "TTTC0801U" : "VTTC0801U";
        String orderType = isMarketOrder ? "01" : "00";

        return executeOrder(trId, stockCode, quantity, price, orderType);
    }

    private KisOrderResponse executeOrder(String trId, String stockCode, int quantity, BigDecimal price, String orderType) {
        if (!authService.isConfigured()) {
            log.warn("KIS API not configured, skipping order");
            return null;
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("CANO", authService.getAccountNumber());
        requestBody.put("ACNT_PRDT_CD", authService.getAccountProductCode());
        requestBody.put("PDNO", stockCode);
        requestBody.put("ORD_DVSN", orderType);
        requestBody.put("ORD_QTY", String.valueOf(quantity));
        requestBody.put("ORD_UNPR", price != null ? price.toPlainString() : "0");

        String hashkey = authService.generateHashkey(requestBody);
        Map<String, String> headers = authService.buildAuthHeaders(trId);

        try {
            Map<String, Object> response = webClient.post()
                .uri("/uapi/domestic-stock/v1/trading/order-cash")
                .headers(h -> {
                    headers.forEach(h::set);
                    if (hashkey != null) {
                        h.set("hashkey", hashkey);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null) {
                String resultCode = (String) response.get("rt_cd");
                String msgCode = (String) response.get("msg_cd");
                String msg = (String) response.get("msg1");

                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                KisOrderResponse.OrderOutput orderOutput = null;
                if (output != null) {
                    orderOutput = new KisOrderResponse.OrderOutput(
                        (String) output.get("ODNO"),
                        (String) output.get("ORD_TMD")
                    );
                }

                KisOrderResponse orderResponse = new KisOrderResponse(resultCode, msgCode, msg, orderOutput);

                if (orderResponse.isSuccess()) {
                    log.info("Order placed successfully: {}", orderResponse.getOrderId());
                } else {
                    log.error("Order failed: [{}] {}", msgCode, msg);
                }

                return orderResponse;
            }
            return null;
        } catch (WebClientResponseException e) {
            logApiError("placing order", e);
            return null;
        } catch (Exception e) {
            log.error("Error placing order: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 일별주문체결조회 (TTTC0081R)
     */
    public KisOrderDetailResponse getOrderHistory(LocalDate date) {
        log.debug("Fetching order history for {}", date);

        if (!authService.isConfigured()) {
            return null;
        }

        String trId = stockProperties.getKis().isProduction() ? "TTTC8001R" : "VTTC8001R";
        Map<String, String> headers = authService.buildAuthHeaders(trId);
        String dateStr = date.format(DATE_FORMATTER);

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/trading/inquire-daily-ccld")
                    .queryParam("CANO", authService.getAccountNumber())
                    .queryParam("ACNT_PRDT_CD", authService.getAccountProductCode())
                    .queryParam("INQR_STRT_DT", dateStr)
                    .queryParam("INQR_END_DT", dateStr)
                    .queryParam("SLL_BUY_DVSN_CD", "00")
                    .queryParam("INQR_DVSN", "00")
                    .queryParam("PDNO", "")
                    .queryParam("CCLD_DVSN", "00")
                    .queryParam("ORD_GNO_BRNO", "")
                    .queryParam("ODNO", "")
                    .queryParam("INQR_DVSN_3", "00")
                    .queryParam("INQR_DVSN_1", "")
                    .queryParam("CTX_AREA_FK100", "")
                    .queryParam("CTX_AREA_NK100", "")
                    .build())
                .headers(h -> headers.forEach(h::set))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .block();

            if (response != null && "0".equals(response.get("rt_cd"))) {
                return mapToOrderDetailResponse(response);
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching order history: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private KisOrderDetailResponse mapToOrderDetailResponse(Map<String, Object> response) {
        List<Map<String, Object>> output1 = (List<Map<String, Object>>) response.get("output1");

        List<KisOrderDetailResponse.OrderDetail> orders = output1 != null
            ? output1.stream().map(this::mapToOrderDetail).toList()
            : Collections.emptyList();

        return new KisOrderDetailResponse(
            (String) response.get("rt_cd"),
            (String) response.get("msg_cd"),
            (String) response.get("msg1"),
            orders
        );
    }

    private KisOrderDetailResponse.OrderDetail mapToOrderDetail(Map<String, Object> output) {
        return new KisOrderDetailResponse.OrderDetail(
            (String) output.get("odno"),
            (String) output.get("pdno"),
            (String) output.get("prdt_name"),
            (String) output.get("sll_buy_dvsn_cd"),
            parseInteger(output.get("ord_qty")),
            parseBigDecimal(output.get("ord_unpr")),
            parseInteger(output.get("tot_ccld_qty")),
            parseBigDecimal(output.get("avg_prvs")),
            (String) output.get("ord_tmd"),
            (String) output.get("ord_gno_brno"),
            (String) output.get("orgn_odno"),
            (String) output.get("ord_dvsn_name"),
            (String) output.get("ccld_cndt_name")
        );
    }

    // ========== Utility Methods ==========

    private void logApiError(String operation, WebClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.error("Authentication failed while {}: {} - Check API keys", operation, e.getResponseBodyAsString());
        } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Rate limited while {}: {}", operation, e.getResponseBodyAsString());
        } else {
            log.error("API error while {}: {} - {}", operation, e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        String str = value.toString().trim();
        if (str.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal from value '{}': {}. Returning ZERO.", value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        String str = value.toString().trim();
        if (str.isEmpty()) return 0L;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value '{}': {}. Returning 0.", value, e.getMessage());
            return 0L;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        String str = value.toString().trim();
        if (str.isEmpty()) return 0;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Integer from value '{}': {}. Returning 0.", value, e.getMessage());
            return 0;
        }
    }
}
