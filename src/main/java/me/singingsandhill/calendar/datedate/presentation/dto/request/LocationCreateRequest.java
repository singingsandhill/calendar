package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationCreateRequest(
        @NotBlank(message = "Location name is required")
        @Size(max = 100, message = "Location name cannot exceed 100 characters")
        String name
) {
}
