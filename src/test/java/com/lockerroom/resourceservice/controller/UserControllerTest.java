package com.lockerroom.resourceservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.lockerroom.resourceservice.configuration.SecurityConfig;
import com.lockerroom.resourceservice.configuration.WebMvcConfig;
import com.lockerroom.resourceservice.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/users";
    private static final Long USER_ID = 1L;
    private static final String KEYCLOAK_ID = "kc-user-uuid";

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(USER_ID)
                .keycloakId(KEYCLOAK_ID)
                .email("test@example.com")
                .nickname("testUser")
                .role(Role.USER)
                .build();
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
    }

    private UserResponse createUserResponse() {
        return new UserResponse(
                USER_ID,
                "test@example.com",
                "testUser",
                Role.USER,
                OAuthProvider.GOOGLE,
                List.of(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetMyInfo {

        @Test
        @DisplayName("should return current user info with 200")
        void getMyInfo_success() throws Exception {
            // given
            UserResponse response = createUserResponse();
            when(userService.getMyInfo(USER_ID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(USER_ID))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("testUser"))
                    .andExpect(jsonPath("$.data.role").value("USER"));

            verify(userService).getMyInfo(USER_ID);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateMyInfo {

        @Test
        @DisplayName("should update user info and return 200")
        void updateMyInfo_success() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("newNickname", null, null);
            UserResponse response = new UserResponse(
                    USER_ID, "test@example.com", "newNickname",
                    Role.USER, OAuthProvider.GOOGLE, List.of(), LocalDateTime.now()
            );
            when(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/me")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.nickname").value("newNickname"));

            verify(userService).updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when nickname is too short")
        void updateMyInfo_shortNickname_returns400() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("a", null, null);

            // when & then
            mockMvc.perform(put(BASE_URL + "/me")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateMyInfo(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/me")
    class Withdraw {

        @Test
        @DisplayName("should withdraw user and return 200")
        void withdraw_success() throws Exception {
            // given
            WithdrawRequest request = new WithdrawRequest("No longer using", "password123");
            doNothing().when(userService).withdraw(eq(USER_ID), any(WithdrawRequest.class));

            // when & then
            mockMvc.perform(delete(BASE_URL + "/me")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            verify(userService).withdraw(eq(USER_ID), any(WithdrawRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/posts")
    class GetMyPosts {

        @Test
        @DisplayName("should return user posts with cursor pagination")
        void getMyPosts_success() throws Exception {
            // given
            UserPostListResponse post = new UserPostListResponse(
                    10L, 1L, "Free Board", "My Post", 100, 5, 3, LocalDateTime.now()
            );
            CursorPageResponse<UserPostListResponse> response = CursorPageResponse.<UserPostListResponse>builder()
                    .items(List.of(post))
                    .nextCursor("10")
                    .hasNext(false)
                    .build();
            when(userService.getMyPosts(eq(USER_ID), any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/posts")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(10))
                    .andExpect(jsonPath("$.data.items[0].title").value("My Post"))
                    .andExpect(jsonPath("$.data.hasNext").value(false));

            verify(userService).getMyPosts(eq(USER_ID), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/comments")
    class GetMyComments {

        @Test
        @DisplayName("should return user comments with cursor pagination")
        void getMyComments_success() throws Exception {
            // given
            UserCommentListResponse comment = new UserCommentListResponse(
                    20L, 10L, "Post Title", "My comment", LocalDateTime.now()
            );
            CursorPageResponse<UserCommentListResponse> response = CursorPageResponse.<UserCommentListResponse>builder()
                    .items(List.of(comment))
                    .nextCursor("20")
                    .hasNext(true)
                    .build();
            when(userService.getMyComments(eq(USER_ID), any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/comments")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(20))
                    .andExpect(jsonPath("$.data.items[0].content").value("My comment"))
                    .andExpect(jsonPath("$.data.hasNext").value(true));

            verify(userService).getMyComments(eq(USER_ID), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/likes")
    class GetMyLikes {

        @Test
        @DisplayName("should return user likes with cursor pagination")
        void getMyLikes_success() throws Exception {
            // given
            UserLikeListResponse like = new UserLikeListResponse(
                    30L, 1L, "Free Board", "Liked Post", "author1",
                    50, 10, 2, LocalDateTime.now()
            );
            CursorPageResponse<UserLikeListResponse> response = CursorPageResponse.<UserLikeListResponse>builder()
                    .items(List.of(like))
                    .nextCursor("30")
                    .hasNext(false)
                    .build();
            when(userService.getMyLikes(eq(USER_ID), any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/likes")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(30))
                    .andExpect(jsonPath("$.data.items[0].title").value("Liked Post"))
                    .andExpect(jsonPath("$.data.hasNext").value(false));

            verify(userService).getMyLikes(eq(USER_ID), any());
        }
    }
}
