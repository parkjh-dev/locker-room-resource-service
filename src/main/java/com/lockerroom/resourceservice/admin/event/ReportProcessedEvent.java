package com.lockerroom.resourceservice.admin.event;

public record ReportProcessedEvent(
        String eventId,
        Long userId,
        Long reportId,
        String decision
) {
}
