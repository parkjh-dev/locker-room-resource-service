package com.lockerroom.resourceservice.comment.service;

import com.lockerroom.resourceservice.comment.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.comment.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.comment.dto.response.CommentResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

public interface CommentService {

    CommentResponse create(Long postId, Long userId, CommentCreateRequest request);

    CommentResponse createReply(Long postId, Long parentId, Long userId, CommentCreateRequest request);

    CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request);

    void delete(Long commentId, Long userId);

    CursorPageResponse<CommentResponse> getByPost(Long postId, CursorPageRequest pageRequest);
}
