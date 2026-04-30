package com.lockerroom.resourceservice.admin.service.impl;

import com.lockerroom.resourceservice.admin.dto.request.ReportProcessRequest;

import com.lockerroom.resourceservice.admin.dto.request.SuspendRequest;

import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;

import com.lockerroom.resourceservice.post.mapper.PostMapper;

import com.lockerroom.resourceservice.post.repository.PostReportRepository;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.enums.ReportAction;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;

import com.lockerroom.resourceservice.post.model.entity.PostReport;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.user.dto.response.AdminUserListResponse;

import com.lockerroom.resourceservice.user.mapper.UserMapper;

import com.lockerroom.resourceservice.user.repository.UserSuspensionRepository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.UserSuspension;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.board.repository.ActiveFootballBoardRepository;

import com.lockerroom.resourceservice.board.repository.ActiveBaseballBoardRepository;

import com.lockerroom.resourceservice.board.repository.FootballBoardRepository;

import com.lockerroom.resourceservice.board.repository.BaseballBoardRepository;

import com.lockerroom.resourceservice.board.repository.BoardRepository;

import com.lockerroom.resourceservice.board.model.entity.ActiveFootballBoard;

import com.lockerroom.resourceservice.board.model.entity.FootballBoard;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository;

import com.lockerroom.resourceservice.sport.repository.SportRepository;

import com.lockerroom.resourceservice.sport.model.entity.Country;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;

import com.lockerroom.resourceservice.sport.model.entity.Sport;

import com.lockerroom.resourceservice.inquiry.dto.response.AdminInquiryListResponse;

import com.lockerroom.resourceservice.inquiry.dto.response.InquiryReplyResponse;

import com.lockerroom.resourceservice.inquiry.dto.response.InquiryDetailResponse;

import com.lockerroom.resourceservice.inquiry.dto.request.InquiryReplyRequest;

import com.lockerroom.resourceservice.inquiry.mapper.InquiryMapper;

import com.lockerroom.resourceservice.inquiry.repository.InquiryReplyRepository;

import com.lockerroom.resourceservice.inquiry.repository.InquiryRepository;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;

import com.lockerroom.resourceservice.inquiry.model.entity.InquiryReply;

import com.lockerroom.resourceservice.inquiry.model.entity.Inquiry;

import com.lockerroom.resourceservice.file.mapper.FileMapper;

import com.lockerroom.resourceservice.file.repository.FileRepository;

import com.lockerroom.resourceservice.file.model.enums.TargetType;

import com.lockerroom.resourceservice.request.model.entity.Request;

import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;

import com.lockerroom.resourceservice.notice.dto.request.NoticeCreateRequest;

import com.lockerroom.resourceservice.notice.mapper.NoticeMapper;

import com.lockerroom.resourceservice.notice.repository.NoticeRepository;

import com.lockerroom.resourceservice.notice.model.entity.Notice;

import com.lockerroom.resourceservice.request.dto.response.AdminRequestListResponse;

import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;

import com.lockerroom.resourceservice.request.dto.request.RequestProcessRequest;

import com.lockerroom.resourceservice.request.mapper.RequestMapper;

import com.lockerroom.resourceservice.request.repository.RequestRepository;

import com.lockerroom.resourceservice.request.model.enums.RequestType;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;

