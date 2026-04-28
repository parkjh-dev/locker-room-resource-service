package com.lockerroom.resourceservice.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "좋아요 토글 후 응답. 변경된 상태와 누적 카운트 반환.")
public record LikeResponse(
        @Schema(description = "게시글 ID", example = "1024")
        Long postId,

        @Schema(description = "토글 후 사용자의 좋아요 상태 (true=좋아요, false=취소)", example = "true")
        boolean isLiked,

        @Schema(description = "토글 반영 후 누적 좋아요 수", example = "43")
        int likeCount
) {
}
