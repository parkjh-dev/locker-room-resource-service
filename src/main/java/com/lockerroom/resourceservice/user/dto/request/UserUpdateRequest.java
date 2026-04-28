package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UserUpdateRequest(
        @Size(min = 2, max = 50) String nickname,
        String currentPassword,
        @Size(min = 8, max = 100) String newPassword,
        @URL @Size(max = 500) String profileImageUrl
) {
}
