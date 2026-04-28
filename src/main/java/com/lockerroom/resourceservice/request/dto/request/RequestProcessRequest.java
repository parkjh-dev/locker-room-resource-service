package com.lockerroom.resourceservice.request.dto.request;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record RequestProcessRequest(
        @NotNull RequestStatus status,
        String rejectReason,
        Long sportId,
        Long leagueId
) {
}
