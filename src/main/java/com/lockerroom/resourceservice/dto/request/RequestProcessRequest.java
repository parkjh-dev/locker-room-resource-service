package com.lockerroom.resourceservice.dto.request;

import com.lockerroom.resourceservice.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record RequestProcessRequest(
        @NotNull RequestStatus status,
        String rejectReason,
        Long sportId,
        Long leagueId
) {
}
