package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.dto.response.*;

public interface UserService {

    UserResponse getMyInfo(Long userId);

    UserResponse updateMyInfo(Long userId, UserUpdateRequest request);

    void withdraw(Long userId, WithdrawRequest request);

    CursorPageResponse<UserPostListResponse> getMyPosts(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserCommentListResponse> getMyComments(Long userId, CursorPageRequest pageRequest);

    CursorPageResponse<UserLikeListResponse> getMyLikes(Long userId, CursorPageRequest pageRequest);
}
