package me.singingsandhill.calendar.runner.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record RunCreateRequest(
    @NotNull(message = "날짜를 입력해주세요")
    LocalDate date,

    @NotNull(message = "시간을 입력해주세요")
    LocalTime time,

    @NotBlank(message = "장소를 입력해주세요")
    @Size(max = 100, message = "장소는 100자 이내로 입력해주세요")
    String location,

    @NotNull(message = "카테고리를 선택해주세요")
    String category
) {}
