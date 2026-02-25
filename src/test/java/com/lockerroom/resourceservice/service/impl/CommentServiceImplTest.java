package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.AuthorInfo;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.mapper.CommentMapper;
import com.lockerroom.resourceservice.model.entity.Board;
import com.lockerroom.resourceservice.model.entity.Comment;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.CommentRepository;
import com.lockerroom.resourceservice.repository.PostRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.repository.UserTeamRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserTeamRepository userTeamRepository;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private CommentMapper commentMapper;

    @InjectMocks private CommentServiceImpl commentService;

    private User postAuthor;
    private User commenter;
    private Board board;
    private Post post;
    private Comment comment;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        postAuthor = User.builder()
                .id(1L)
                .email("author@test.com")
                .nickname("author")
                .role(Role.USER)
                .build();

        commenter = User.builder()
                .id(2L)
                .email("commenter@test.com")
                .nickname("commenter")
                .role(Role.USER)
                .build();

        board = Board.builder()
                .id(1L)
                .name("Free Board")
                .type(BoardType.COMMON)
                .build();

        post = Post.builder()
                .id(1L)
                .board(board)
                .user(postAuthor)
                .title("Test Post")
                .content("Test Content")
                .build();

        comment = Comment.builder()
                .id(1L)
                .post(post)
                .user(commenter)
                .content("Test Comment")
                .build();

        parentComment = Comment.builder()
                .id(10L)
                .post(post)
                .user(postAuthor)
                .content("Parent Comment")
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create comment successfully")
        void create_success() {
            CommentCreateRequest request = new CommentCreateRequest("New comment");
            CommentResponse expectedResponse = new CommentResponse(
                    1L, new AuthorInfo(2L, "commenter", null, null),
                    "New comment", false, null, List.of()
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(1);
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            CommentResponse result = commentService.create(1L, 2L, request);

            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("New comment");
            verify(commentRepository).save(any(Comment.class));
            verify(kafkaProducerService).send(eq("notification.comment"), eq("1"), any());
        }

        @Test
        @DisplayName("should not send notification when commenter is post author")
        void create_sameUser_noNotification() {
            CommentCreateRequest request = new CommentCreateRequest("Self comment");
            Comment selfComment = Comment.builder()
                    .id(2L)
                    .post(post)
                    .user(postAuthor)
                    .content("Self comment")
                    .build();
            CommentResponse expectedResponse = new CommentResponse(
                    2L, new AuthorInfo(1L, "author", null, null),
                    "Self comment", false, null, List.of()
            );

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(postAuthor));
            when(commentRepository.save(any(Comment.class))).thenReturn(selfComment);
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(1);
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            commentService.create(1L, 1L, request);

            verify(kafkaProducerService, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void create_postNotFound_throwsException() {
            CommentCreateRequest request = new CommentCreateRequest("Comment");
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.create(999L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void create_userNotFound_throwsException() {
            CommentCreateRequest request = new CommentCreateRequest("Comment");
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.create(1L, 999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createReply")
    class CreateReply {

        @Test
        @DisplayName("should create reply successfully")
        void createReply_success() {
            CommentCreateRequest request = new CommentCreateRequest("Reply content");
            Comment reply = Comment.builder()
                    .id(2L)
                    .post(post)
                    .user(commenter)
                    .parent(parentComment)
                    .content("Reply content")
                    .build();
            CommentResponse expectedResponse = new CommentResponse(
                    2L, new AuthorInfo(2L, "commenter", null, null),
                    "Reply content", false, null, List.of()
            );

            when(commentRepository.findById(10L)).thenReturn(Optional.of(parentComment));
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
            when(commentRepository.save(any(Comment.class))).thenReturn(reply);
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(2);
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            CommentResponse result = commentService.createReply(1L, 10L, 2L, request);

            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Reply content");
            verify(commentRepository).save(any(Comment.class));
            verify(kafkaProducerService).send(eq("notification.reply"), eq("1"), any());
        }

        @Test
        @DisplayName("should throw exception when depth exceeds limit (replying to reply)")
        void createReply_depthExceeded_throwsException() {
            Comment replyComment = Comment.builder()
                    .id(20L)
                    .post(post)
                    .user(commenter)
                    .parent(parentComment)
                    .content("This is already a reply")
                    .build();

            CommentCreateRequest request = new CommentCreateRequest("Nested reply");

            when(commentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.createReply(1L, 20L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }

        @Test
        @DisplayName("should not send notification when replier is parent comment author")
        void createReply_sameUser_noNotification() {
            CommentCreateRequest request = new CommentCreateRequest("Self reply");
            Comment reply = Comment.builder()
                    .id(2L)
                    .post(post)
                    .user(postAuthor)
                    .parent(parentComment)
                    .content("Self reply")
                    .build();
            CommentResponse expectedResponse = new CommentResponse(
                    2L, new AuthorInfo(1L, "author", null, null),
                    "Self reply", false, null, List.of()
            );

            when(commentRepository.findById(10L)).thenReturn(Optional.of(parentComment));
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(postAuthor));
            when(commentRepository.save(any(Comment.class))).thenReturn(reply);
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(2);
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            commentService.createReply(1L, 10L, 1L, request);

            verify(kafkaProducerService, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should use parent post when postId is null")
        void createReply_nullPostId_usesParentPost() {
            CommentCreateRequest request = new CommentCreateRequest("Reply");
            Comment reply = Comment.builder()
                    .id(3L)
                    .post(post)
                    .user(commenter)
                    .parent(parentComment)
                    .content("Reply")
                    .build();
            CommentResponse expectedResponse = new CommentResponse(
                    3L, new AuthorInfo(2L, "commenter", null, null),
                    "Reply", false, null, List.of()
            );

            when(commentRepository.findById(10L)).thenReturn(Optional.of(parentComment));
            when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
            when(commentRepository.save(any(Comment.class))).thenReturn(reply);
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(2);
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            CommentResponse result = commentService.createReply(null, 10L, 2L, request);

            assertThat(result).isNotNull();
            verify(postRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("should throw exception when parent comment not found")
        void createReply_parentNotFound_throwsException() {
            CommentCreateRequest request = new CommentCreateRequest("Reply");
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.createReply(1L, 999L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update comment successfully")
        void update_success() {
            CommentUpdateRequest request = new CommentUpdateRequest("Updated comment");
            CommentResponse expectedResponse = new CommentResponse(
                    1L, new AuthorInfo(2L, "commenter", null, null),
                    "Updated comment", false, null, List.of()
            );

            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(commentMapper.toResponse(any(Comment.class), any())).thenReturn(expectedResponse);

            CommentResponse result = commentService.update(1L, 2L, request);

            assertThat(result).isNotNull();
            assertThat(comment.getContent()).isEqualTo("Updated comment");
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void update_notOwner_throwsException() {
            CommentUpdateRequest request = new CommentUpdateRequest("Updated");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            User nonOwner = User.builder().id(999L).nickname("other").role(Role.USER).build();
            when(userRepository.findById(999L)).thenReturn(Optional.of(nonOwner));

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.update(1L, 999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when comment not found")
        void update_commentNotFound_throwsException() {
            CommentUpdateRequest request = new CommentUpdateRequest("Updated");
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.update(999L, 2L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft delete comment and update post comment count")
        void delete_success() {
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(commentRepository.countByPostIdAndDeletedAtIsNull(1L)).thenReturn(0);

            commentService.delete(1L, 2L);

            assertThat(comment.isDeleted()).isTrue();
            verify(commentRepository).countByPostIdAndDeletedAtIsNull(1L);
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void delete_notOwner_throwsException() {
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            User nonOwner = User.builder().id(999L).nickname("other").role(Role.USER).build();
            when(userRepository.findById(999L)).thenReturn(Optional.of(nonOwner));

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.delete(1L, 999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when comment not found")
        void delete_commentNotFound_throwsException() {
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.delete(999L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getByPost")
    class GetByPost {

        @Test
        @DisplayName("should return comments with replies for a post")
        void getByPost_success() {
            Comment rootComment = Comment.builder()
                    .id(1L)
                    .post(post)
                    .user(postAuthor)
                    .content("Root comment")
                    .build();

            Comment replyComment = Comment.builder()
                    .id(2L)
                    .post(post)
                    .user(commenter)
                    .parent(rootComment)
                    .content("Reply")
                    .build();

            CommentResponse replyResponse = new CommentResponse(
                    2L, new AuthorInfo(2L, "commenter", null, null),
                    "Reply", false, null, List.of()
            );
            CommentResponse rootResponse = new CommentResponse(
                    1L, new AuthorInfo(1L, "author", null, null),
                    "Root comment", false, null, List.of(replyResponse)
            );

            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(commentRepository.findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByIdAsc(eq(1L), any()))
                    .thenReturn(List.of(rootComment));
            when(commentRepository.findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(replyComment));
            when(commentMapper.toResponse(eq(replyComment), any())).thenReturn(replyResponse);
            when(commentMapper.toResponseWithReplies(eq(rootComment), anyList(), any())).thenReturn(rootResponse);

            CursorPageResponse<CommentResponse> result = commentService.getByPost(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).replies()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no comments")
        void getByPost_noComments_returnsEmptyList() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(commentRepository.findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByIdAsc(eq(1L), any()))
                    .thenReturn(Collections.emptyList());

            CursorPageResponse<CommentResponse> result = commentService.getByPost(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when post not found")
        void getByPost_postNotFound_throwsException() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> commentService.getByPost(999L, pageRequest));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }
}
