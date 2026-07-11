package me.singingsandhill.calendar.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bithumb v2 주문 생성(POST /v2/orders) 응답.
 * v1 과 달리 state/executed_volume/trades 가 없고 접수 확인용 식별자만 반환한다
 * → 체결 정보는 GET /v1/order 재조회로 정규화해야 한다(BithumbV2OrderApi.normalize).
 */
public record BithumbV2OrderCreateResponse(
    @JsonProperty("order_id") String orderId,
    @JsonProperty("client_order_id") String clientOrderId,
    @JsonProperty("market") String market,
    @JsonProperty("side") String side,
    @JsonProperty("order_type") String orderType,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("stp_type") String stpType,
    @JsonProperty("time_in_force") String timeInForce
) {}
