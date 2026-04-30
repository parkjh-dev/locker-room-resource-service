package com.lockerroom.resourceservice.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "투표 요청 — 한 번 투표하면 변경 불가 (idempotent).")
public record PollVoteRequest(
        @Schema(description = "선택한 옵션 ID", example = "1")
        @NotNull Long optionId
) {
}
