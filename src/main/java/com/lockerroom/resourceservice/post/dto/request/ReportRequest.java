package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
