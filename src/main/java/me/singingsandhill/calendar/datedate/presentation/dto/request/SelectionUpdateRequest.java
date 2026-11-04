package me.singingsandhill.calendar.datedate.presentation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SelectionUpdateRequest(
        @NotNull(message = "{validation.selections.required}")
        List<Integer> selections
) {
}
