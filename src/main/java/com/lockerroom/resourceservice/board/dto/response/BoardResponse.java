package com.lockerroom.resourceservice.board.dto.response;

import com.lockerroom.resourceservice.board.model.enums.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 응답. 게시판 목록 조회 시 한 행.")
public record BoardResponse(
        @Schema(description = "게시판 ID", example = "1")
        Long id,

        @Schema(description = "게시판 이름", example = "전북 현대 게시판")
        String name,

        @Schema(description = "게시판 유형 (COMMON, QNA, NOTICE, TEAM, NEWS)")
        BoardType type,

        @Schema(description = "팀 게시판일 때 팀 ID. 그 외엔 null.", example = "101", nullable = true)
        Long teamId,

        @Schema(description = "팀 게시판일 때 팀명. 그 외엔 null.", example = "전북 현대 모터스", nullable = true)
        String teamName
) {
}
