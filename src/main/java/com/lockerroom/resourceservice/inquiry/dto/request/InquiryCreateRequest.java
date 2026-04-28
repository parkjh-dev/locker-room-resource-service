package com.lockerroom.resourceservice.inquiry.dto.request;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "1:1 문의 등록 요청. 첨부파일은 사전 업로드 후 ID 전달.")
public record InquiryCreateRequest(
        @Schema(description = "문의 유형 (BUG, FEATURE, ACCOUNT 등)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull InquiryType type,

        @Schema(description = "문의 제목 (최대 200자)", example = "로그인이 안 됩니다", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "문의 내용 (최대 5000자)", example = "구글 OAuth 로그인 시 오류가 발생합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 5000) String content,

        @Schema(description = "첨부파일 ID 목록 (최대 5개). 사전 업로드 후 받은 ID.", example = "[201]", nullable = true)
        @Size(max = 5) List<Long> fileIds
) {
}
