package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.kafka.event.QnaPostCreatedEvent;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.*;
import com.lockerroom.resourceservice.service.BoardService;
import com.lockerroom.resourceservice.service.FileService;
import com.lockerroom.resourceservice.service.PostService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReportRepository postReportRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final BoardService boardService;
    private final FileService fileService;
    private final KafkaProducerService kafkaProducerService;
    private final PostMapper postMapper;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public PostDetailResponse create(Long userId, PostCreateRequest request) {
        User user = findUserById(userId);
        Board board = boardService.validateBoardAccess(request.boardId(), userId);

        Post post = Post.builder()
                .board(board)
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        Post saved = postRepository.save(post);

        fileService.linkFilesToTarget(request.fileIds(), TargetType.POST, saved.getId(), userId);

        if (board.getType() == BoardType.QNA) {
            kafkaProducerService.send(
                    Constants.KAFKA_TOPIC_QNA_POST_CREATED,
                    String.valueOf(saved.getId()),
                    new QnaPostCreatedEvent(
                            saved.getId(),
                            user.getId(),
                            user.getNickname(),
                            saved.getTitle(),
                            saved.getContent(),
                            LocalDateTime.now()
                    )
            );
        }

        return toDetailResponse(saved, userId);
    }

    @Override
    @Transactional
    public PostDetailResponse getDetail(Long postId, Long userId) {
        Post post = findPostById(postId);
        post.incrementViewCount();
        return toDetailResponse(post, userId);
    }

    @Override
    @Transactional
    public PostDetailResponse update(Long postId, Long userId, PostUpdateRequest request) {
        Post post = findPostById(postId);
        validatePostOwner(post, userId);

        post.updateTitle(request.title());
        post.updateContent(request.content());

        syncFiles(postId, request.fileIds(), userId);

        return toDetailResponse(post, userId);
    }

    @Override
    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = findPostById(postId);
        validatePostOwner(post, userId);

        fileService.deleteFilesByTarget(TargetType.POST, postId);
        post.softDelete();
    }

    @Override
    @Transactional
    public LikeResponse toggleLike(Long postId, Long userId) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        var existing = postLikeRepository.findByPostIdAndUserId(postId, userId);

        boolean isLiked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            isLiked = false;
        } else {
            PostLike like = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(like);
            isLiked = true;
        }

        int likeCount = postLikeRepository.countByPostId(postId);
        post.updateLikeCount(likeCount);

        return new LikeResponse(postId, isLiked, likeCount);
    }

    @Override
    @Transactional
    public ReportResponse report(Long postId, Long userId, ReportRequest request) {
        Post post = findPostById(postId);
        User user = findUserById(userId);

        if (postReportRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new CustomException(ErrorCode.POST_ALREADY_REPORTED);
        }

        PostReport report1 = PostReport.builder()
                .post(post)
                .user(user)
                .reason(request.reason())
                .build();

        PostReport saved = postReportRepository.save(report1);

        return postMapper.toReportResponse(saved);
    }

    private PostDetailResponse toDetailResponse(Post post, Long userId) {
        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);

        List<FileEntity> files = fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(
                TargetType.POST, post.getId());
        List<FileResponse> fileResponses = fileMapper.toResponseList(files);

        return postMapper.toDetailResponse(post, isLiked, fileResponses);
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void syncFiles(Long postId, List<Long> newFileIds, Long userId) {
        List<FileEntity> existingFiles = fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(
                TargetType.POST, postId);
        List<Long> existingFileIds = existingFiles.stream().map(FileEntity::getId).toList();

        List<Long> safeNewFileIds = (newFileIds != null) ? newFileIds : List.of();

        List<FileEntity> toDelete = existingFiles.stream()
                .filter(f -> !safeNewFileIds.contains(f.getId()))
                .toList();
        for (FileEntity file : toDelete) {
            fileService.delete(file.getId(), userId);
        }

        List<Long> toLink = safeNewFileIds.stream()
                .filter(id -> !existingFileIds.contains(id))
                .toList();
        fileService.linkFilesToTarget(toLink, TargetType.POST, postId, userId);
    }

    private void validatePostOwner(Post post, Long userId) {
        if (post.getUser().getId().equals(userId)) return;

        User actor = findUserById(userId);
        if (actor.getRole() == Role.ADMIN) return;

        throw new CustomException(ErrorCode.POST_ACCESS_DENIED);
    }
}
