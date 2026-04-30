package com.lockerroom.resourceservice.post.service.impl;

import com.lockerroom.resourceservice.post.dto.response.LikeResponse;

import com.lockerroom.resourceservice.post.dto.response.ReportResponse;

import com.lockerroom.resourceservice.post.dto.response.PostListResponse;

import com.lockerroom.resourceservice.post.dto.response.PostDetailResponse;

import com.lockerroom.resourceservice.post.repository.PostReportRepository;

import com.lockerroom.resourceservice.post.repository.PostLikeRepository;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.entity.PostReport;

import com.lockerroom.resourceservice.post.model.entity.PostLike;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.user.repository.UserTeamRepository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.board.model.entity.Board;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;

import com.lockerroom.resourceservice.file.dto.response.FileResponse;

import com.lockerroom.resourceservice.file.repository.FileRepository;

import com.lockerroom.resourceservice.file.model.entity.FileEntity;

import com.lockerroom.resourceservice.post.dto.request.PollPayload;
import com.lockerroom.resourceservice.post.dto.request.PollVoteRequest;
import com.lockerroom.resourceservice.post.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.post.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.post.dto.request.ReportRequest;
import com.lockerroom.resourceservice.post.dto.response.PollResponse;
import com.lockerroom.resourceservice.post.model.entity.Poll;
import com.lockerroom.resourceservice.post.model.entity.PollOption;
import com.lockerroom.resourceservice.post.model.entity.PollVote;
import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import com.lockerroom.resourceservice.post.repository.PollOptionRepository;
import com.lockerroom.resourceservice.post.repository.PollRepository;
import com.lockerroom.resourceservice.post.repository.PollVoteRepository;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.infrastructure.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.post.event.QnaPostCreatedEvent;
import com.lockerroom.resourceservice.file.mapper.FileMapper;
import com.lockerroom.resourceservice.post.mapper.PollMapper;
import com.lockerroom.resourceservice.post.mapper.PostMapper;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import org.springframework.dao.DataIntegrityViolationException;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.file.model.enums.TargetType;
import com.lockerroom.resourceservice.board.service.BoardService;
import com.lockerroom.resourceservice.file.service.FileService;
import com.lockerroom.resourceservice.post.service.PostService;
import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;
    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;
    private final FileRepository fileRepository;
    private final BoardService boardService;
    private final FileService fileService;
    private final KafkaProducerService kafkaProducerService;
    private final PostMapper postMapper;
    private final PollMapper pollMapper;
    private final FileMapper fileMapper;

    @Override
    public List<PostListResponse> getPopularPosts(int size, Integer days) {
        LocalDateTime since = (days != null) ? LocalDateTime.now().minusDays(days) : null;
        List<Post> posts = postRepository.findPopularPosts(since, PageRequest.of(0, size));
        return posts.stream()
                .map(p -> postMapper.toListResponse(p, pollRepository.existsByPostId(p.getId())))
                .toList();
    }

    @Override
    @Transactional
    public PostDetailResponse create(Long userId, PostCreateRequest request) {
        User user = findUserById(userId);
        Board board = boardService.validateBoardAccess(request.boardId(), userId);

        // 카테고리 — 미지정 시 게시판 type별 default (QNA→QUESTION, 그 외→GENERAL)
        PostCategory category = (request.category() != null)
                ? request.category()
                : PostCategory.defaultFor(board.getType());

        Post post = Post.builder()
                .board(board)
                .user(user)
                .title(request.title())
                .content(request.content())
                .category(category)
                .build();

        Post saved = postRepository.save(post);

        fileService.linkFilesToTarget(request.fileIds(), TargetType.POST, saved.getId(), userId);

        // 투표 — payload 있으면 함께 생성
        if (request.poll() != null) {
            createPollFor(saved, request.poll());
        }

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

    private void createPollFor(Post post, PollPayload payload) {
        Poll poll = Poll.builder()
                .post(post)
                .question(payload.question())
                .expiresAt(payload.expiresAt())
                .build();
        Poll savedPoll = pollRepository.save(poll);

        for (String text : payload.options()) {
            PollOption opt = PollOption.builder()
                    .poll(savedPoll)
                    .text(text.trim())
                    .build();
            pollOptionRepository.save(opt);
        }
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
        if (request.category() != null) {
            post.updateCategory(request.category());
        }
        // 투표는 수정 불가 (참여자 표 보호)

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

        String teamName = resolveTeamName(post.getUser().getId());

        PollResponse pollResponse = buildPollResponse(post.getId(), userId);

        return postMapper.toDetailResponse(post, isLiked, fileResponses, teamName, pollResponse);
    }

    /** 투표 정보 매핑 — Poll이 없으면 null. 익명 사용자(userId=null)는 myVoteOptionId 항상 null. */
    private PollResponse buildPollResponse(Long postId, Long userId) {
        return pollRepository.findByPostId(postId)
                .map(poll -> {
                    List<PollOption> options = pollOptionRepository.findByPollIdOrderByIdAsc(poll.getId());
                    Long myVoteOptionId = (userId != null)
                            ? pollVoteRepository.findByPollIdAndUserId(poll.getId(), userId)
                                    .map(v -> v.getOption().getId())
                                    .orElse(null)
                            : null;
                    return pollMapper.toResponse(poll, options, myVoteOptionId);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public PollResponse vote(Long postId, Long userId, PollVoteRequest request) {
        Poll poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POLL_NOT_FOUND));

        if (LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            throw new CustomException(ErrorCode.POLL_EXPIRED);
        }

        PollOption option = pollOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.POLL_OPTION_INVALID));

        // 옵션이 해당 투표 소속인지 검증
        if (!option.getPoll().getId().equals(poll.getId())) {
            throw new CustomException(ErrorCode.POLL_OPTION_INVALID);
        }

        // 멱등성 — 이미 투표한 경우 현재 상태만 반환
        if (pollVoteRepository.findByPollIdAndUserId(poll.getId(), userId).isPresent()) {
            return buildPollResponseFor(poll, userId);
        }

        User user = findUserById(userId);

        try {
            PollVote pollVote = PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .user(user)
                    .build();
            pollVoteRepository.saveAndFlush(pollVote); // UNIQUE 위반 즉시 catch

            option.incrementVoteCount();
            poll.incrementTotalVotes();
        } catch (DataIntegrityViolationException race) {
            // 동시성 race condition — 다른 트랜잭션이 먼저 투표한 상태이므로 멱등 처리
        }

        return buildPollResponseFor(poll, userId);
    }

    private PollResponse buildPollResponseFor(Poll poll, Long userId) {
        List<PollOption> options = pollOptionRepository.findByPollIdOrderByIdAsc(poll.getId());
        Long myVoteOptionId = pollVoteRepository.findByPollIdAndUserId(poll.getId(), userId)
                .map(v -> v.getOption().getId())
                .orElse(null);
        return pollMapper.toResponse(poll, options, myVoteOptionId);
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

    private String resolveTeamName(Long userId) {
        return userTeamRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(ut -> {
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
                    return (String) null;
                })
                .orElse(null);
    }
}
