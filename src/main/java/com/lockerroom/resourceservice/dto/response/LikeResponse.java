package com.lockerroom.resourceservice.dto.response;

public record LikeResponse(
        Long postId,
        boolean isLiked,
        int likeCount
) {
}
