package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse create(Long postId, Long userId, CommentCreateRequest request);

    CommentResponse createReply(Long postId, Long parentId, Long userId, CommentCreateRequest request);

    CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request);

    void delete(Long commentId, Long userId);

    List<CommentResponse> getByPost(Long postId);
}
