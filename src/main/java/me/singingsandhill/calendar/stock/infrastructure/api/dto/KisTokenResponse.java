package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 접근토큰발급 응답 (oauth2/tokenP)
 */
public record KisTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("access_token_token_expired") String tokenExpiredTime
) {}
