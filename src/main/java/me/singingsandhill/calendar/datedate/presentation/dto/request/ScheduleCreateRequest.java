package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleCreateRequest(
        @NotNull(message = "연도를 입력해주세요")
        @Min(value = 2024, message = "연도는 2024년 이상이어야 합니다")
        @Max(value = 2100, message = "연도는 2100년 이하이어야 합니다")
        Integer year,

        @NotNull(message = "월을 입력해주세요")
        @Min(value = 1, message = "월은 1에서 12 사이여야 합니다")
        @Max(value = 12, message = "월은 1에서 12 사이여야 합니다")
        Integer month,

        @Min(value = 4, message = "주 수는 4에서 7 사이여야 합니다")
        @Max(value = 7, message = "주 수는 4에서 7 사이여야 합니다")
        Integer weeks
) {
}
