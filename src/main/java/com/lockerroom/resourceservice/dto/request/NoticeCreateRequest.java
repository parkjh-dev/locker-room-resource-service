package com.lockerroom.resourceservice.dto.request;

import com.lockerroom.resourceservice.model.enums.NoticeScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        boolean isPinned,
        @NotNull NoticeScope scope,
        Long teamId
) {
}
