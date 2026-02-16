package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        List<Long> fileIds
) {
}
