package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerCreateRequest(
        @NotBlank(message = "사용자 ID를 입력해주세요")
        @Size(min = 2, max = 20, message = "사용자 ID는 2자 이상 20자 이하로 입력해주세요")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "사용자 ID는 영문 소문자, 숫자, 하이픈(-)만 사용할 수 있습니다")
        String ownerId
) {
}
