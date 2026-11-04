package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantCreateRequest(
        @NotBlank(message = "{validation.participant.name.required}")
        @Size(max = 10, message = "{validation.participant.name.size}")
        String name
) {
}
