package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeSlotCreateRequest(
        @NotNull(message = "날짜를 선택해주세요")
        @Min(value = 1, message = "날짜가 올바르지 않습니다")
        Integer dayIndex,

        @NotNull(message = "시작 시간을 선택해주세요")
        @Min(value = 0, message = "시작 시간은 00:00~23:30 사이여야 합니다")
        @Max(value = 1410, message = "시작 시간은 00:00~23:30 사이여야 합니다")
        Integer startMinute,

        @NotNull(message = "종료 시간을 선택해주세요")
        @Min(value = 30, message = "종료 시간은 00:30~24:00 사이여야 합니다")
        @Max(value = 1440, message = "종료 시간은 00:30~24:00 사이여야 합니다")
        Integer endMinute
) {
}
