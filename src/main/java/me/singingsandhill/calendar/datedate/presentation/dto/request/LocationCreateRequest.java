package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationCreateRequest(
        @NotBlank(message = "{validation.location.name.required}")
        @Size(max = 100, message = "{validation.location.name.size}")
        String name
) {
}
