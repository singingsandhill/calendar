package me.singingsandhill.calendar.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank(message = "Menu name is required")
        @Size(max = 100, message = "Menu name cannot exceed 100 characters")
        String name
) {
}
