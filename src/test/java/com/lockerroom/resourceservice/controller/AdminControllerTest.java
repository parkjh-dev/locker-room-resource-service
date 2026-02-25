package com.lockerroom.resourceservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.lockerroom.resourceservice.configuration.SecurityConfig;
import com.lockerroom.resourceservice.configuration.WebMvcConfig;
import com.lockerroom.resourceservice.dto.request.NoticeCreateRequest;
import com.lockerroom.resourceservice.dto.request.ReportProcessRequest;
import com.lockerroom.resourceservice.dto.request.SuspendRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NoticeScope;
import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.AdminService;
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

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin";
    private static final Long ADMIN_ID = 1L;
    private static final String KEYCLOAK_ID = "kc-admin-uuid";

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .id(ADMIN_ID)
                .keycloakId(KEYCLOAK_ID)
                .email("admin@example.com")
                .nickname("admin1")
                .role(Role.ADMIN)
                .build();
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(admin));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetUsers {

        @Test
        @DisplayName("should return user list with ADMIN role")
        void getUsers_asAdmin_success() throws Exception {
            // given
            AdminUserListResponse user = new AdminUserListResponse(
                    10L, "user@example.com", "user1", Role.USER,
                    OAuthProvider.GOOGLE, false, LocalDateTime.now()
            );
            CursorPageResponse<AdminUserListResponse> response = CursorPageResponse.<AdminUserListResponse>builder()
                    .items(List.of(user))
                    .nextCursor("10")
                    .hasNext(false)
                    .build();
            when(adminService.getUsers(any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/users")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(10))
                    .andExpect(jsonPath("$.data.items[0].email").value("user@example.com"))
                    .andExpect(jsonPath("$.data.hasNext").value(false));

            verify(adminService).getUsers(any());
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void getUsers_asUser_returns403() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/users")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).getUsers(any());
        }

        @Test
        @DisplayName("should return 401 when no auth provided")
        void getUsers_noAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/users"))
                    .andExpect(status().isUnauthorized());

            verify(adminService, never()).getUsers(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/suspend")
    class SuspendUser {

        @Test
        @DisplayName("should suspend user and return 200")
        void suspendUser_asAdmin_success() throws Exception {
            // given
            SuspendRequest request = new SuspendRequest(
                    "Violation of rules",
                    LocalDateTime.now().plusDays(7)
            );
            doNothing().when(adminService).suspendUser(eq(10L), eq(ADMIN_ID), any(SuspendRequest.class));

            // when & then
            mockMvc.perform(put(BASE_URL + "/users/{userId}/suspend", 10L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            verify(adminService).suspendUser(eq(10L), eq(ADMIN_ID), any(SuspendRequest.class));
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void suspendUser_asUser_returns403() throws Exception {
            // given
            SuspendRequest request = new SuspendRequest(
                    "Violation", LocalDateTime.now().plusDays(7)
            );

            // when & then
            mockMvc.perform(put(BASE_URL + "/users/{userId}/suspend", 10L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).suspendUser(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notices")
    class CreateNotice {

        @Test
        @DisplayName("should create notice and return 201")
        void createNotice_asAdmin_success() throws Exception {
            // given
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "Important Notice", "Notice content", true, NoticeScope.ALL, null
            );
            NoticeDetailResponse response = new NoticeDetailResponse(
                    1L, "Important Notice", "Notice content", true,
                    NoticeScope.ALL, null, null, "admin1",
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(adminService.createNotice(eq(ADMIN_ID), any(NoticeCreateRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/notices")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("Important Notice"))
                    .andExpect(jsonPath("$.data.scope").value("ALL"));

            verify(adminService).createNotice(eq(ADMIN_ID), any(NoticeCreateRequest.class));
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void createNotice_asUser_returns403() throws Exception {
            // given
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "Notice", "Content", false, NoticeScope.ALL, null
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/notices")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).createNotice(any(), any());
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void createNotice_blankTitle_returns400() throws Exception {
            // given
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "", "Content", false, NoticeScope.ALL, null
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/notices")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(adminService, never()).createNotice(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/reports")
    class GetReports {

        @Test
        @DisplayName("should return report list with ADMIN role")
        void getReports_asAdmin_success() throws Exception {
            // given
            ReportListResponse report = new ReportListResponse(
                    1L, 100L, "Reported Post", "reporter1",
                    "Spam content", ReportStatus.PENDING, LocalDateTime.now()
            );
            CursorPageResponse<ReportListResponse> response = CursorPageResponse.<ReportListResponse>builder()
                    .items(List.of(report))
                    .nextCursor("1")
                    .hasNext(false)
                    .build();
            when(adminService.getReports(any())).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/reports")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(1))
                    .andExpect(jsonPath("$.data.items[0].reason").value("Spam content"))
                    .andExpect(jsonPath("$.data.items[0].status").value("PENDING"));

            verify(adminService).getReports(any());
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void getReports_asUser_returns403() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/reports")
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).getReports(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/reports/{reportId}")
    class ProcessReport {

        @Test
        @DisplayName("should process report and return 200")
        void processReport_asAdmin_success() throws Exception {
            // given
            ReportProcessRequest request = new ReportProcessRequest(ReportStatus.APPROVED, "Post deleted");
            doNothing().when(adminService).processReport(eq(1L), eq(ADMIN_ID), any(ReportProcessRequest.class));

            // when & then
            mockMvc.perform(put(BASE_URL + "/reports/{reportId}", 1L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            verify(adminService).processReport(eq(1L), eq(ADMIN_ID), any(ReportProcessRequest.class));
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void processReport_asUser_returns403() throws Exception {
            // given
            ReportProcessRequest request = new ReportProcessRequest(ReportStatus.APPROVED, "action");

            // when & then
            mockMvc.perform(put(BASE_URL + "/reports/{reportId}", 1L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).processReport(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/notices/{noticeId}")
    class UpdateNotice {

        @Test
        @DisplayName("should update notice and return 200")
        void updateNotice_asAdmin_success() throws Exception {
            // given
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "Updated Notice", "Updated content", false, NoticeScope.ALL, null
            );
            NoticeDetailResponse response = new NoticeDetailResponse(
                    1L, "Updated Notice", "Updated content", false,
                    NoticeScope.ALL, null, null, "admin1",
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(adminService.updateNotice(eq(1L), eq(ADMIN_ID), any(NoticeCreateRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/notices/{noticeId}", 1L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.title").value("Updated Notice"));

            verify(adminService).updateNotice(eq(1L), eq(ADMIN_ID), any(NoticeCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/notices/{noticeId}")
    class DeleteNotice {

        @Test
        @DisplayName("should delete notice and return 200")
        void deleteNotice_asAdmin_success() throws Exception {
            // given
            doNothing().when(adminService).deleteNotice(1L);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/notices/{noticeId}", 1L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            verify(adminService).deleteNotice(1L);
        }

        @Test
        @DisplayName("should return 403 when user role is USER")
        void deleteNotice_asUser_returns403() throws Exception {
            // when & then
            mockMvc.perform(delete(BASE_URL + "/notices/{noticeId}", 1L)
                            .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID)).authorities(() -> "ROLE_USER")))
                    .andExpect(status().isForbidden());

            verify(adminService, never()).deleteNotice(any());
        }
    }
}
