package com.lockerroom.resourceservice.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        boolean isPinned
) {
}
