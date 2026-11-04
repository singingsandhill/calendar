package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

public record ScheduleUpdateRequest(
        @Min(value = 4, message = "{validation.schedule.weeks.updateRange}")
        @Max(value = 6, message = "{validation.schedule.weeks.updateRange}")
        Integer weeks,

        // null = 그대로, "" = 지우기 (ADR datedate/domain/0011)
        @Size(max = Schedule.MAX_DESCRIPTION_LENGTH, message = "{validation.schedule.description.size}")
        String description
) {
}
