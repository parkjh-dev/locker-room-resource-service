package com.lockerroom.resourceservice.post.dto.request;

import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 수정 요청. 게시판 이동·투표 변경은 불가 (참여자 표 보호).")
public record PostUpdateRequest(
        @Schema(description = "수정할 제목 (최대 200자)", example = "두산 베어스 응원합니다 (수정)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "수정할 본문 (최대 10000자)", example = "내용을 보강했습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10000) String content,

        @Schema(description = "수정할 말머리. null이면 기존값 유지.", nullable = true)
        PostCategory category,

        @Schema(description = "첨부파일 ID 목록 (최대 5개). 빈 배열 전달 시 모든 첨부 제거.", example = "[101]", nullable = true)
        @Size(max = 5) List<Long> fileIds
) {
}
