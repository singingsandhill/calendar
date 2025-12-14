package me.singingsandhill.calendar.presentation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SelectionUpdateRequest(
        @NotNull(message = "Selections list is required")
        List<Integer> selections
) {
}