import com.lockerroom.resourceservice.common.model.enums.Role;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.infrastructure.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSuspensionRepository userSuspensionRepository;
    @Mock private PostReportRepository postReportRepository;
    @Mock private PostRepository postRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private InquiryRepository inquiryRepository;
    @Mock private InquiryReplyRepository inquiryReplyRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private FileRepository fileRepository;
    @Mock private SportRepository sportRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private FootballLeagueRepository footballLeagueRepository;
    @Mock private FootballTeamRepository footballTeamRepository;
    @Mock private FootballBoardRepository footballBoardRepository;
    @Mock private ActiveFootballBoardRepository activeFootballBoardRepository;
    @Mock private BaseballLeagueRepository baseballLeagueRepository;
    @Mock private BaseballTeamRepository baseballTeamRepository;
    @Mock private BaseballBoardRepository baseballBoardRepository;
    @Mock private ActiveBaseballBoardRepository activeBaseballBoardRepository;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private UserMapper userMapper;
    @Mock private PostMapper postMapper;
    @Mock private NoticeMapper noticeMapper;
    @Mock private InquiryMapper inquiryMapper;
    @Mock private RequestMapper requestMapper;
    @Mock private FileMapper fileMapper;

    @InjectMocks private AdminServiceImpl adminService;

    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .nickname("admin")
                .role(Role.ADMIN)
                .build();

        user = User.builder()
                .id(2L)
                .email("user@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("getUsers")
    class GetUsers {

        @Test
        @DisplayName("should return paginated user list")
        void getUsers_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            AdminUserListResponse response = new AdminUserListResponse(
                    2L, "user@test.com", "testuser", Role.USER, null, false, null);

            when(userRepository.findUsersFiltered(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(user));
            when(userSuspensionRepository.findActiveSuspendedUserIds(anyList(), any(OffsetDateTime.class)))
                    .thenReturn(List.of());
            when(userMapper.toAdminListResponse(user, false)).thenReturn(response);

            CursorPageResponse<AdminUserListResponse> result = adminService.getUsers(pageRequest, null, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should set hasNext when more results exist")
        void getUsers_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            User user2 = User.builder().id(3L).email("u2@test.com").nickname("user2").role(Role.USER).build();
            AdminUserListResponse response = new AdminUserListResponse(
                    2L, "user@test.com", "testuser", Role.USER, null, false, null);

            when(userRepository.findUsersFiltered(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(user, user2));
            when(userSuspensionRepository.findActiveSuspendedUserIds(anyList(), any(OffsetDateTime.class)))
                    .thenReturn(List.of());
            when(userMapper.toAdminListResponse(user, false)).thenReturn(response);

            CursorPageResponse<AdminUserListResponse> result = adminService.getUsers(pageRequest, null, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(2L));
        }
    }

    @Nested
    @DisplayName("suspendUser")
    class SuspendUser {

        @Test
        @DisplayName("should suspend user successfully")
        void suspendUser_success() {
            SuspendRequest request = new SuspendRequest("규정 위반", OffsetDateTime.now().plusDays(7));

            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(userSuspensionRepository.save(any(UserSuspension.class)))
                    .thenReturn(UserSuspension.builder().build());

            adminService.suspendUser(2L, 1L, request);

            verify(userSuspensionRepository).save(any(UserSuspension.class));
        }

        @Test
        @DisplayName("should throw exception when target user not found")
        void suspendUser_userNotFound() {
            SuspendRequest request = new SuspendRequest("규정 위반", OffsetDateTime.now().plusDays(7));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.suspendUser(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when admin not found")
        void suspendUser_adminNotFound() {
            SuspendRequest request = new SuspendRequest("규정 위반", OffsetDateTime.now().plusDays(7));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.suspendUser(2L, 999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getReports")
    class GetReports {

        @Test
        @DisplayName("should return paginated report list")
        void getReports_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            Post post = Post.builder().id(1L).title("Test").build();
            PostReport report = PostReport.builder().id(1L).post(post).user(user).reason("spam").build();
            ReportListResponse response = new ReportListResponse(
                    1L, 1L, "Test", "testuser", "spam", ReportStatus.PENDING, null);

            when(postReportRepository.findReportsFiltered(isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(report));
            when(postMapper.toReportListResponse(report)).thenReturn(response);

            CursorPageResponse<ReportListResponse> result = adminService.getReports(pageRequest, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).reason()).isEqualTo("spam");
        }
    }

    @Nested
    @DisplayName("processReport")
    class ProcessReport {

        @Test
        @DisplayName("should process report and send Kafka event")
        void processReport_success() {
            Post post = Post.builder().id(1L).title("Test").build();
            PostReport report = PostReport.builder().id(1L).post(post).user(user).reason("spam").build();
            ReportProcessRequest request = new ReportProcessRequest(ReportStatus.APPROVED, null, null);

            when(postReportRepository.findByIdWithPostAndUser(1L)).thenReturn(Optional.of(report));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            adminService.processReport(1L, 1L, request);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
            verify(kafkaProducerService).send(
                    eq(Constants.KAFKA_TOPIC_NOTIFICATION_REPORT_PROCESSED), anyString(), any());
        }

        @Test
        @DisplayName("should soft delete post when action is DELETE")
        void processReport_deleteAction() {
            Post post = Post.builder().id(1L).title("Test").build();
            PostReport report = PostReport.builder().id(1L).post(post).user(user).reason("spam").build();
            ReportProcessRequest request = new ReportProcessRequest(ReportStatus.APPROVED, ReportAction.DELETE_POST, null);

            when(postReportRepository.findByIdWithPostAndUser(1L)).thenReturn(Optional.of(report));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            adminService.processReport(1L, 1L, request);

            assertThat(post.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when report not found")
        void processReport_reportNotFound() {
            ReportProcessRequest request = new ReportProcessRequest(ReportStatus.APPROVED, null, null);
            when(postReportRepository.findByIdWithPostAndUser(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.processReport(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createNotice")
    class CreateNotice {

        @Test
        @DisplayName("should create notice successfully")
        void createNotice_success() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "공지 제목", "공지 내용", true, null, null);
            Notice notice = Notice.builder()
                    .id(1L).title("공지 제목").content("공지 내용").isPinned(true).admin(admin)
                    .build();
            NoticeDetailResponse response = new NoticeDetailResponse(
                    1L, "공지 제목", "공지 내용", true, com.lockerroom.resourceservice.notice.model.enums.NoticeScope.ALL, null, null, "admin", null, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
            when(noticeMapper.toDetailResponse(notice)).thenReturn(response);

            NoticeDetailResponse result = adminService.createNotice(1L, request);

            assertThat(result.title()).isEqualTo("공지 제목");
            verify(noticeRepository).save(any(Notice.class));
        }
    }

    @Nested
    @DisplayName("updateNotice")
    class UpdateNotice {

        @Test
        @DisplayName("should update notice successfully")
        void updateNotice_success() {
            Notice notice = Notice.builder()
                    .id(1L).title("원래 제목").content("원래 내용").isPinned(false).admin(admin)
                    .build();
            NoticeCreateRequest request = new NoticeCreateRequest(
                    "수정 제목", "수정 내용", true, null, null);
            NoticeDetailResponse response = new NoticeDetailResponse(
                    1L, "수정 제목", "수정 내용", true, com.lockerroom.resourceservice.notice.model.enums.NoticeScope.ALL, null, null, "admin", null, null);

            when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
            when(noticeMapper.toDetailResponse(notice)).thenReturn(response);

            NoticeDetailResponse result = adminService.updateNotice(1L, 1L, request);

            assertThat(notice.getTitle()).isEqualTo("수정 제목");
            assertThat(notice.getContent()).isEqualTo("수정 내용");
            assertThat(notice.isPinned()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when notice not found")
        void updateNotice_notFound() {
            NoticeCreateRequest request = new NoticeCreateRequest("제목", "내용", false, null, null);
            when(noticeRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.updateNotice(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when entity not found")
        void updateNotice_deleted() {
            NoticeCreateRequest request = new NoticeCreateRequest("제목", "내용", false, null, null);
            when(noticeRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.updateNotice(1L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteNotice")
    class DeleteNotice {

        @Test
        @DisplayName("should soft delete notice successfully")
        void deleteNotice_success() {
            Notice notice = Notice.builder().id(1L).title("제목").content("내용").admin(admin).build();
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

            adminService.deleteNotice(1L);

            assertThat(notice.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when notice not found")
        void deleteNotice_notFound() {
            when(noticeRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.deleteNotice(999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getInquiries")
    class GetInquiries {

        @Test
        @DisplayName("should return paginated inquiry list")
        void getInquiries_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            Inquiry inquiry = Inquiry.builder()
                    .id(1L).user(user).type(InquiryType.BUG).title("버그").content("내용").build();
            AdminInquiryListResponse response = new AdminInquiryListResponse(
                    1L, "testuser", InquiryType.BUG, "버그", InquiryStatus.PENDING, null);

            when(inquiryRepository.findInquiriesFiltered(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(inquiry));
            when(inquiryMapper.toAdminListResponse(inquiry)).thenReturn(response);

            CursorPageResponse<AdminInquiryListResponse> result = adminService.getInquiries(pageRequest, null, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).type()).isEqualTo(InquiryType.BUG);
        }
    }

    @Nested
    @DisplayName("replyInquiry")
    class ReplyInquiry {

        @Test
        @DisplayName("should reply to inquiry and send Kafka event")
        void replyInquiry_success() {
            Inquiry inquiry = Inquiry.builder()
                    .id(1L).user(user).type(InquiryType.BUG).title("버그").content("내용").build();
            InquiryReplyRequest request = new InquiryReplyRequest("답변 내용입니다.");
            InquiryDetailResponse response = new InquiryDetailResponse(
                    1L, InquiryType.BUG, "버그", "내용", InquiryStatus.ANSWERED,
                    Collections.emptyList(), List.of(new InquiryReplyResponse(1L, "admin", "답변 내용입니다.", null)),
                    null);

            when(inquiryRepository.findByIdWithUser(1L)).thenReturn(Optional.of(inquiry));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(inquiryReplyRepository.save(any(InquiryReply.class)))
                    .thenReturn(InquiryReply.builder().id(1L).build());
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.INQUIRY, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(InquiryReply.builder().id(1L).build()));
            when(inquiryMapper.toReplyResponseList(anyList()))
                    .thenReturn(List.of(new InquiryReplyResponse(1L, "admin", "답변 내용입니다.", null)));
            when(inquiryMapper.toDetailResponse(eq(inquiry), anyList(), anyList())).thenReturn(response);

            InquiryDetailResponse result = adminService.replyInquiry(1L, 1L, request);

            assertThat(result.status()).isEqualTo(InquiryStatus.ANSWERED);
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
            verify(kafkaProducerService).send(
                    eq(Constants.KAFKA_TOPIC_NOTIFICATION_INQUIRY_REPLIED), anyString(), any());
        }

        @Test
        @DisplayName("should throw exception when inquiry not found")
        void replyInquiry_notFound() {
            InquiryReplyRequest request = new InquiryReplyRequest("답변");
            when(inquiryRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.replyInquiry(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getRequests")
    class GetRequests {

        @Test
        @DisplayName("should return paginated request list")
        void getRequests_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            Request requestEntity = Request.builder()
                    .id(1L).user(user).type(RequestType.TEAM).name("수원 삼성").reason("추가 요청").build();
            AdminRequestListResponse response = new AdminRequestListResponse(
                    1L, "testuser", RequestType.TEAM, "수원 삼성", "추가 요청", RequestStatus.PENDING, null);

            when(requestRepository.findRequestsFiltered(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(requestEntity));
            when(requestMapper.toAdminListResponse(requestEntity)).thenReturn(response);

            CursorPageResponse<AdminRequestListResponse> result = adminService.getRequests(pageRequest, null, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).type()).isEqualTo(RequestType.TEAM);
        }
    }

    @Nested
    @DisplayName("processRequest")
    class ProcessRequest {

        @Test
        @DisplayName("should approve SPORT request and auto-create sport")
        void processRequest_approveSport() {
            Request requestEntity = Request.builder()
                    .id(1L).user(user).type(RequestType.SPORT).name("배드민턴").reason("추가 요청").build();
            RequestProcessRequest request = new RequestProcessRequest(RequestStatus.APPROVED, null, null, null);
            RequestDetailResponse response = new RequestDetailResponse(
                    1L, RequestType.SPORT, "배드민턴", "추가 요청", RequestStatus.APPROVED, null, null, null);

            when(requestRepository.findById(1L)).thenReturn(Optional.of(requestEntity));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(requestMapper.toDetailResponse(requestEntity)).thenReturn(response);

            RequestDetailResponse result = adminService.processRequest(1L, 1L, request);

            assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
            verify(sportRepository).save(any(Sport.class));
        }

        @Test
        @DisplayName("should reject request with reason")
        void processRequest_reject() {
            Request requestEntity = Request.builder()
                    .id(1L).user(user).type(RequestType.TEAM).name("수원 삼성").reason("추가 요청").build();
            RequestProcessRequest request = new RequestProcessRequest(RequestStatus.REJECTED, "K리그1만 지원", null, null);
            RequestDetailResponse response = new RequestDetailResponse(
                    1L, RequestType.TEAM, "수원 삼성", "추가 요청", RequestStatus.REJECTED, "K리그1만 지원", null, null);

            when(requestRepository.findById(1L)).thenReturn(Optional.of(requestEntity));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(requestMapper.toDetailResponse(requestEntity)).thenReturn(response);

            RequestDetailResponse result = adminService.processRequest(1L, 1L, request);

            assertThat(requestEntity.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(requestEntity.getRejectReason()).isEqualTo("K리그1만 지원");
        }

        @Test
        @DisplayName("should throw exception when request not found")
        void processRequest_notFound() {
            RequestProcessRequest request = new RequestProcessRequest(RequestStatus.APPROVED, null, null, null);
            when(requestRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> adminService.processRequest(999L, 1L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("should approve TEAM request and auto-create football team with boards")
        void processRequest_approveTeam_football() {
            Request requestEntity = Request.builder()
                    .id(2L).user(user).type(RequestType.TEAM).name("수원 삼성").reason("팀 추가 요청").build();
            RequestProcessRequest request = new RequestProcessRequest(RequestStatus.APPROVED, null, 1L, 10L);
            RequestDetailResponse response = new RequestDetailResponse(
                    2L, RequestType.TEAM, "수원 삼성", "팀 추가 요청", RequestStatus.APPROVED, null, null, null);

            Sport football = Sport.builder().id(1L).nameKo("축구").nameEn("Football").build();
            Country country = Country.builder().id(1L).nameKo("영국").nameEn("England").build();
            FootballLeague league = FootballLeague.builder().id(10L).country(country).nameKo("프리미어리그").nameEn("Premier League").build();

            when(requestRepository.findById(2L)).thenReturn(Optional.of(requestEntity));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(sportRepository.findById(1L)).thenReturn(Optional.of(football));
            when(footballLeagueRepository.findById(10L)).thenReturn(Optional.of(league));
            when(footballTeamRepository.save(any(FootballTeam.class))).thenAnswer(inv -> inv.getArgument(0));
            when(footballBoardRepository.save(any(FootballBoard.class))).thenAnswer(inv -> inv.getArgument(0));
            when(activeFootballBoardRepository.save(any(ActiveFootballBoard.class))).thenAnswer(inv -> inv.getArgument(0));
            when(requestMapper.toDetailResponse(requestEntity)).thenReturn(response);

            RequestDetailResponse result = adminService.processRequest(2L, 1L, request);

            assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
            verify(footballTeamRepository).save(any(FootballTeam.class));
            verify(footballBoardRepository, times(2)).save(any(FootballBoard.class));
            verify(activeFootballBoardRepository).save(any(ActiveFootballBoard.class));
        }
    }
}
