package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleCreateRequest(
        @NotNull(message = "Year is required")
        @Min(value = 2024, message = "Year must be at least 2024")
        @Max(value = 2100, message = "Year must be at most 2100")
        Integer year,

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,

        @Min(value = 4, message = "Weeks must be between 4 and 7")
        @Max(value = 7, message = "Weeks must be between 4 and 7")
        Integer weeks
) {
}
