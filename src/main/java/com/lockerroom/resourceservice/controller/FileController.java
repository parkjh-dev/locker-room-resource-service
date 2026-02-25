package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ResponseEntity<ApiResponse<FileResponse>> upload(
            @CurrentUserId Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetType") TargetType targetType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("파일이 업로드되었습니다.", fileService.upload(userId, file, targetType)));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long fileId,
            @CurrentUserId Long userId) {
        fileService.delete(fileId, userId);
        return ResponseEntity.noContent().build();
    }
}
