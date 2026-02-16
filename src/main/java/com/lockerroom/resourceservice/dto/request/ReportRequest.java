package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportRequest(
        @NotBlank String reason
) {
}
