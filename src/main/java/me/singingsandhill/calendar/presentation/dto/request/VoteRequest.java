package me.singingsandhill.calendar.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoteRequest(
        @NotBlank(message = "Voter name is required")
        @Size(max = 10, message = "Voter name cannot exceed 10 characters")
        String voterName
) {
}
