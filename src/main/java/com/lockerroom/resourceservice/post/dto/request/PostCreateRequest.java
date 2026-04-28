package com.lockerroom.resourceservice.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 작성 요청. 첨부파일은 사전 업로드 후 ID 전달.")
public record PostCreateRequest(
        @Schema(description = "작성 대상 게시판 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long boardId,

        @Schema(description = "게시글 제목 (최대 200자)", example = "두산 베어스 응원합니다", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "본문 (최대 10000자, Markdown 미지원)", example = "오늘 경기 정말 좋았습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10000) String content,

        @Schema(description = "첨부파일 ID 목록 (최대 5개). /api/v1/files로 사전 업로드 후 받은 ID.", example = "[101, 102]", nullable = true)
        @Size(max = 5) List<Long> fileIds
) {
}
