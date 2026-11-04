package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeSlotCreateRequest(
        @NotNull(message = "{validation.timeSlot.day.required}")
        @Min(value = 1, message = "{validation.timeSlot.day.invalid}")
        Integer dayIndex,

        @NotNull(message = "{validation.timeSlot.start.required}")
        @Min(value = 0, message = "{validation.timeSlot.start.range}")
        @Max(value = 1410, message = "{validation.timeSlot.start.range}")
        Integer startMinute,

        @NotNull(message = "{validation.timeSlot.end.required}")
        @Min(value = 30, message = "{validation.timeSlot.end.range}")
        @Max(value = 1440, message = "{validation.timeSlot.end.range}")
        Integer endMinute
) {
}
