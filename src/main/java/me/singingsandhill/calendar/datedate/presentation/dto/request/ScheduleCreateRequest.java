package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleCreateRequest(
        @NotNull(message = "{validation.schedule.year.required}")
        @Min(value = 2024, message = "{validation.schedule.year.min}")
        @Max(value = 2100, message = "{validation.schedule.year.max}")
        Integer year,

        @NotNull(message = "{validation.schedule.month.required}")
        @Min(value = 1, message = "{validation.schedule.month.range}")
        @Max(value = 12, message = "{validation.schedule.month.range}")
        Integer month,

        @Min(value = 4, message = "{validation.schedule.weeks.createRange}")
        @Max(value = 7, message = "{validation.schedule.weeks.createRange}")
        Integer weeks
) {
}
