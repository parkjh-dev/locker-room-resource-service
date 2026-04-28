package com.lockerroom.resourceservice.post.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.infrastructure.aop.Idempotent;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.post.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.post.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.post.dto.request.ReportRequest;
import com.lockerroom.resourceservice.post.dto.response.LikeResponse;
import com.lockerroom.resourceservice.post.dto.response.PostDetailResponse;
import com.lockerroom.resourceservice.post.dto.response.PostListResponse;
import com.lockerroom.resourceservice.post.dto.response.ReportResponse;
import com.lockerroom.resourceservice.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "게시글", description = "게시글 작성/조회/수정/삭제, 인기글, 좋아요, 신고")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @Operation(summary = "인기 게시글 조회", description = "지정 기간 내 좋아요/조회수 기준 인기 게시글을 조회합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getPopularPosts(
            @Parameter(description = "반환 개수 (1~50)") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @Parameter(description = "조회 기간(일). null=전체 기간") @RequestParam(required = false) @Min(1) @Max(365) Integer days) {
        return ResponseEntity.ok(ApiResponse.success("인기 게시글을 조회했습니다.", postService.getPopularPosts(size, days)));
    }

    @Operation(summary = "게시글 작성", description = "게시판에 새 게시글을 작성합니다. Idempotency-Key 헤더 권장.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BOARD_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BOARD_NOT_FOUND, USER_NOT_FOUND")
    })
    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 작성되었습니다.", postService.create(userId, request)));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 본문/첨부파일/좋아요 상태를 조회합니다. 익명 접근 가능 (좋아요 상태는 false 반환).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @SecurityRequirements
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getDetail(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @CurrentUserId(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("게시글을 조회했습니다.", postService.getDetail(postId, userId)));
    }

    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글의 제목/본문을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "POST_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @PathVariable Long postId,
            @CurrentUserId Long userId,
            @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", postService.update(postId, userId, request)));
    }

    @Operation(summary = "게시글 삭제", description = "본인이 작성한 게시글을 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "POST_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        postService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 좋아요 토글", description = "좋아요를 추가하거나 취소합니다. 응답으로 현재 상태와 누적 카운트를 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    })
    @Idempotent
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @PathVariable Long postId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("좋아요가 처리되었습니다.", postService.toggleLike(postId, userId)));
    }

    @Operation(summary = "게시글 신고", description = "부적절한 게시글을 신고 접수합니다. 동일 게시글에 중복 신고 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신고 접수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "POST_ALREADY_REPORTED")
    })
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
