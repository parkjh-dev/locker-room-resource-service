package com.lockerroom.resourceservice.user.controller;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.comment.dto.response.UserCommentListResponse;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;
import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;
import com.lockerroom.resourceservice.user.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.user.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.user.dto.response.UserResponse;
import com.lockerroom.resourceservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "내 정보 조회/수정, 활동 내역(글/댓글/좋아요), 회원 탈퇴")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(userId)));
    }

    @Operation(summary = "내 정보 수정", description = "닉네임, 프로필 이미지, 응원 팀 등을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "USER_NICKNAME_DUPLICATED")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @CurrentUserId Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("회원 정보가 수정되었습니다.", userService.updateMyInfo(userId, request)));
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 비활성화하고 탈퇴 사유를 기록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "탈퇴 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @CurrentUserId Long userId,
            @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내가 작성한 글 목록", description = "커서 페이지네이션. 본인이 작성한 게시글만 반환.")
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserPostListResponse>>> getMyPosts(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyPosts(userId, pageRequest)));
    }

    @Operation(summary = "내가 작성한 댓글 목록", description = "커서 페이지네이션. 본인이 작성한 댓글만 반환.")
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserCommentListResponse>>> getMyComments(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyComments(userId, pageRequest)));
    }

    @Operation(summary = "내가 좋아요 한 글 목록", description = "커서 페이지네이션. 본인이 좋아요 누른 게시글만 반환.")
    @GetMapping("/me/likes")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserLikeListResponse>>> getMyLikes(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyLikes(userId, pageRequest)));
    }
}
