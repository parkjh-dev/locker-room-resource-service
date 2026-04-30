package com.lockerroom.resourceservice.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "응원팀 등록 요청. 종목별 락: 한 번 등록한 종목은 변경 불가, 미등록 종목은 추가 가능.")
public record AddUserTeamsRequest(
        @Schema(description = "등록할 응원팀 목록 (종목별 1개씩, 미등록 종목만 가능)")
        @NotEmpty(message = "최소 1개 이상의 응원팀을 등록해야 합니다.")
        @Valid
        List<TeamSelection> teams
) {
        @Schema(description = "선택한 종목·팀 페어")
        public record TeamSelection(
                @Schema(description = "종목 ID", example = "1")
                @NotNull Long sportId,

                @Schema(description = "팀 ID (종목과 매칭되어야 함)", example = "101")
                @NotNull Long teamId
        ) {
        }
}
