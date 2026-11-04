package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoteRequest(
        @NotBlank(message = "{validation.voter.name.required}")
        @Size(max = 10, message = "{validation.voter.name.size}")
        String voterName
) {
}
