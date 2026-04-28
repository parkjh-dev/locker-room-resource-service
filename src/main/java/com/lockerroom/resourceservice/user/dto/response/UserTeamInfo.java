package com.lockerroom.resourceservice.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자가 응원하는 팀 정보 (종목별 1개).")
public record UserTeamInfo(
        @Schema(description = "팀 ID", example = "7")
        Long teamId,

        @Schema(description = "팀명 (한국어)", example = "두산 베어스")
        String teamName,

        @Schema(description = "종목 ID", example = "2")
        Long sportId,

        @Schema(description = "종목명 (한국어)", example = "야구")
        String sportName
) {
}
