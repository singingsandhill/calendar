package me.singingsandhill.calendar.datedate.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoteRequest(
        @NotBlank(message = "투표자 이름을 입력해주세요")
        @Size(max = 10, message = "투표자 이름은 10자 이내로 입력해주세요")
        String voterName
) {
}
