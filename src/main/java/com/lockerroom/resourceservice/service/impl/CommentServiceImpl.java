package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.kafka.event.NotificationEvent;
import com.lockerroom.resourceservice.mapper.CommentMapper;
import com.lockerroom.resourceservice.model.entity.Comment;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.repository.CommentRepository;
import com.lockerroom.resourceservice.repository.PostRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.CommentService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse create(Long postId, Long userId, CommentCreateRequest request) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.content())
                .build();

        Comment saved = commentRepository.save(comment);
        updateCommentCount(post);

        if (!user.getId().equals(post.getUser().getId())) {
            kafkaProducerService.send(
                    Constants.KAFKA_TOPIC_NOTIFICATION_COMMENT,
                    String.valueOf(post.getUser().getId()),
                    new NotificationEvent(
                            post.getUser().getId(),
                            user.getId(),
                            user.getNickname(),
                            NotificationType.COMMENT,
                            post.getId(),
                            user.getNickname() + "님이 댓글을 작성했습니다.",
                            LocalDateTime.now()
                    )
            );
        }

        return commentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse createReply(Long postId, Long parentId, Long userId, CommentCreateRequest request) {
        Comment parent = findCommentById(parentId);
        Post post = (postId != null) ? findPostById(postId) : parent.getPost();
        User user = findUserById(userId);

        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }

        Comment reply = Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(request.content())
                .build();

        Comment saved = commentRepository.save(reply);
        updateCommentCount(post);

        if (!user.getId().equals(parent.getUser().getId())) {
            kafkaProducerService.send(
                    Constants.KAFKA_TOPIC_NOTIFICATION_REPLY,
                    String.valueOf(parent.getUser().getId()),
                    new NotificationEvent(
                            parent.getUser().getId(),
                            user.getId(),
                            user.getNickname(),
                            NotificationType.REPLY,
                            post.getId(),
                            user.getNickname() + "님이 대댓글을 작성했습니다.",
                            LocalDateTime.now()
                    )
            );
        }

        return commentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        Comment comment = findCommentById(commentId);
        validateCommentOwner(comment, userId);

        comment.updateContent(request.content());

        return commentMapper.toResponse(comment);
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        validateCommentOwner(comment, userId);
        comment.softDelete();
        updateCommentCount(comment.getPost());
    }

    @Override
    public List<CommentResponse> getByPost(Long postId) {
        findPostById(postId);

        List<Comment> rootComments = commentRepository
                .findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(postId);

        return rootComments.stream()
                .map(this::toResponseWithReplies)
                .toList();
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        List<Comment> replies = commentRepository
                .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(comment.getId());

        List<CommentResponse> replyResponses = replies.stream()
                .map(commentMapper::toResponse)
                .toList();

        return commentMapper.toResponseWithReplies(comment, replyResponses);
    }

    private void updateCommentCount(Post post) {
        int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());
        post.updateCommentCount(count);
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
}
