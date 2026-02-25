package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.aop.Idempotent;
import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.PostService;
import java.util.List;
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

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getPopularPosts(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success("인기 게시글을 조회했습니다.", postService.getPopularPosts(size, days)));
    }

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 작성되었습니다.", postService.create(userId, request)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getDetail(
            @PathVariable Long postId,
            @CurrentUserId(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("게시글을 조회했습니다.", postService.getDetail(postId, userId)));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", postService.update(postId, userId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        postService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Idempotent
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("좋아요가 처리되었습니다.", postService.toggleLike(postId, userId)));
    }

    @Idempotent
    @PostMapping("/{postId}/report")
    public ResponseEntity<ApiResponse<ReportResponse>> report(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다.", postService.report(postId, userId, request)));
    }
}
