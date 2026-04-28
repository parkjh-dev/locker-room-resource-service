package com.lockerroom.resourceservice.file.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.file.dto.response.FileResponse;
import com.lockerroom.resourceservice.file.model.enums.TargetType;
import com.lockerroom.resourceservice.file.service.FileService;
import com.lockerroom.resourceservice.infrastructure.aop.RateLimit;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "파일", description = "S3 기반 첨부파일 업로드/삭제. 게시글, 문의 등에 첨부됩니다.")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드", description = "multipart/form-data로 파일을 업로드하고 S3에 저장합니다. 사용자당 60초에 10건 제한.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE_SIZE_EXCEEDED, FILE_TYPE_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "COMMON_RATE_LIMIT_EXCEEDED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE_UPLOAD_FAILED")
    })
    @RateLimit(bucket = "file-upload", max = 10, windowSeconds = 60)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileResponse>> upload(
            @CurrentUserId Long userId,
            @Parameter(description = "업로드할 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "첨부 대상 타입 (POST, INQUIRY 등)") @RequestParam("targetType") TargetType targetType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("파일이 업로드되었습니다.", fileService.upload(userId, file, targetType)));
    }

    @Operation(summary = "파일 삭제", description = "본인이 업로드한 파일을 삭제합니다(S3 객체 + DB).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND")
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long fileId,
            @CurrentUserId Long userId) {
        fileService.delete(fileId, userId);
        return ResponseEntity.noContent().build();
    }
}
