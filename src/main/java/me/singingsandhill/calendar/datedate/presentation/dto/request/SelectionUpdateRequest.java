package me.singingsandhill.calendar.datedate.presentation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SelectionUpdateRequest(
        @NotNull(message = "날짜 선택 목록을 입력해주세요")
        List<Integer> selections
) {
}
