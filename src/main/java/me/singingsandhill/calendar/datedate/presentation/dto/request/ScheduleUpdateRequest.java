package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ScheduleUpdateRequest(
        @Min(value = 4, message = "주 수는 4에서 6 사이여야 합니다")
        @Max(value = 6, message = "주 수는 4에서 6 사이여야 합니다")
        Integer weeks
) {
}
