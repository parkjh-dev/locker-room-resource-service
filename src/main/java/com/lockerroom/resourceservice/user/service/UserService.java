package com.lockerroom.resourceservice.user.service;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.comment.dto.response.UserCommentListResponse;

import com.lockerroom.resourceservice.user.dto.response.UserResponse;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest;
import com.lockerroom.resourceservice.user.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.user.dto.request.WithdrawRequest;

public interface UserService {

    UserResponse getMyInfo(Long userId);

    UserResponse updateMyInfo(Long userId, UserUpdateRequest request);

    void withdraw(Long userId, WithdrawRequest request);

    CursorPageResponse<UserPostListResponse> getMyPosts(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserCommentListResponse> getMyComments(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserLikeListResponse> getMyLikes(Long userId, CursorPageRequest pageRequest);

    /**
     * 응원팀 등록 — 종목별 락. 이미 등록된 종목이 포함되면 409.
     * 호출 성공 시 onboardingCompletedAt이 idempotent하게 셋.
     */
    UserResponse addUserTeams(Long userId, AddUserTeamsRequest request);

    /** 온보딩 건너뛰기 — onboardingCompletedAt만 셋, 응원팀은 빈 상태 유지. */
    UserResponse skipOnboarding(Long userId);
}
