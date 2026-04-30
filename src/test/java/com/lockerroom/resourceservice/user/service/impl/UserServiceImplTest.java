package com.lockerroom.resourceservice.user.service.impl;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.post.repository.PostLikeRepository;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.entity.PostLike;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.comment.dto.response.UserCommentListResponse;

import com.lockerroom.resourceservice.comment.repository.CommentRepository;

import com.lockerroom.resourceservice.comment.model.entity.Comment;

import com.lockerroom.resourceservice.user.dto.response.UserTeamInfo;

import com.lockerroom.resourceservice.user.dto.response.UserResponse;

import com.lockerroom.resourceservice.user.repository.UserWithdrawalRepository;

import com.lockerroom.resourceservice.user.repository.UserTeamRepository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.UserWithdrawal;

import com.lockerroom.resourceservice.user.model.entity.UserTeam;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.board.model.entity.Board;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.Sport;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.user.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.user.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.comment.mapper.CommentMapper;
import com.lockerroom.resourceservice.post.mapper.PostMapper;
import com.lockerroom.resourceservice.user.mapper.UserMapper;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.common.model.enums.Role;
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
    @Mock private FootballTeamRepository footballTeamRepository;
    @Mock private BaseballTeamRepository baseballTeamRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.SportRepository sportRepository;
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
                .password("oldpass")
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
        @DisplayName("should return user info with empty teams")
        void getMyInfo_success() {
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", false, null, "testuser",
                    Role.USER, OAuthProvider.GOOGLE, null, teamInfos, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.getMyInfo(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("user@test.com");
            assertThat(result.nickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should return user info with team resolved from football")
        void getMyInfo_withFootballTeam() {
            Sport football = Sport.builder().id(1L).nameKo("축구").nameEn("Football").isActive(true).build();
            UserTeam userTeam = UserTeam.builder().id(1L).user(user).teamId(10L).sport(football).build();
            FootballTeam ft = FootballTeam.builder().id(10L).nameKo("울산 HD FC").nameEn("Ulsan HD FC").build();
            UserTeamInfo teamInfo = new UserTeamInfo(10L, "울산 HD FC", 1L, "축구");
            List<UserTeamInfo> teamInfos = List.of(teamInfo);
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", false, null, "testuser",
                    Role.USER, OAuthProvider.GOOGLE, null, teamInfos, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(List.of(userTeam));
            when(footballTeamRepository.findAllById(List.of(10L))).thenReturn(List.of(ft));
            when(userMapper.toUserTeamInfo(userTeam, "울산 HD FC")).thenReturn(teamInfo);
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.getMyInfo(1L);

            assertThat(result).isNotNull();
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
        @DisplayName("should throw exception when entity not found")
        void getMyInfo_deletedUser_throwsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

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
            UserUpdateRequest request = new UserUpdateRequest("newnickname", null, null, null);
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", false, null, "newnickname",
                    Role.USER, OAuthProvider.GOOGLE, null, teamInfos, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("newnickname")).thenReturn(false);
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
            assertThat(user.getNickname()).isEqualTo("newnickname");
        }

        @Test
        @DisplayName("should update password successfully")
        void updateMyInfo_passwordUpdate_success() {
            UserUpdateRequest request = new UserUpdateRequest(null, "oldpass", "newpassword", null);
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", false, null, "testuser",
                    Role.USER, OAuthProvider.GOOGLE, null, teamInfos, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
            assertThat(user.getPassword()).isEqualTo("newpassword");
        }

        @Test
        @DisplayName("should throw exception when nickname is already taken")
        void updateMyInfo_duplicateNickname_throwsException() {
            UserUpdateRequest request = new UserUpdateRequest("taken", null, null, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("taken")).thenReturn(true);

            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.updateMyInfo(1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        }

        @Test
        @DisplayName("should allow same nickname when it belongs to current user")
        void updateMyInfo_sameNickname_success() {
            UserUpdateRequest request = new UserUpdateRequest("testuser", null, null, null);
            List<UserTeamInfo> teamInfos = Collections.emptyList();
            UserResponse expectedResponse = new UserResponse(
                    1L, "user@test.com", false, null, "testuser",
                    Role.USER, OAuthProvider.GOOGLE, null, teamInfos, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("testuser")).thenReturn(true);
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(user, teamInfos)).thenReturn(expectedResponse);

            UserResponse result = userService.updateMyInfo(1L, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void updateMyInfo_userNotFound_throwsException() {
            UserUpdateRequest request = new UserUpdateRequest("nick", null, null, null);
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
            WithdrawRequest request = new WithdrawRequest("No longer needed", "oldpass");

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

    /* ────────── Phase 3: 응원팀 등록 + 온보딩 skip ────────── */

    @Nested
    @DisplayName("addUserTeams (응원팀 등록 + 종목별 락)")
    class AddUserTeams {

        private Sport football;
        private Sport baseball;

        @BeforeEach
        void setupSports() {
            football = Sport.builder().id(1L).nameKo("축구").nameEn("Football").build();
            baseball = Sport.builder().id(2L).nameKo("야구").nameEn("Baseball").build();
        }

        @Test
        @DisplayName("새 종목 팀 등록 시 onboardingCompletedAt이 셋팅된다")
        void addUserTeams_newSport_setsOnboarding() {
            com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest req =
                    new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest(
                            List.of(new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest.TeamSelection(1L, 101L)));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(sportRepository.findById(1L)).thenReturn(Optional.of(football));
            when(userTeamRepository.existsByUserIdAndSportId(1L, 1L)).thenReturn(false);
            when(footballTeamRepository.existsById(101L)).thenReturn(true);
            // getMyInfo 분기 — 빈 teams 응답
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(eq(user), anyList())).thenReturn(
                    new UserResponse(1L, "user@test.com", false, null, "testuser",
                            Role.USER, OAuthProvider.GOOGLE, null, List.of(), null, null));

            userService.addUserTeams(1L, req);

            assertThat(user.getOnboardingCompletedAt()).isNotNull();
            verify(userTeamRepository).save(any(UserTeam.class));
        }

        @Test
        @DisplayName("이미 등록된 종목에 다시 등록 시 DUPLICATE_USER_TEAM 예외")
        void addUserTeams_duplicateSport_throws() {
            com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest req =
                    new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest(
                            List.of(new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest.TeamSelection(1L, 101L)));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(sportRepository.findById(1L)).thenReturn(Optional.of(football));
            when(userTeamRepository.existsByUserIdAndSportId(1L, 1L)).thenReturn(true); // 이미 등록됨

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.addUserTeams(1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USER_TEAM);
            verify(userTeamRepository, never()).save(any(UserTeam.class));
        }

        @Test
        @DisplayName("팀이 종목과 매칭 안 되면 INVALID_TEAM_FOR_SPORT 예외")
        void addUserTeams_invalidTeamForSport_throws() {
            com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest req =
                    new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest(
                            List.of(new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest.TeamSelection(1L, 999L)));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(sportRepository.findById(1L)).thenReturn(Optional.of(football));
            when(userTeamRepository.existsByUserIdAndSportId(1L, 1L)).thenReturn(false);
            when(footballTeamRepository.existsById(999L)).thenReturn(false); // 매칭 X

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.addUserTeams(1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TEAM_FOR_SPORT);
        }

        @Test
        @DisplayName("미지원 종목(농구·배구)은 INVALID_TEAM_FOR_SPORT — switch default 분기")
        void addUserTeams_unsupportedSport_throws() {
            Sport basketball = Sport.builder().id(3L).nameKo("농구").nameEn("Basketball").build();
            com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest req =
                    new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest(
                            List.of(new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest.TeamSelection(3L, 301L)));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(sportRepository.findById(3L)).thenReturn(Optional.of(basketball));
            when(userTeamRepository.existsByUserIdAndSportId(1L, 3L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.addUserTeams(1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TEAM_FOR_SPORT);
        }

        @Test
        @DisplayName("Sport ID가 존재하지 않으면 SPORT_NOT_FOUND")
        void addUserTeams_sportNotFound_throws() {
            com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest req =
                    new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest(
                            List.of(new com.lockerroom.resourceservice.user.dto.request.AddUserTeamsRequest.TeamSelection(99L, 101L)));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(sportRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.addUserTeams(1L, req));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SPORT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("skipOnboarding")
    class SkipOnboarding {

        @Test
        @DisplayName("최초 호출 시 onboardingCompletedAt이 셋팅된다")
        void skipOnboarding_firstCall_setsTimestamp() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(eq(user), anyList())).thenReturn(
                    new UserResponse(1L, "user@test.com", false, null, "testuser",
                            Role.USER, OAuthProvider.GOOGLE, null, List.of(), null, null));

            assertThat(user.getOnboardingCompletedAt()).isNull();
            userService.skipOnboarding(1L);
            assertThat(user.getOnboardingCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 셋된 사용자가 다시 호출해도 idempotent — 기존 시각 유지")
        void skipOnboarding_idempotent() {
            java.time.LocalDateTime original = java.time.LocalDateTime.of(2026, 1, 1, 0, 0);
            // reflection으로 셋
            User onboardedUser = User.builder()
                    .id(1L).email("user@test.com").nickname("testuser")
                    .password("p").role(Role.USER).provider(OAuthProvider.GOOGLE)
                    .onboardingCompletedAt(original)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(onboardedUser));
            when(userTeamRepository.findByUserIdWithSport(1L)).thenReturn(Collections.emptyList());
            when(userMapper.toResponse(eq(onboardedUser), anyList())).thenReturn(
                    new UserResponse(1L, "user@test.com", false, null, "testuser",
                            Role.USER, OAuthProvider.GOOGLE, null, List.of(), original, null));

            userService.skipOnboarding(1L);
            assertThat(onboardedUser.getOnboardingCompletedAt()).isEqualTo(original);
        }
    }
}
