package com.lockerroom.resourceservice.dto.request;

import com.lockerroom.resourceservice.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportProcessRequest(
        @NotNull ReportStatus status,
        String action
) {
}
