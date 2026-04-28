package com.lockerroom.resourceservice.board.dto.response;

import com.lockerroom.resourceservice.board.model.enums.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 응답. 게시판 목록 조회 시 한 행.")
public record BoardResponse(
        @Schema(description = "게시판 ID", example = "1")
        Long id,

        @Schema(description = "게시판 이름", example = "공통 게시판")
        String name,

        @Schema(description = "게시판 유형 (COMMON, QNA, NOTICE 등)")
        BoardType type
) {
}
