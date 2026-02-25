package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getByPost(
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getByPost(postId)));
    }

    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentService.create(postId, userId, request)));
    }

    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long commentId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(commentService.update(commentId, userId, request)));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long commentId,
            @CurrentUserId Long userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/api/v1/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> createReply(
            @PathVariable Long commentId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentService.createReply(null, commentId, userId, request)));
    }
}
