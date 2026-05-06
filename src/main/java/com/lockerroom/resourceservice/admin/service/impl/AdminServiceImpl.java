package com.lockerroom.resourceservice.admin.service.impl;

import com.lockerroom.resourceservice.admin.dto.response.AdminDashboardResponse;

import com.lockerroom.resourceservice.admin.dto.request.ReportProcessRequest;

import com.lockerroom.resourceservice.admin.dto.request.SuspendRequest;

import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;

import com.lockerroom.resourceservice.post.mapper.PostMapper;

import com.lockerroom.resourceservice.post.repository.PostReportRepository;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;

import com.lockerroom.resourceservice.post.model.entity.PostReport;

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

import com.lockerroom.resourceservice.board.model.entity.ActiveFootballBoard;

import com.lockerroom.resourceservice.board.model.entity.ActiveBaseballBoard;

import com.lockerroom.resourceservice.board.model.entity.FootballBoard;

import com.lockerroom.resourceservice.board.model.entity.BaseballBoard;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository;

import com.lockerroom.resourceservice.sport.repository.SportRepository;

import com.lockerroom.resourceservice.sport.model.enums.SportBoardType;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;

import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;

import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;

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

import com.lockerroom.resourceservice.file.dto.response.FileResponse;

import com.lockerroom.resourceservice.file.mapper.FileMapper;

import com.lockerroom.resourceservice.file.repository.FileRepository;

import com.lockerroom.resourceservice.file.model.enums.TargetType;

