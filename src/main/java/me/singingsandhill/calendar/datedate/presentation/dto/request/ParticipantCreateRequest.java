package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantCreateRequest(
        @NotBlank(message = "Participant name is required")
        @Size(max = 10, message = "Participant name cannot exceed 10 characters")
        String name
) {
}
