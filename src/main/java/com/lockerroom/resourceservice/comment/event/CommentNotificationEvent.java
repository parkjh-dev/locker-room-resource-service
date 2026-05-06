package com.lockerroom.resourceservice.comment.event;

public record CommentNotificationEvent(
        String eventId,
        Long userId,
        Long postId,
        Long commentId,
        String actorNickname
) {
}
