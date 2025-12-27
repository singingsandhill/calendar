package me.singingsandhill.calendar.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AttendanceCreateRequest(
    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
    String participantName,

    @NotNull(message = "거리를 입력해주세요")
    @DecimalMin(value = "0.1", message = "거리는 최소 0.1km 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "거리는 최대 100km 이하여야 합니다")
    BigDecimal distance
) {}
