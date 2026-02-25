package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.*;
import com.lockerroom.resourceservice.service.BoardService;
import com.lockerroom.resourceservice.service.FileService;
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
    @Mock private UserRepository userRepository;
    @Mock private FileRepository fileRepository;
    @Mock private UserTeamRepository userTeamRepository;
    @Mock private BoardService boardService;
    @Mock private FileService fileService;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private PostMapper postMapper;
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
            PostCreateRequest request = new PostCreateRequest(1L, "Test Title", "Test Content", null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    1L, 1L, "Free Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "Test Title", "Test Content",
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
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any())).thenReturn(expectedResponse);

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

            PostCreateRequest request = new PostCreateRequest(2L, "QnA Title", "QnA Content", null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    2L, 2L, "QnA Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "QnA Title", "QnA Content",
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
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.create(1L, request);

            assertThat(result).isNotNull();
            verify(kafkaProducerService).send(eq("qna-post.created"), eq("2"), any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void create_userNotFound_throwsException() {
            PostCreateRequest request = new PostCreateRequest(1L, "Title", "Content", null);
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
                    1, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any())).thenReturn(expectedResponse);

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
            PostUpdateRequest request = new PostUpdateRequest("Updated Title", "Updated Content", null);
            PostDetailResponse expectedResponse = new PostDetailResponse(
                    1L, 1L, "Free Board",
                    new AuthorInfo(1L, "testuser", null, null),
                    "Updated Title", "Updated Content",
                    0, 0, 0, false, false,
                    Collections.emptyList(), null, null
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).thenReturn(false);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(userTeamRepository.findFirstByUserIdOrderByIdAsc(anyLong())).thenReturn(Optional.empty());
            when(postMapper.toDetailResponse(any(Post.class), eq(false), anyList(), any())).thenReturn(expectedResponse);

            PostDetailResponse result = postService.update(1L, 1L, request);

            assertThat(result).isNotNull();
            assertThat(post.getTitle()).isEqualTo("Updated Title");
            assertThat(post.getContent()).isEqualTo("Updated Content");
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void update_notOwner_throwsException() {
            PostUpdateRequest request = new PostUpdateRequest("Updated", "Updated", null);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

            CustomException exception = assertThrows(CustomException.class,
                    () -> postService.update(1L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void update_postNotFound_throwsException() {
            PostUpdateRequest request = new PostUpdateRequest("Updated", "Updated", null);
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
}
