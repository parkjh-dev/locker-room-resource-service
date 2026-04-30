package com.lockerroom.resourceservice.post.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "투표 작성 페이로드. 게시글 작성 시 함께 전송. 옵션 2~5개, 각 50자 이하, 중복 불가.")
public record PollPayload(
        @Schema(description = "투표 질문 (선택). null이면 본문 자체가 질문 역할.", example = "오늘 MVP는?", nullable = true)
        @Size(max = 120) String question,

        @Schema(description = "투표 옵션 텍스트 목록 (2~5개, trim 후 중복 불가)", example = "[\"손흥민\", \"황희찬\", \"이강인\"]")
        @NotEmpty
        @Size(min = 2, max = 5, message = "투표 옵션은 2~5개여야 합니다.")
        List<@NotNull @Size(min = 1, max = 50) String> options,

        @Schema(description = "마감 시각 (미래)", example = "2026-05-03T19:00:00")
        @NotNull @Future LocalDateTime expiresAt
) {
    /**
     * 옵션 텍스트 중복 검증 — trim 후 비교.
     * 클라이언트 검증을 신뢰하지 않고 백엔드도 보호.
     */
    @JsonIgnore
    @AssertTrue(message = "투표 옵션에 중복된 텍스트가 있습니다.")
    public boolean isOptionsUnique() {
        if (options == null) return true; // @NotEmpty가 별도 처리
        Set<String> seen = new HashSet<>();
        for (String opt : options) {
            if (opt == null) continue; // 내부 @NotNull이 별도 처리
            if (!seen.add(opt.trim())) return false;
        }
        return true;
    }
}
