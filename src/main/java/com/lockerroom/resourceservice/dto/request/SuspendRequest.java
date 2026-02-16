package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SuspendRequest(
        @NotBlank String reason,
        @NotNull @Future LocalDateTime suspendedUntil
) {
}
