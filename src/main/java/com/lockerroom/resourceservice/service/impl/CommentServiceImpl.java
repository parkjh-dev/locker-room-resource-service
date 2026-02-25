package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.kafka.event.NotificationEvent;
import com.lockerroom.resourceservice.mapper.CommentMapper;
import com.lockerroom.resourceservice.model.entity.Comment;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.entity.UserTeam;
import com.lockerroom.resourceservice.repository.CommentRepository;
import com.lockerroom.resourceservice.repository.PostRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.repository.UserTeamRepository;
import com.lockerroom.resourceservice.service.CommentService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;
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

        return commentMapper.toResponse(saved, resolveTeamName(userId));
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

        return commentMapper.toResponse(saved, resolveTeamName(userId));
    }

    @Override
    @Transactional
    public CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        Comment comment = findCommentById(commentId);
        validateCommentOwner(comment, userId);

        comment.updateContent(request.content());

        return commentMapper.toResponse(comment, resolveTeamName(userId));
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
    public CursorPageResponse<CommentResponse> getByPost(Long postId, CursorPageRequest pageRequest) {
        findPostById(postId);

        Long cursorId = pageRequest.decodeCursor();
        PageRequest pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Comment> rootComments = (cursorId != null)
                ? commentRepository.findByPostIdAndParentIsNullAndDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(
                        postId, cursorId, pageable)
                : commentRepository.findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByIdAsc(
                        postId, pageable);

        boolean hasNext = rootComments.size() > pageRequest.getSize();
        List<Comment> resultComments = hasNext
                ? rootComments.subList(0, pageRequest.getSize()) : rootComments;

        // Batch-load teamNames for N+1 prevention
        List<Long> allUserIds = resultComments.stream()
                .map(c -> c.getUser().getId())
                .collect(Collectors.toList());
        resultComments.forEach(c -> {
            List<Comment> replies = commentRepository
                    .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(c.getId());
            replies.forEach(r -> allUserIds.add(r.getUser().getId()));
        });
        Map<Long, String> teamNameMap = buildTeamNameMap(allUserIds);

        List<CommentResponse> items = resultComments.stream()
                .map(c -> toResponseWithReplies(c, teamNameMap))
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultComments.get(resultComments.size() - 1).getId())
                : null;

        return CursorPageResponse.<CommentResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private CommentResponse toResponseWithReplies(Comment comment, Map<Long, String> teamNameMap) {
        List<Comment> replies = commentRepository
                .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(comment.getId());

        List<CommentResponse> replyResponses = replies.stream()
                .map(r -> commentMapper.toResponse(r, teamNameMap.get(r.getUser().getId())))
                .toList();

        return commentMapper.toResponseWithReplies(comment, replyResponses, teamNameMap.get(comment.getUser().getId()));
    }

    private void updateCommentCount(Post post) {
        int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());
        post.updateCommentCount(count);
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private String resolveTeamName(Long userId) {
        return userTeamRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(ut -> ut.getTeam().getName())
                .orElse(null);
    }

    private Map<Long, String> buildTeamNameMap(List<Long> userIds) {
        return userTeamRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        ut -> ut.getUser().getId(),
                        ut -> ut.getTeam().getName(),
                        (first, second) -> first
                ));
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (comment.getUser().getId().equals(userId)) return;

        User actor = findUserById(userId);
        if (actor.getRole() == Role.ADMIN) return;

        throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
    }
}
