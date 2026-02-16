package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.LikeResponse;
import com.lockerroom.resourceservice.dto.response.PostDetailResponse;
import com.lockerroom.resourceservice.dto.response.ReportResponse;

public interface PostService {

    PostDetailResponse create(Long userId, PostCreateRequest request);

    PostDetailResponse getDetail(Long postId, Long userId);

    PostDetailResponse update(Long postId, Long userId, PostUpdateRequest request);

    void delete(Long postId, Long userId);

    LikeResponse toggleLike(Long postId, Long userId);

    ReportResponse report(Long postId, Long userId, ReportRequest request);
}
