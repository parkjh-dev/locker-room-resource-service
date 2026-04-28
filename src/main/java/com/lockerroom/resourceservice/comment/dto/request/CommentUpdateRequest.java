package com.lockerroom.resourceservice.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 수정 요청.")
public record CommentUpdateRequest(
        @Schema(description = "수정할 댓글 내용 (최대 1000자)", example = "오타 수정합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 1000) String content
) {
}
