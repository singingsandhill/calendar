package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationCreateRequest(
        @NotBlank(message = "장소 이름을 입력해주세요")
        @Size(max = 100, message = "장소 이름은 100자 이내로 입력해주세요")
        String name
) {
}
