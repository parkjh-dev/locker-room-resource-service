package com.lockerroom.resourceservice.comment.service.impl;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.comment.repository.CommentRepository;

import com.lockerroom.resourceservice.comment.model.entity.Comment;

import com.lockerroom.resourceservice.user.repository.UserTeamRepository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.UserTeam;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;

import com.lockerroom.resourceservice.comment.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.comment.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.comment.dto.response.CommentResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.infrastructure.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.comment.event.CommentNotificationEvent;
import com.lockerroom.resourceservice.comment.event.ReplyNotificationEvent;
import com.lockerroom.resourceservice.comment.mapper.CommentMapper;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.comment.service.CommentService;
import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.ArrayList;
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
    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;
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
                    new CommentNotificationEvent(
                            UUID.randomUUID().toString(),
                            post.getUser().getId(),
                            post.getId(),
                            saved.getId(),
                            user.getNickname()
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
                    new ReplyNotificationEvent(
                            UUID.randomUUID().toString(),
                            parent.getUser().getId(),
                            post.getId(),
                            parent.getId(),
                            saved.getId(),
                            user.getNickname()
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

        // Batch-load replies grouped by parent id (single query)
        List<Long> rootIds = resultComments.stream().map(Comment::getId).toList();
        List<Comment> allReplies = rootIds.isEmpty()
                ? List.of()
                : commentRepository.findByParentIdInAndDeletedAtIsNullOrderByParentIdAscCreatedAtAsc(rootIds);
        Map<Long, List<Comment>> repliesByParent = allReplies.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Batch-load teamNames for N+1 prevention (root + reply authors)
        List<Long> allUserIds = new ArrayList<>();
        resultComments.forEach(c -> allUserIds.add(c.getUser().getId()));
        allReplies.forEach(r -> allUserIds.add(r.getUser().getId()));
        Map<Long, String> teamNameMap = buildTeamNameMap(allUserIds);

        List<CommentResponse> items = resultComments.stream()
                .map(c -> toResponseWithReplies(c, repliesByParent.getOrDefault(c.getId(), List.of()), teamNameMap))
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

    private CommentResponse toResponseWithReplies(Comment comment, List<Comment> replies, Map<Long, String> teamNameMap) {
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
                .map(this::resolveTeamNameFromUserTeam)
                .orElse(null);
    }

    private Map<Long, String> buildTeamNameMap(List<Long> userIds) {
        List<UserTeam> userTeams = userTeamRepository.findByUserIdIn(userIds);

        List<Long> footballTeamIds = new java.util.ArrayList<>();
        List<Long> baseballTeamIds = new java.util.ArrayList<>();

        for (UserTeam ut : userTeams) {
            String sportEn = ut.getSport().getNameEn();
            if ("Football".equalsIgnoreCase(sportEn)) {
                footballTeamIds.add(ut.getTeamId());
            } else if ("Baseball".equalsIgnoreCase(sportEn)) {
                baseballTeamIds.add(ut.getTeamId());
            }
        }

        java.util.Map<Long, String> teamNameById = new java.util.HashMap<>();
        if (!footballTeamIds.isEmpty()) {
            footballTeamRepository.findAllById(footballTeamIds)
                    .forEach(ft -> teamNameById.put(ft.getId(), ft.getNameKo()));
        }
        if (!baseballTeamIds.isEmpty()) {
            baseballTeamRepository.findAllById(baseballTeamIds)
                    .forEach(bt -> teamNameById.put(bt.getId(), bt.getNameKo()));
        }

        return userTeams.stream()
                .collect(Collectors.toMap(
                        ut -> ut.getUser().getId(),
                        ut -> teamNameById.getOrDefault(ut.getTeamId(), ""),
                        (first, second) -> first
                ));
    }

    private String resolveTeamNameFromUserTeam(UserTeam ut) {
        String sportNameEn = ut.getSport().getNameEn();
        if ("Football".equalsIgnoreCase(sportNameEn)) {
            return footballTeamRepository.findById(ut.getTeamId())
                    .map(FootballTeam::getNameKo)
                    .orElse(null);
        } else if ("Baseball".equalsIgnoreCase(sportNameEn)) {
            return baseballTeamRepository.findById(ut.getTeamId())
                    .map(BaseballTeam::getNameKo)
                    .orElse(null);
        }
        return null;
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (comment.getUser().getId().equals(userId)) return;

        User actor = findUserById(userId);
        if (actor.getRole() == Role.ADMIN) return;

        throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
    }
}
