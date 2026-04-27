package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record SuspendRequest(
        @NotBlank @Size(max = 500) String reason,
        @NotNull @Future OffsetDateTime suspendedUntil
) {
}
