package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank(message = "메뉴 이름을 입력해주세요")
        @Size(max = 100, message = "메뉴 이름은 100자 이내로 입력해주세요")
        String name,

        @Size(max = 500, message = "URL은 500자 이내로 입력해주세요")
        String url
) {
}
