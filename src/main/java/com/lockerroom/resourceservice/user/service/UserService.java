package com.lockerroom.resourceservice.user.service;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.comment.dto.response.UserCommentListResponse;

import com.lockerroom.resourceservice.user.dto.response.UserResponse;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.user.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.user.dto.request.WithdrawRequest;

public interface UserService {

    UserResponse getMyInfo(Long userId);

    UserResponse updateMyInfo(Long userId, UserUpdateRequest request);

    void withdraw(Long userId, WithdrawRequest request);

    CursorPageResponse<UserPostListResponse> getMyPosts(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserCommentListResponse> getMyComments(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserLikeListResponse> getMyLikes(Long userId, CursorPageRequest pageRequest);
}
