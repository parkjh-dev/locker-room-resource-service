package com.lockerroom.resourceservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.lockerroom.resourceservice.configuration.SecurityConfig;
import com.lockerroom.resourceservice.dto.request.CommentCreateRequest;
import com.lockerroom.resourceservice.dto.request.CommentUpdateRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.AuthorInfo;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;

    private CommentResponse createCommentResponse() {
        return new CommentResponse(
                COMMENT_ID,
                new AuthorInfo(USER_ID, "testUser"),
                "Test comment content",
                false,
                LocalDateTime.now(),
                List.of()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}/comments")
    class GetByPost {

        @Test
        @DisplayName("should return comments for a post with 200")
        void getByPost_success() throws Exception {
            // given
            CommentResponse comment1 = createCommentResponse();
            CommentResponse comment2 = new CommentResponse(
                    201L, new AuthorInfo(2L, "user2"), "Another comment",
                    false, LocalDateTime.now(), List.of()
            );
            when(commentService.getByPost(POST_ID)).thenReturn(List.of(comment1, comment2));

            // when & then
            mockMvc.perform(get("/api/v1/posts/{postId}/comments", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data[0].content").value("Test comment content"));

            verify(commentService).getByPost(POST_ID);
        }

        @Test
        @DisplayName("should return empty list when no comments exist")
        void getByPost_emptyList() throws Exception {
            // given
            when(commentService.getByPost(POST_ID)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/posts/{postId}/comments", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/comments")
    class Create {

        @Test
        @DisplayName("should create a comment and return 201")
        void create_success() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest("New comment");
            CommentResponse response = createCommentResponse();
            when(commentService.create(eq(POST_ID), eq(USER_ID), any(CommentCreateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/comments", POST_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data.content").value("Test comment content"));

            verify(commentService).create(eq(POST_ID), eq(USER_ID), any(CommentCreateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when content is blank")
        void create_blankContent_returns400() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/comments", POST_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/comments/{commentId}")
    class Update {

        @Test
        @DisplayName("should update a comment and return 200")
        void update_success() throws Exception {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("Updated comment");
            CommentResponse response = new CommentResponse(
                    COMMENT_ID, new AuthorInfo(USER_ID, "testUser"),
                    "Updated comment", false, LocalDateTime.now(), List.of()
            );
            when(commentService.update(eq(COMMENT_ID), eq(USER_ID), any(CommentUpdateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/comments/{commentId}", COMMENT_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data.content").value("Updated comment"));

            verify(commentService).update(eq(COMMENT_ID), eq(USER_ID), any(CommentUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when content is blank")
        void update_blankContent_returns400() throws Exception {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("");

            // when & then
            mockMvc.perform(put("/api/v1/comments/{commentId}", COMMENT_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).update(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/comments/{commentId}")
    class Delete {

        @Test
        @DisplayName("should delete a comment and return 200")
        void delete_success() throws Exception {
            // given
            doNothing().when(commentService).delete(COMMENT_ID, USER_ID);

            // when & then
            mockMvc.perform(delete("/api/v1/comments/{commentId}", COMMENT_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            verify(commentService).delete(COMMENT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/comments/{commentId}/replies")
    class CreateReply {

        @Test
        @DisplayName("should create a reply and return 201")
        void createReply_success() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest("Reply content");
            CommentResponse response = new CommentResponse(
                    300L, new AuthorInfo(USER_ID, "testUser"),
                    "Reply content", false, LocalDateTime.now(), List.of()
            );
            when(commentService.createReply(isNull(), eq(COMMENT_ID), eq(USER_ID), any(CommentCreateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/comments/{commentId}/replies", COMMENT_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(300))
                    .andExpect(jsonPath("$.data.content").value("Reply content"));

            verify(commentService).createReply(isNull(), eq(COMMENT_ID), eq(USER_ID), any(CommentCreateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when reply content is blank")
        void createReply_blankContent_returns400() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/comments/{commentId}/replies", COMMENT_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).createReply(any(), any(), any(), any());
        }
    }
}
