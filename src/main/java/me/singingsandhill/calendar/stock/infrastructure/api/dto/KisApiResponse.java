package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KIS API 공통 응답 래퍼
 */
public record KisApiResponse<T>(
    @JsonProperty("rt_cd") String resultCode,
    @JsonProperty("msg_cd") String messageCode,
    @JsonProperty("msg1") String message,
    @JsonProperty("output") T output
) {
    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
