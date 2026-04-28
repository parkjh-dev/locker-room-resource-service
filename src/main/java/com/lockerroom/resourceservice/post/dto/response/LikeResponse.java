package com.lockerroom.resourceservice.post.dto.response;

public record LikeResponse(
        Long postId,
        boolean isLiked,
        int likeCount
) {
}
