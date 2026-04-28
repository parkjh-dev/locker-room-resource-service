package com.lockerroom.resourceservice.post.service;

import com.lockerroom.resourceservice.post.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.post.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.post.dto.request.ReportRequest;
import com.lockerroom.resourceservice.post.dto.response.LikeResponse;
import com.lockerroom.resourceservice.post.dto.response.PostDetailResponse;
import com.lockerroom.resourceservice.post.dto.response.PostListResponse;
import com.lockerroom.resourceservice.post.dto.response.ReportResponse;

import java.util.List;

public interface PostService {

    List<PostListResponse> getPopularPosts(int size, Integer days);

    PostDetailResponse create(Long userId, PostCreateRequest request);

    PostDetailResponse getDetail(Long postId, Long userId);

    PostDetailResponse update(Long postId, Long userId, PostUpdateRequest request);

    void delete(Long postId, Long userId);

    LikeResponse toggleLike(Long postId, Long userId);

    ReportResponse report(Long postId, Long userId, ReportRequest request);
}
