package com.lockerroom.resourceservice.post.dto.request;

import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 작성 요청. 카테고리·투표 옵셔널.")
public record PostCreateRequest(
        @Schema(description = "작성 대상 게시판 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long boardId,

        @Schema(description = "게시글 제목 (최대 200자)", example = "두산 베어스 응원합니다", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "본문 (최대 10000자, Tiptap HTML 허용)", example = "<p>오늘 경기 정말 좋았습니다.</p>", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10000) String content,

        @Schema(description = "말머리. null이면 게시판 type별 default(QNA→QUESTION, 그 외→GENERAL)로 자동 매핑.", nullable = true)
        PostCategory category,

        @Schema(description = "투표 페이로드 (선택). 옵션 2~5개, 마감 미래 시점.", nullable = true)
        @Valid PollPayload poll,

        @Schema(description = "첨부파일 ID 목록 (최대 5개). /api/v1/files로 사전 업로드 후 받은 ID.", example = "[101, 102]", nullable = true)
        @Size(max = 5) List<Long> fileIds
) {
}