import com.lockerroom.resourceservice.file.model.entity.FileEntity;

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
import com.lockerroom.resourceservice.admin.event.InquiryRepliedEvent;
import com.lockerroom.resourceservice.admin.event.ReportProcessedEvent;
import com.lockerroom.resourceservice.admin.service.AdminService;
import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import com.lockerroom.resourceservice.post.model.enums.ReportAction;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int DEFAULT_SUSPENSION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserSuspensionRepository userSuspensionRepository;
    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final NoticeRepository noticeRepository;
    private final SportRepository sportRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final RequestRepository requestRepository;
    private final FileRepository fileRepository;
    private final FootballLeagueRepository footballLeagueRepository;
    private final FootballTeamRepository footballTeamRepository;
    private final FootballBoardRepository footballBoardRepository;
    private final ActiveFootballBoardRepository activeFootballBoardRepository;
    private final BaseballLeagueRepository baseballLeagueRepository;
    private final BaseballTeamRepository baseballTeamRepository;
    private final BaseballBoardRepository baseballBoardRepository;
    private final ActiveBaseballBoardRepository activeBaseballBoardRepository;
    private final KafkaProducerService kafkaProducerService;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final NoticeMapper noticeMapper;
    private final InquiryMapper inquiryMapper;
    private final RequestMapper requestMapper;
    private final FileMapper fileMapper;

    @Override
    public AdminDashboardResponse getDashboard() {
        long pendingReportCount = postReportRepository.countByStatusAndDeletedAtIsNull(ReportStatus.PENDING);
        long pendingInquiryCount = inquiryRepository.countByStatusAndDeletedAtIsNull(InquiryStatus.PENDING);
        long pendingRequestCount = requestRepository.countByStatusAndDeletedAtIsNull(RequestStatus.PENDING);
        return new AdminDashboardResponse(pendingReportCount, pendingInquiryCount, pendingRequestCount);
    }

    @Override
    public CursorPageResponse<AdminUserListResponse> getUsers(CursorPageRequest pageRequest, String keyword, Role role) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<User> users = userRepository.findUsersFiltered(keyword, role, cursorId, pageable);

        List<Long> userIds = users.stream().map(User::getId).toList();
        Set<Long> suspendedUserIds = userIds.isEmpty()
                ? Set.of()
                : new HashSet<>(userSuspensionRepository.findActiveSuspendedUserIds(userIds, OffsetDateTime.now()));

        return buildCursorPage(users, pageRequest.getSize(),
                u -> userMapper.toAdminListResponse(u, suspendedUserIds.contains(u.getId())),
                User::getId);
    }

    @Override
    @Transactional
    public void suspendUser(Long userId, Long adminId, SuspendRequest request) {
        User user = findUserById(userId);
        User admin = findUserById(adminId);

        UserSuspension suspension = UserSuspension.builder()
                .user(user)
                .reason(request.reason())
                .suspendedAt(OffsetDateTime.now())
                .suspendedUntil(request.suspendedUntil())
                .admin(admin)
                .build();

        userSuspensionRepository.save(suspension);
    }

    @Override
    @Transactional
    public void unsuspendUser(Long userId, Long adminId) {
        findUserById(userId);
        User admin = findUserById(adminId);

        UserSuspension suspension = userSuspensionRepository
                .findActiveByUserId(userId, OffsetDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.SUSPENSION_NOT_FOUND));

        suspension.softDelete();
        log.info("Suspension #{} on user #{} unsuspended by admin #{}({})",
                suspension.getId(), userId, admin.getId(), admin.getNickname());
    }

    @Override
    public CursorPageResponse<ReportListResponse> getReports(CursorPageRequest pageRequest, ReportStatus status) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<PostReport> reports = postReportRepository.findReportsFiltered(status, cursorId, pageable);

        return buildCursorPage(reports, pageRequest.getSize(), postMapper::toReportListResponse, PostReport::getId);
    }

    @Override
    @Transactional
    public void processReport(Long reportId, Long adminId, ReportProcessRequest request) {
        PostReport report = postReportRepository.findByIdWithPostAndUser(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        User admin = findUserById(adminId);
        report.process(request.status(), admin);

        if (request.action() != null) {
            handleReportAction(request.action(), report, admin, request.suspensionDays());
        }

        kafkaProducerService.send(
                Constants.KAFKA_TOPIC_NOTIFICATION_REPORT_PROCESSED,
                String.valueOf(report.getUser().getId()),
                new ReportProcessedEvent(
                        UUID.randomUUID().toString(),
                        report.getUser().getId(),
                        report.getId(),
                        request.status().name()
                )
        );
    }

    @Override
    @Transactional
    public NoticeDetailResponse createNotice(Long adminId, NoticeCreateRequest request) {
        User admin = findUserById(adminId);

        com.lockerroom.resourceservice.notice.model.enums.NoticeScope scope =
                (request.scope() != null) ? request.scope()
                        : com.lockerroom.resourceservice.notice.model.enums.NoticeScope.ALL;
        String teamName = (scope == com.lockerroom.resourceservice.notice.model.enums.NoticeScope.TEAM)
                ? resolveTeamName(request.teamId()) : null;

        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .isPinned(request.isPinned())
                .scope(scope)
                .teamId(scope == com.lockerroom.resourceservice.notice.model.enums.NoticeScope.TEAM
                        ? request.teamId() : null)
                .teamName(teamName)
                .admin(admin)
                .build();

        Notice saved = noticeRepository.save(notice);
        return noticeMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, Long adminId, NoticeCreateRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notice.updateTitle(request.title());
        notice.updateContent(request.content());
        notice.updateIsPinned(request.isPinned());

        com.lockerroom.resourceservice.notice.model.enums.NoticeScope scope =
                (request.scope() != null) ? request.scope()
                        : com.lockerroom.resourceservice.notice.model.enums.NoticeScope.ALL;
        String teamName = (scope == com.lockerroom.resourceservice.notice.model.enums.NoticeScope.TEAM)
                ? resolveTeamName(request.teamId()) : null;
        notice.updateScope(scope, request.teamId(), teamName);

        return noticeMapper.toDetailResponse(notice);
    }

    /**
     * teamId로 Football/Baseball 팀 조회해 teamName 반환. 매칭 없으면 null.
     */
    private String resolveTeamName(Long teamId) {
        if (teamId == null) return null;
        return footballTeamRepository.findById(teamId)
                .map(com.lockerroom.resourceservice.sport.model.entity.FootballTeam::getNameKo)
                .orElseGet(() -> baseballTeamRepository.findById(teamId)
                        .map(com.lockerroom.resourceservice.sport.model.entity.BaseballTeam::getNameKo)
                        .orElse(null));
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notice.softDelete();
    }

    @Override
    public CursorPageResponse<AdminInquiryListResponse> getInquiries(
            CursorPageRequest pageRequest, InquiryStatus status, InquiryType type) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Inquiry> inquiries = inquiryRepository.findInquiriesFiltered(status, type, cursorId, pageable);

        return buildCursorPage(inquiries, pageRequest.getSize(), inquiryMapper::toAdminListResponse, Inquiry::getId);
    }

    @Override
    @Transactional
    public InquiryDetailResponse replyInquiry(Long inquiryId, Long adminId, InquiryReplyRequest request) {
        Inquiry inquiry = inquiryRepository.findByIdWithUser(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));

        User admin = findUserById(adminId);

        InquiryReply reply = InquiryReply.builder()
                .inquiry(inquiry)
                .admin(admin)
                .content(request.content())
                .build();

        InquiryReply savedReply = inquiryReplyRepository.save(reply);
        inquiry.updateStatus(InquiryStatus.ANSWERED);

        kafkaProducerService.send(
                Constants.KAFKA_TOPIC_NOTIFICATION_INQUIRY_REPLIED,
                String.valueOf(inquiry.getUser().getId()),
                new InquiryRepliedEvent(
                        UUID.randomUUID().toString(),
                        inquiry.getUser().getId(),
                        inquiry.getId(),
                        savedReply.getId()
                )
        );

        List<FileEntity> files = fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(
                TargetType.INQUIRY, inquiry.getId());
        List<FileResponse> fileResponses = fileMapper.toResponseList(files);

        List<InquiryReply> allReplies = inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(inquiryId);
        List<InquiryReplyResponse> replyResponses = inquiryMapper.toReplyResponseList(allReplies);

        return inquiryMapper.toDetailResponse(inquiry, fileResponses, replyResponses);
    }

    @Override
    public CursorPageResponse<AdminRequestListResponse> getRequests(
            CursorPageRequest pageRequest, RequestStatus status, RequestType type) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Request> requests = requestRepository.findRequestsFiltered(status, type, cursorId, pageable);

        return buildCursorPage(requests, pageRequest.getSize(), requestMapper::toAdminListResponse, Request::getId);
    }

    @Override
    @Transactional
    public RequestDetailResponse processRequest(Long requestId, Long adminId, RequestProcessRequest request) {
        Request entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.REQUEST_NOT_FOUND));

        User admin = findUserById(adminId);
        entity.process(request.status(), admin, request.rejectReason());

        if (request.status() == RequestStatus.APPROVED) {
            autoCreateFromRequest(entity, request.sportId(), request.leagueId());
        }

        return requestMapper.toDetailResponse(entity);
    }

    private void autoCreateFromRequest(Request entity, Long sportId, Long leagueId) {
        if (entity.getType() == RequestType.SPORT) {
            Sport sport = Sport.builder()
                    .nameKo(entity.getName())
                    .nameEn(entity.getName())
                    .build();
            sportRepository.save(sport);
            log.info("Auto-created Sport '{}' from request #{}", entity.getName(), entity.getId());
        } else if (entity.getType() == RequestType.TEAM) {
            if (sportId == null || leagueId == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
            Sport sport = sportRepository.findById(sportId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SPORT_NOT_FOUND));

            String sportNameEn = sport.getNameEn().toLowerCase();
            if (sportNameEn.contains("football") || sportNameEn.contains("soccer")) {
                createFootballTeam(entity, leagueId);
            } else if (sportNameEn.contains("baseball")) {
                createBaseballTeam(entity, leagueId);
            } else {
                log.warn("Unsupported sport '{}' for TEAM auto-creation, request #{}", sport.getNameEn(), entity.getId());
            }
        }
    }

    private void createFootballTeam(Request entity, Long leagueId) {
        FootballLeague league = footballLeagueRepository.findById(leagueId)
                .orElseThrow(() -> new CustomException(ErrorCode.LEAGUE_NOT_FOUND));

        FootballTeam team = FootballTeam.builder()
                .league(league)
                .nameKo(entity.getName())
                .nameEn(entity.getName())
                .build();
        footballTeamRepository.save(team);

        FootballBoard teamBoard = FootballBoard.builder()
                .footballTeam(team)
                .name(entity.getName() + " 게시판")
                .type(SportBoardType.TEAM)
                .build();
        footballBoardRepository.save(teamBoard);

        FootballBoard newsBoard = FootballBoard.builder()
                .footballTeam(team)
                .name(entity.getName() + " 뉴스")
                .type(SportBoardType.NEWS)
                .build();
        footballBoardRepository.save(newsBoard);

        ActiveFootballBoard activeBoard = ActiveFootballBoard.builder()
                .footballTeam(team)
                .board(teamBoard)
                .build();
        activeFootballBoardRepository.save(activeBoard);

        log.info("Auto-created FootballTeam '{}' with boards from request #{}", entity.getName(), entity.getId());
    }

    private void createBaseballTeam(Request entity, Long leagueId) {
        BaseballLeague league = baseballLeagueRepository.findById(leagueId)
                .orElseThrow(() -> new CustomException(ErrorCode.LEAGUE_NOT_FOUND));

        BaseballTeam team = BaseballTeam.builder()
                .league(league)
                .nameKo(entity.getName())
                .nameEn(entity.getName())
                .build();
        baseballTeamRepository.save(team);

        BaseballBoard teamBoard = BaseballBoard.builder()
                .baseballTeam(team)
                .name(entity.getName() + " 게시판")
                .type(SportBoardType.TEAM)
                .build();
        baseballBoardRepository.save(teamBoard);

        BaseballBoard newsBoard = BaseballBoard.builder()
                .baseballTeam(team)
                .name(entity.getName() + " 뉴스")
                .type(SportBoardType.NEWS)
                .build();
        baseballBoardRepository.save(newsBoard);

        ActiveBaseballBoard activeBoard = ActiveBaseballBoard.builder()
                .baseballTeam(team)
                .board(teamBoard)
                .build();
        activeBaseballBoardRepository.save(activeBoard);

        log.info("Auto-created BaseballTeam '{}' with boards from request #{}", entity.getName(), entity.getId());
    }

    private void handleReportAction(ReportAction action, PostReport report, User admin, Integer suspensionDays) {
        switch (action) {
            case DELETE_POST -> report.getPost().softDelete();
            case SUSPEND_USER -> {
                int days = (suspensionDays != null) ? suspensionDays : DEFAULT_SUSPENSION_DAYS;
                UserSuspension suspension = UserSuspension.builder()
                        .user(report.getPost().getUser())
                        .admin(admin)
                        .reason("신고 처리: " + report.getReason())
                        .suspendedAt(OffsetDateTime.now())
                        .suspendedUntil(OffsetDateTime.now().plusDays(days))
                        .build();
                userSuspensionRepository.save(suspension);
                report.getPost().softDelete();
            }
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private <E, R> CursorPageResponse<R> buildCursorPage(
            List<E> items, int size,
            java.util.function.Function<E, R> mapper,
            java.util.function.Function<E, Long> idExtractor) {
        boolean hasNext = items.size() > size;
        List<E> resultItems = hasNext ? items.subList(0, size) : items;

        List<R> mapped = resultItems.stream().map(mapper).toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(idExtractor.apply(resultItems.get(resultItems.size() - 1)))
                : null;

        return CursorPageResponse.<R>builder()
                .items(mapped)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

}
