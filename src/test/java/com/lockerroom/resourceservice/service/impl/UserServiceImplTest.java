package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.CommentMapper;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.mapper.UserMapper;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserTeamRepository userTeamRepository;
    @Mock private UserWithdrawalRepository userWithdrawalRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private UserMapper userMapper;
    @Mock private PostMapper postMapper;
    @Mock private CommentMapper commentMapper;

    @InjectMocks private UserServiceImpl userService;

    private User user;
    private Board board;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .provider(OAuthProvider.GOOGLE)
                .build();

        board = Board.builder()
                .id(1L)
                .name("Free Board")
                .type(BoardType.COMMON)
                .build();

        post = Post.builder()
                .id(1L)
                .board(board)
                .user(user)
                .title("Test Post")
                .content("Test Content")
                .build();
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("should return user info with teams")
        void getMyInfo_success() {
            List<UserTeamInfo> teamInfos = List.of(
                    new UserTeamInfo(1L, "Team A", 1L, "Soccer")
            );
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", "testuser",
                    Role.USER, OAuthProvider.GOOGLE, teamInfos, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toUserTeamInfoList(anyList())).thenReturn(teamInfos);
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.getMyInfo(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("user@test.com");
            assertThat(result.nickname()).isEqualTo("testuser");
            assertThat(result.teams()).hasSize(1);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void getMyInfo_userNotFound_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.getMyInfo(999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user is deleted")
        void getMyInfo_deletedUser_throwsException() {
            user.softDelete();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.getMyInfo(1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateMyInfo")
    class UpdateMyInfo {

        @Test
        @DisplayName("should update nickname successfully")
        void updateMyInfo_nicknameUpdate_success() {
            UserUpdateRequest request = new UserUpdateRequest("newnickname", null, null);
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", "newnickname",
                    Role.USER, OAuthProvider.GOOGLE, teamInfos, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("newnickname")).thenReturn(false);
            when(userTeamRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toUserTeamInfoList(anyList())).thenReturn(teamInfos);
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
            assertThat(user.getNickname()).isEqualTo("newnickname");
        }

        @Test
        @DisplayName("should update password successfully")
        void updateMyInfo_passwordUpdate_success() {
            UserUpdateRequest request = new UserUpdateRequest(null, "oldpass", "newpassword");
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", "testuser",
                    Role.USER, OAuthProvider.GOOGLE, teamInfos, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toUserTeamInfoList(anyList())).thenReturn(teamInfos);
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
            assertThat(user.getPassword()).isEqualTo("newpassword");
        }

        @Test
        @DisplayName("should throw exception when nickname is already taken")
        void updateMyInfo_duplicateNickname_throwsException() {
            UserUpdateRequest request = new UserUpdateRequest("taken", null, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("taken")).thenReturn(true);

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.updateMyInfo(1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        }

        @Test
        @DisplayName("should allow same nickname when it belongs to current user")
        void updateMyInfo_sameNickname_success() {
            UserUpdateRequest request = new UserUpdateRequest("testuser", null, null);
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", "testuser",
                    Role.USER, OAuthProvider.GOOGLE, teamInfos, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("testuser")).thenReturn(true);
            when(userTeamRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toUserTeamInfoList(anyList())).thenReturn(teamInfos);
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void updateMyInfo_userNotFound_throwsException() {
            UserUpdateRequest request = new UserUpdateRequest("nick", null, null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.updateMyInfo(999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("should withdraw user successfully")
        void withdraw_success() {
            WithdrawRequest request = new WithdrawRequest("No longer needed", "password");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userWithdrawalRepository.save(any(UserWithdrawal.class)))
                    .thenReturn(UserWithdrawal.builder().build());

            userService.withdraw(1L, request);

            assertThat(user.isDeleted()).isTrue();
            verify(userWithdrawalRepository).save(any(UserWithdrawal.class));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void withdraw_userNotFound_throwsException() {
            WithdrawRequest request = new WithdrawRequest("Reason", "password");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.withdraw(999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getMyPosts")
    class GetMyPosts {

        @Test
        @DisplayName("should return paginated posts with hasNext=true")
        void getMyPosts_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(2);

            Post post1 = Post.builder().id(3L).board(board).user(user).title("Post 3").content("c").build();
            Post post2 = Post.builder().id(2L).board(board).user(user).title("Post 2").content("c").build();
            Post post3 = Post.builder().id(1L).board(board).user(user).title("Post 1").content("c").build();

            UserPostListResponse response1 = new UserPostListResponse(3L, 1L, "Free Board", "Post 3", 0, 0, 0, null);
            UserPostListResponse response2 = new UserPostListResponse(2L, 1L, "Free Board", "Post 2", 0, 0, 0, null);

            when(postRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(post1, post2, post3));
            when(postMapper.toUserPostListResponse(post1)).thenReturn(response1);
            when(postMapper.toUserPostListResponse(post2)).thenReturn(response2);

            CursorPageResponse<UserPostListResponse> result = userService.getMyPosts(1L, pageRequest);

            assertThat(result.getItems()).hasSize(2);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(2L));
        }

        @Test
        @DisplayName("should return paginated posts with hasNext=false")
        void getMyPosts_noNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            UserPostListResponse response1 = new UserPostListResponse(1L, 1L, "Free Board", "Post 1", 0, 0, 0, null);

            when(postRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(post));
            when(postMapper.toUserPostListResponse(post)).thenReturn(response1);

            CursorPageResponse<UserPostListResponse> result = userService.getMyPosts(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return empty list when no posts")
        void getMyPosts_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(postRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(Collections.emptyList());

            CursorPageResponse<UserPostListResponse> result = userService.getMyPosts(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("getMyComments")
    class GetMyComments {

        @Test
        @DisplayName("should return paginated comments with hasNext=true")
        void getMyComments_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            Comment comment1 = Comment.builder().id(2L).post(post).user(user).content("Comment 2").build();
            Comment comment2 = Comment.builder().id(1L).post(post).user(user).content("Comment 1").build();

            UserCommentListResponse response1 = new UserCommentListResponse(2L, 1L, "Test Post", "Comment 2", null);

            when(commentRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(comment1, comment2));
            when(commentMapper.toUserCommentListResponse(comment1)).thenReturn(response1);

            CursorPageResponse<UserCommentListResponse> result = userService.getMyComments(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(2L));
        }

        @Test
        @DisplayName("should return empty list when no comments")
        void getMyComments_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(commentRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(Collections.emptyList());

            CursorPageResponse<UserCommentListResponse> result = userService.getMyComments(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("getMyLikes")
    class GetMyLikes {

        @Test
        @DisplayName("should return paginated likes with hasNext=true")
        void getMyLikes_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            Post likedPost1 = Post.builder().id(10L).board(board).user(user).title("Liked 1").content("c").build();
            Post likedPost2 = Post.builder().id(11L).board(board).user(user).title("Liked 2").content("c").build();

            PostLike like1 = PostLike.builder().id(1L).post(likedPost1).user(user).build();
            PostLike like2 = PostLike.builder().id(2L).post(likedPost2).user(user).build();

            UserLikeListResponse response1 = new UserLikeListResponse(
                    10L, 1L, "Free Board", "Liked 1", "testuser", 0, 0, 0, null
            );

            when(postLikeRepository.findByUserIdWithPost(eq(1L), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(like1, like2));
            when(postMapper.toUserLikeListResponse(likedPost1)).thenReturn(response1);

            CursorPageResponse<UserLikeListResponse> result = userService.getMyLikes(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(1L));
        }

        @Test
        @DisplayName("should return empty list when no likes")
        void getMyLikes_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(postLikeRepository.findByUserIdWithPost(eq(1L), isNull(), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            CursorPageResponse<UserLikeListResponse> result = userService.getMyLikes(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }
}
