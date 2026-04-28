package com.lockerroom.resourceservice.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게시글 신고 요청.")
public record ReportRequest(
        @Schema(description = "신고 사유 (최대 500자)", example = "욕설 포함", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 500) String reason
) {
}
