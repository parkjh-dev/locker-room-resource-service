package com.lockerroom.resourceservice.request.dto.request;

import com.lockerroom.resourceservice.request.model.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestCreateRequest(
        @NotNull RequestType type,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2000) String reason
) {
}
