package com.lockerroom.resourceservice.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 투표 응답. 마감 후엔 결과만 노출 (프론트가 expiresAt으로 closed 처리).")
public record PollResponse(
        @Schema(description = "투표 질문 (없으면 본문이 질문 역할)", nullable = true)
        String question,

        @Schema(description = "옵션 목록 (등록 순서)")
        List<PollOptionResponse> options,

        @Schema(description = "투표 마감 시각", example = "2026-05-03T19:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "전체 투표 수", example = "90")
        int totalVotes,

        @Schema(description = "현재 사용자가 선택한 옵션 ID. 미투표·익명이면 null.", nullable = true)
        Long myVoteOptionId
) {
}
