package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ScheduleUpdateRequest(
        @Min(value = 4, message = "Weeks must be between 4 and 6")
        @Max(value = 6, message = "Weeks must be between 4 and 6")
        Integer weeks
) {
}
