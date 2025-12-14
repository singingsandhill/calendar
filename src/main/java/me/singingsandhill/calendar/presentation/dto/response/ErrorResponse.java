package me.singingsandhill.calendar.presentation.dto.response;

public record ErrorResponse(
        String code,
        String message
) {
}
