package com.lockerroom.resourceservice.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글/답글 작성 요청.")
public record CommentCreateRequest(
        @Schema(description = "댓글 내용 (최대 1000자)", example = "좋은 글 잘 봤습니다!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 1000) String content
) {
}
