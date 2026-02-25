package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.aop.Idempotent;
import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.create(userId, request)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getDetail(
            @PathVariable Long postId,
            @CurrentUserId(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(postService.getDetail(postId, userId)));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postService.update(postId, userId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        postService.delete(postId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Idempotent
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(postService.toggleLike(postId, userId)));
    }

    @Idempotent
    @PostMapping("/{postId}/report")
    public ResponseEntity<ApiResponse<ReportResponse>> report(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.report(postId, userId, request)));
    }
}
