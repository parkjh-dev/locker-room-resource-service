package com.lockerroom.resourceservice.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 5) List<Long> fileIds
) {
}
