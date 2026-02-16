package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 2, max = 50) String nickname,
        String currentPassword,
        @Size(min = 8, max = 100) String newPassword
) {
}
