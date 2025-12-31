package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerCreateRequest(
        @NotBlank(message = "Owner ID is required")
        @Size(min = 2, max = 20, message = "Owner ID must be between 2 and 20 characters")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Owner ID can only contain lowercase letters, numbers, and hyphens")
        String ownerId
) {
}
