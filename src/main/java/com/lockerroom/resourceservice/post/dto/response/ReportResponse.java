package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신고 접수 직후 응답. 신고 ID와 초기 상태(PENDING).")
public record ReportResponse(
        @Schema(description = "신고 ID", example = "55")
        Long reportId,

        @Schema(description = "신고된 게시글 ID", example = "1024")
        Long postId,

        @Schema(description = "처리 상태 (접수 직후엔 PENDING)")
        ReportStatus status
) {
}
