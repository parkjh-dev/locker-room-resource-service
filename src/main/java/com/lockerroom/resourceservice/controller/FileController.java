package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.FileResponse;
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
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fileService.upload(userId, file)));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long fileId,
            @CurrentUserId Long userId) {
        fileService.delete(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
