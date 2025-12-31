package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank(message = "Menu name is required")
        @Size(max = 100, message = "Menu name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Menu URL cannot exceed 500 characters")
        String url
) {
}
