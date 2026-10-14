package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

public record ScheduleUpdateRequest(
        @Min(value = 4, message = "주 수는 4에서 6 사이여야 합니다")
        @Max(value = 6, message = "주 수는 4에서 6 사이여야 합니다")
        Integer weeks,

        // null = 그대로, "" = 지우기 (ADR datedate/domain/0011)
        @Size(max = Schedule.MAX_DESCRIPTION_LENGTH, message = "설명은 500자 이내로 입력해주세요")
        String description
) {
}
