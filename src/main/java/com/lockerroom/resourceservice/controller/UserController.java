package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @CurrentUserId Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateMyInfo(userId, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @CurrentUserId Long userId,
            @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserPostListResponse>>> getMyPosts(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyPosts(userId, pageRequest)));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserCommentListResponse>>> getMyComments(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyComments(userId, pageRequest)));
    }

    @GetMapping("/me/likes")
    public ResponseEntity<ApiResponse<CursorPageResponse<UserLikeListResponse>>> getMyLikes(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyLikes(userId, pageRequest)));
    }
}
