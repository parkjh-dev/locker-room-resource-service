package com.lockerroom.resourceservice.post.service.impl;

import com.lockerroom.resourceservice.post.dto.response.LikeResponse;

import com.lockerroom.resourceservice.post.dto.response.ReportResponse;

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

import com.lockerroom.resourceservice.file.repository.FileRepository;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;

import com.lockerroom.resourceservice.post.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.post.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.post.dto.request.ReportRequest;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.infrastructure.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.file.mapper.FileMapper;
import com.lockerroom.resourceservice.post.mapper.PostMapper;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.file.model.enums.TargetType;
import com.lockerroom.resourceservice.board.service.BoardService;
import com.lockerroom.resourceservice.file.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostReportRepository postReportRepository;
    @Mock private com.lockerroom.resourceservice.post.repository.PollRepository pollRepository;
    @Mock private com.lockerroom.resourceservice.post.repository.PollOptionRepository pollOptionRepository;
    @Mock private com.lockerroom.resourceservice.post.repository.PollVoteRepository pollVoteRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileRepository fileRepository;
    @Mock private UserTeamRepository userTeamRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.FootballTeamRepository footballTeamRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository baseballTeamRepository;
    @Mock private BoardService boardService;
    @Mock private FileService fileService;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private PostMapper postMapper;
    @Mock private com.lockerroom.resourceservice.post.mapper.PollMapper pollMapper;
    @Mock private FileMapper fileMapper;

    @InjectMocks private PostServiceImpl postService;

    private User user;
    private User otherUser;
    private Board board;
    private Board qnaBoard;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .nickname("otheruser")
                .role(Role.USER)
                .build();

        board = Board.builder()
                .id(1L)
                .name("Free Board")
                .type(BoardType.COMMON)
                .build();

        qnaBoard = Board.builder()
                .id(2L)
                .name("QnA Board")
                .type(BoardType.QNA)
                .build();

        post = Post.builder()
                .id(1L)
                .board(board)
                .user(user)
                .title("Test Title")
                .content("Test Content")
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create post successfully for COMMON board")
        void create_success() {
            PostCreateRequest request = new PostCreateRequest(1L, "Test Title", "Test Content", null, null, null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    1L, 1L, "Free Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "Test Title", "Test Content",
                    com.lockerroom.resourceservice.post.model.enums.PostCategory.GENERAL, null,
                     0, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(boardService.validateBoardAccess(1L, 1L)).thenReturn(board);
            when(postRepository.save(any(Post.class))).thenReturn(post);
            when(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.create(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Test Title");
            verify(postRepository).save(any(Post.class));
            verify(kafkaProducerService, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should create post and send Kafka event for QNA board")
        void create_qnaBoard_sendsKafkaEvent() {
            Post qnaPost = Post.builder()
                    .id(2L)
                    .board(qnaBoard)
                    .user(user)
                    .title("QnA Title")
                    .content("QnA Content")
                    .build();

            PostCreateRequest request = new PostCreateRequest(2L, "QnA Title", "QnA Content", null, null, null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    2L, 2L, "QnA Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "QnA Title", "QnA Content",
                    com.lockerroom.resourceservice.post.model.enums.PostCategory.GENERAL, null,
                     0, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(boardService.validateBoardAccess(2L, 1L)).thenReturn(qnaBoard);
            when(postRepository.save(any(Post.class))).thenReturn(qnaPost);
            when(postLikeRepository.existsByPostIdAndUserId(2L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 2L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.create(1L, request);

            assertThat(result).isNotNull();
            verify(kafkaProducerService).send(eq("qna-post.created"), eq("2"), any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void create_userNotFound_throwsException() {
            PostCreateRequest request = new PostCreateRequest(1L, "Title", "Content", null, null, null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.create(999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("should return post detail and increment view count")
        void getDetail_success() {
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    1L, 1L, "Free Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "Test Title", "Test Content",
                    com.lockerroom.resourceservice.post.model.enums.PostCategory.GENERAL, null,
                     1, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.getDetail(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(post.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void getDetail_postNotFound_throwsException() {
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.getDetail(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when entity not found")
        void getDetail_deletedPost_throwsException() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.getDetail(1L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update post successfully")
        void update_success() {
            PostUpdateRequest request = new PostUpdateRequest("Updated Title", "Updated Content", null, null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    1L, 1L, "Free Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "Updated Title", "Updated Content",
                    com.lockerroom.resourceservice.post.model.enums.PostCategory.GENERAL, null,
                     0, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.update(1L, 1L, request);

            assertThat(result).isNotNull();
            assertThat(post.getTitle()).isEqualTo("Updated Title");
            assertThat(post.getContent()).isEqualTo("Updated Content");
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void update_notOwner_throwsException() {
            PostUpdateRequest request = new PostUpdateRequest("Updated", "Updated", null, null);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.update(1L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void update_postNotFound_throwsException() {
            PostUpdateRequest request = new PostUpdateRequest("Updated", "Updated", null, null);
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.update(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft delete post successfully")
        void delete_success() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            postService.delete(1L, 1L);

            assertThat(post.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void delete_notOwner_throwsException() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.delete(1L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void delete_postNotFound_throwsException() {
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.delete(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("toggleLike")
    class ToggleLike {

        @Test
        @DisplayName("should add like when not already liked")
        void toggleLike_addLike() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postLikeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
            when(postLikeRepository.save(any(PostLike.class))).thenReturn(PostLike.builder().build());
            when(postLikeRepository.countByPostId(1L)).thenReturn(1);

            LikeResponse result = postService.toggleLike(1L, 1L);

            assertThat(result.isLiked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(1);
            assertThat(result.postId()).isEqualTo(1L);
            verify(postLikeRepository).save(any(PostLike.class));
            verify(postLikeRepository, never()).delete(any(PostLike.class));
        }

        @Test
        @DisplayName("should remove like when already liked")
        void toggleLike_removeLike() {
            PostLike existingLike = PostLike.builder()
                    .id(1L)
                    .post(post)
                    .user(user)
                    .build();

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postLikeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingLike));
            when(postLikeRepository.countByPostId(1L)).thenReturn(0);

            LikeResponse result = postService.toggleLike(1L, 1L);

            assertThat(result.isLiked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0);
            verify(postLikeRepository).delete(existingLike);
            verify(postLikeRepository, never()).save(any(PostLike.class));
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void toggleLike_postNotFound_throwsException() {
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.toggleLike(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void toggleLike_userNotFound_throwsException() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.toggleLike(1L, 999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("report")
    class Report {

        @Test
        @DisplayName("should report post successfully")
        void report_success() {
            ReportRequest request = new ReportRequest("Spam content");
            PostReport savedReport = PostReport.builder()
                    .id(1L)
                    .post(post)
                    .user(user)
                    .reason("Spam content")
                    .build();
            ReportResponse expectedResponse = new ReportResponse(1L, 1L, ReportStatus.PENDING);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postReportRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(postReportRepository.save(any(PostReport.class))).thenReturn(savedReport);
            when(postMapper.toReportResponse(savedReport)).thenReturn(expectedResponse);

            ReportResponse result = postService.report(1L, 1L, request);

            assertThat(result).isNotNull();
            assertThat(result.reportId()).isEqualTo(1L);
            assertThat(result.postId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(ReportStatus.PENDING);
            verify(postReportRepository).save(any(PostReport.class));
        }

        @Test
        @DisplayName("should throw exception when duplicate report")
        void report_duplicate_throwsException() {
            ReportRequest request = new ReportRequest("Spam content");

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postReportRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(true);

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.report(1L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ALREADY_REPORTED);
            verify(postReportRepository, never()).save(any(PostReport.class));
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void report_postNotFound_throwsException() {
            ReportRequest request = new ReportRequest("Spam");
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.report(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void report_userNotFound_throwsException() {
            ReportRequest request = new ReportRequest("Spam");
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.report(1L, 999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    /* ────────── Phase 4: 게시글 투표 ────────── */

    @Nested
    @DisplayName("vote (게시글 투표 멱등성)")
    class Vote {

        private com.lockerroom.resourceservice.post.model.entity.Poll poll;
        private com.lockerroom.resourceservice.post.model.entity.PollOption option1;

        @BeforeEach
        void setupPoll() {
            poll = com.lockerroom.resourceservice.post.model.entity.Poll.builder()
                    .id(10L).post(post).question("Q?")
                    .expiresAt(java.time.LocalDateTime.now().plusDays(3))
                    .build();
            option1 = com.lockerroom.resourceservice.post.model.entity.PollOption.builder()
                    .id(100L).poll(poll).text("Option A").build();
        }

        @Test
        @DisplayName("투표 없는 게시글은 POLL_NOT_FOUND")
        void vote_pollNotFound() {
            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> postService.vote(1L, 1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
        }

        @Test
        @DisplayName("마감된 투표는 POLL_EXPIRED")
        void vote_expired_throws() {
            com.lockerroom.resourceservice.post.model.entity.Poll expiredPoll =
                    com.lockerroom.resourceservice.post.model.entity.Poll.builder()
                            .id(10L).post(post)
                            .expiresAt(java.time.LocalDateTime.now().minusDays(1))
                            .build();
            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(expiredPoll));

            CustomException ex = assertThrows(CustomException.class,
                    () -> postService.vote(1L, 1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_EXPIRED);
        }

        @Test
        @DisplayName("존재하지 않는 옵션 ID는 POLL_OPTION_INVALID")
        void vote_invalidOption_throws() {
            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(999L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(poll));
            when(pollOptionRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> postService.vote(1L, 1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_OPTION_INVALID);
        }

        @Test
        @DisplayName("다른 투표의 옵션 ID는 POLL_OPTION_INVALID")
        void vote_optionFromDifferentPoll_throws() {
            com.lockerroom.resourceservice.post.model.entity.Poll otherPoll =
                    com.lockerroom.resourceservice.post.model.entity.Poll.builder().id(99L).build();
            com.lockerroom.resourceservice.post.model.entity.PollOption otherOption =
                    com.lockerroom.resourceservice.post.model.entity.PollOption.builder()
                            .id(100L).poll(otherPoll).text("Wrong poll").build();

            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(poll));
            when(pollOptionRepository.findById(100L)).thenReturn(Optional.of(otherOption));

            CustomException ex = assertThrows(CustomException.class,
                    () -> postService.vote(1L, 1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_OPTION_INVALID);
        }

        @Test
        @DisplayName("이미 투표한 사용자가 다시 호출하면 멱등 — 신규 vote 저장 안 됨")
        void vote_alreadyVoted_idempotent() {
            com.lockerroom.resourceservice.post.model.entity.PollVote existingVote =
                    com.lockerroom.resourceservice.post.model.entity.PollVote.builder()
                            .id(1L).poll(poll).option(option1).user(user).build();

            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(poll));
            when(pollOptionRepository.findById(100L)).thenReturn(Optional.of(option1));
            when(pollVoteRepository.findByPollIdAndUserId(10L, 1L)).thenReturn(Optional.of(existingVote));
            when(pollOptionRepository.findByPollIdOrderByIdAsc(10L)).thenReturn(List.of(option1));
            when(pollMapper.toResponse(eq(poll), anyList(), eq(100L))).thenReturn(
                    new com.lockerroom.resourceservice.post.dto.response.PollResponse(
                            "Q?", List.of(), poll.getExpiresAt(), 0, 100L));

            postService.vote(1L, 1L, req);

            verify(pollVoteRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("최초 투표는 PollVote 저장 + voteCount 증가")
        void vote_firstTime_savesAndIncrements() {
            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(poll));
            when(pollOptionRepository.findById(100L)).thenReturn(Optional.of(option1));
            when(pollVoteRepository.findByPollIdAndUserId(10L, 1L))
                    .thenReturn(Optional.empty())  // 최초 호출 시 없음
                    .thenReturn(Optional.of(com.lockerroom.resourceservice.post.model.entity.PollVote.builder()
                            .id(1L).poll(poll).option(option1).user(user).build())); // 두 번째 호출(빌드 응답용)
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(pollOptionRepository.findByPollIdOrderByIdAsc(10L)).thenReturn(List.of(option1));
            when(pollMapper.toResponse(eq(poll), anyList(), any())).thenReturn(
                    new com.lockerroom.resourceservice.post.dto.response.PollResponse(
                            "Q?", List.of(), poll.getExpiresAt(), 1, 100L));

            int beforeOption = option1.getVoteCount();
            int beforeTotal = poll.getTotalVotes();
            postService.vote(1L, 1L, req);

            verify(pollVoteRepository).saveAndFlush(any(com.lockerroom.resourceservice.post.model.entity.PollVote.class));
            assertThat(option1.getVoteCount()).isEqualTo(beforeOption + 1);
            assertThat(poll.getTotalVotes()).isEqualTo(beforeTotal + 1);
        }

        @Test
        @DisplayName("동시성 race — saveAndFlush가 UNIQUE 위반하면 멱등 처리")
        void vote_uniqueConstraintRace_idempotent() {
            com.lockerroom.resourceservice.post.dto.request.PollVoteRequest req =
                    new com.lockerroom.resourceservice.post.dto.request.PollVoteRequest(100L);

            when(pollRepository.findByPostId(1L)).thenReturn(Optional.of(poll));
            when(pollOptionRepository.findById(100L)).thenReturn(Optional.of(option1));
            when(pollVoteRepository.findByPollIdAndUserId(10L, 1L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(com.lockerroom.resourceservice.post.model.entity.PollVote.builder()
                            .id(1L).poll(poll).option(option1).user(user).build()));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(pollVoteRepository.saveAndFlush(any()))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("UNIQUE violated"));
            when(pollOptionRepository.findByPollIdOrderByIdAsc(10L)).thenReturn(List.of(option1));
            when(pollMapper.toResponse(eq(poll), anyList(), any())).thenReturn(
                    new com.lockerroom.resourceservice.post.dto.response.PollResponse(
                            "Q?", List.of(), poll.getExpiresAt(), 1, 100L));

            // 예외가 service 밖으로 새지 않고 정상 응답되어야 함
            postService.vote(1L, 1L, req);
            verify(pollVoteRepository).saveAndFlush(any());
        }
    }
}
