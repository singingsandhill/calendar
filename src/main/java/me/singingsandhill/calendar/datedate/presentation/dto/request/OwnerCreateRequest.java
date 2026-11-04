package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerCreateRequest(
        @NotBlank(message = "{validation.ownerId.required}")
        @Size(min = 2, max = 20, message = "{validation.ownerId.size}")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "{validation.ownerId.pattern}")
        String ownerId
) {
}
