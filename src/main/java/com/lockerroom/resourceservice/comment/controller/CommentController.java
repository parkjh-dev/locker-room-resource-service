package com.lockerroom.resourceservice.comment.controller;

import com.lockerroom.resourceservice.comment.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.comment.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.comment.dto.response.CommentResponse;
import com.lockerroom.resourceservice.comment.service.CommentService;
import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.aop.Idempotent;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글", description = "게시글 댓글 작성/조회/수정/삭제, 답글(reply) 작성")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "게시글 댓글 목록", description = "게시글에 달린 댓글을 커서 페이지네이션으로 반환합니다. 익명 접근 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @SecurityRequirements
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CursorPageResponse<CommentResponse>>> getByPost(
            @PathVariable Long postId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getByPost(postId, pageRequest)));
    }

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @Idempotent
    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 작성되었습니다.", commentService.create(postId, userId, request)));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글의 내용을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMENT_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    })
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long commentId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", commentService.update(commentId, userId, request)));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMENT_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    })
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @CurrentUserId Long userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "답글 작성", description = "댓글에 답글(reply)을 작성합니다. 답글의 답글은 허용하지 않습니다(depth=1 한정).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT_REPLY_DEPTH_EXCEEDED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    })
    @Idempotent
    @PostMapping("/api/v1/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> createReply(
            @PathVariable Long commentId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("답글이 작성되었습니다.", commentService.createReply(null, commentId, userId, request)));
    }
}
