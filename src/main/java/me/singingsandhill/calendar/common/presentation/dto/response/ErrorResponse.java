package me.singingsandhill.calendar.common.presentation.dto.response;

public record ErrorResponse(
        String code,
        String message
) {
}
