package com.lockerroom.resourceservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.lockerroom.resourceservice.configuration.SecurityConfig;
import com.lockerroom.resourceservice.configuration.WebMvcConfig;
import com.lockerroom.resourceservice.dto.request.PostCreateRequest;
import com.lockerroom.resourceservice.dto.request.PostUpdateRequest;
import com.lockerroom.resourceservice.dto.request.ReportRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.PostService;
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

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/posts";
    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 100L;
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

    private PostDetailResponse createPostDetailResponse() {
        return new PostDetailResponse(
                POST_ID,
                1L,
                "Free Board",
                new AuthorInfo(USER_ID, "testUser", null, null),
                "Test Title",
                "Test Content",
                10,
                5,
                3,
                false,
                false,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/posts")
    class Create {

        @Test
        @DisplayName("should create a post and return 201")
        void create_success() throws Exception {
            // given
            PostCreateRequest request = new PostCreateRequest(1L, "Test Title", "Test Content", List.of());
            PostDetailResponse response = createPostDetailResponse();
            when(postService.create(eq(USER_ID), any(PostCreateRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(POST_ID))
                    .andExpect(jsonPath("$.data.title").value("Test Title"));

            verify(postService).create(eq(USER_ID), any(PostCreateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void create_blankTitle_returns400() throws Exception {
            // given
            PostCreateRequest request = new PostCreateRequest(1L, "", "Test Content", List.of());

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).create(any(), any());
        }

        @Test
        @DisplayName("should return 400 when boardId is null")
        void create_nullBoardId_returns400() throws Exception {
            // given
            PostCreateRequest request = new PostCreateRequest(null, "Title", "Content", List.of());

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}")
    class GetDetail {

        @Test
        @DisplayName("should return post detail with 200")
        void getDetail_success() throws Exception {
            // given
            PostDetailResponse response = createPostDetailResponse();
            when(postService.getDetail(eq(POST_ID), eq(USER_ID))).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/{postId}", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(POST_ID))
                    .andExpect(jsonPath("$.data.title").value("Test Title"))
                    .andExpect(jsonPath("$.data.content").value("Test Content"));

            verify(postService).getDetail(POST_ID, USER_ID);
        }

        @Test
        @DisplayName("should return post detail without auth (public endpoint)")
        void getDetail_withoutAuth_success() throws Exception {
            // given
            PostDetailResponse response = createPostDetailResponse();
            when(postService.getDetail(eq(POST_ID), any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/{postId}", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(POST_ID));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/posts/{postId}")
    class Update {

        @Test
        @DisplayName("should update a post and return 200")
        void update_success() throws Exception {
            // given
            PostUpdateRequest request = new PostUpdateRequest("Updated Title", "Updated Content", List.of());
            PostDetailResponse response = new PostDetailResponse(
                    POST_ID, 1L, "Free Board",
                    new AuthorInfo(USER_ID, "testUser", null, null),
                    "Updated Title", "Updated Content",
                    10, 5, 3, false, false, List.of(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(postService.update(eq(POST_ID), eq(USER_ID), any(PostUpdateRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/{postId}", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.content").value("Updated Content"));

            verify(postService).update(eq(POST_ID), eq(USER_ID), any(PostUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void update_blankTitle_returns400() throws Exception {
            // given
            PostUpdateRequest request = new PostUpdateRequest("", "Updated Content", List.of());

            // when & then
            mockMvc.perform(put(BASE_URL + "/{postId}", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).update(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId}")
    class Delete {

        @Test
        @DisplayName("should delete a post and return 204")
        void delete_success() throws Exception {
            // given
            doNothing().when(postService).delete(POST_ID, USER_ID);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{postId}", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isNoContent());

            verify(postService).delete(POST_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/like")
    class ToggleLike {

        @Test
        @DisplayName("should toggle like and return 200")
        void toggleLike_success() throws Exception {
            // given
            LikeResponse response = new LikeResponse(POST_ID, true, 6);
            when(postService.toggleLike(POST_ID, USER_ID)).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/{postId}/like", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.postId").value(POST_ID))
                    .andExpect(jsonPath("$.data.isLiked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(6));

            verify(postService).toggleLike(POST_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/report")
    class Report {

        @Test
        @DisplayName("should report a post and return 201")
        void report_success() throws Exception {
            // given
            ReportRequest request = new ReportRequest("Inappropriate content");
            ReportResponse response = new ReportResponse(1L, POST_ID, ReportStatus.PENDING);
            when(postService.report(eq(POST_ID), eq(USER_ID), any(ReportRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/{postId}/report", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.reportId").value(1))
                    .andExpect(jsonPath("$.data.postId").value(POST_ID))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(postService).report(eq(POST_ID), eq(USER_ID), any(ReportRequest.class));
        }

        @Test
        @DisplayName("should return 400 when reason is blank")
        void report_blankReason_returns400() throws Exception {
            // given
            ReportRequest request = new ReportRequest("");

            // when & then
            mockMvc.perform(post(BASE_URL + "/{postId}/report", POST_ID)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).report(any(), any(), any());
        }
    }
}
