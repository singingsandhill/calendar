package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank(message = "{validation.menu.name.required}")
        @Size(max = 100, message = "{validation.menu.name.size}")
        String name,

        @Size(max = 500, message = "{validation.menu.url.size}")
        String url
) {
}
