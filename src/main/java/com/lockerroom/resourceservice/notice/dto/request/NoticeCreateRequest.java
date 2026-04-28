package com.lockerroom.resourceservice.notice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "[관리자] 공지사항 작성/수정 요청.")
public record NoticeCreateRequest(
        @Schema(description = "공지 제목 (최대 200자)", example = "서비스 점검 안내", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "공지 본문 (최대 10000자)", example = "5월 1일 0시~2시 점검 예정입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 10000) String content,

        @Schema(description = "상단 고정 여부 (true=항상 최상단)", example = "true")
        boolean isPinned
) {
}
