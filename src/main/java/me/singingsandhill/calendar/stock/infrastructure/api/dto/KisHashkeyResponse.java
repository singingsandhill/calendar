package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hashkey 발급 응답
 */
public record KisHashkeyResponse(
    @JsonProperty("HASH") String hashkey,
    @JsonProperty("BODY") Object body
) {}
