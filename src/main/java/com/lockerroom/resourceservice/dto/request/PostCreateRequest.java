package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotNull Long boardId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        List<Long> fileIds
) {
}
