package com.lockerroom.resourceservice.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "첨부파일 응답. 업로드 직후 + 게시글/문의 상세에 임베드.")
public record FileResponse(
        @Schema(description = "파일 ID (게시글 작성 시 fileIds 필드로 전달)", example = "101")
        Long id,

        @Schema(description = "원본 파일명 (사용자 표시용)", example = "stadium.jpg")
        String originalName,

        @Schema(description = "S3 다운로드/표시 URL", example = "https://cdn.example.com/files/2026/04/abc123.jpg")
        String url,

        @Schema(description = "파일 크기 (바이트)", example = "204800")
        long size,

        @Schema(description = "MIME 타입", example = "image/jpeg")
        String mimeType
) {
}
