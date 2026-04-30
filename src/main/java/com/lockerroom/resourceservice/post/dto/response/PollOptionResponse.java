package com.lockerroom.resourceservice.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 옵션 응답.")
public record PollOptionResponse(
        @Schema(description = "옵션 ID", example = "1")
        Long id,

        @Schema(description = "옵션 텍스트", example = "손흥민")
        String text,

        @Schema(description = "이 옵션의 투표 수", example = "45")
        int voteCount
) {
}
