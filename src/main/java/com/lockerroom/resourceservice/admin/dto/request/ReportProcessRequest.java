package com.lockerroom.resourceservice.admin.dto.request;

import com.lockerroom.resourceservice.post.model.enums.ReportAction;
import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReportProcessRequest(
        @NotNull ReportStatus status,
        ReportAction action,
        @Min(1) @Max(365) Integer suspensionDays
) {
}
