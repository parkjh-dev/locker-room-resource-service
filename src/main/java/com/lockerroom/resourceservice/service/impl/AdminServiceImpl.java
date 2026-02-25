package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.*;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.kafka.event.NotificationEvent;
import com.lockerroom.resourceservice.mapper.*;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.*;
import com.lockerroom.resourceservice.repository.*;
import com.lockerroom.resourceservice.service.AdminService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final TeamRepository teamRepository;
    private final SportRepository sportRepository;
    private final BoardRepository boardRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final RequestRepository requestRepository;
    private final FileRepository fileRepository;
    private final KafkaProducerService kafkaProducerService;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final NoticeMapper noticeMapper;
    private final InquiryMapper inquiryMapper;
    private final RequestMapper requestMapper;
    private final FileMapper fileMapper;

    @Override
    public CursorPageResponse<AdminUserListResponse> getUsers(CursorPageRequest pageRequest, String keyword, Role role) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<User> users = userRepository.findUsersFiltered(keyword, role, cursorId, pageable);

        return buildCursorPage(users, pageRequest.getSize(), u -> {
            boolean isSuspended = userSuspensionRepository
                    .findActiveByUserId(u.getId(), LocalDateTime.now())
                    .isPresent();
            return userMapper.toAdminListResponse(u, isSuspended);
        }, User::getId);
    }

    @Override
    @Transactional
    public void suspendUser(Long userId, Long adminId, SuspendRequest request) {
        User user = findUserById(userId);
        User admin = findUserById(adminId);

        UserSuspension suspension = UserSuspension.builder()
                .user(user)
                .reason(request.reason())
                .suspendedAt(LocalDateTime.now())
                .suspendedUntil(request.suspendedUntil())
                .admin(admin)
                .build();

        userSuspensionRepository.save(suspension);
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
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        User admin = findUserById(adminId);
        report.process(request.status(), admin);

        if (request.action() != null) {
            handleReportAction(request.action(), report, admin, request.suspensionDays());
        }

        kafkaProducerService.send(
                Constants.KAFKA_TOPIC_NOTIFICATION_REPORT_PROCESSED,
                String.valueOf(report.getUser().getId()),
                new NotificationEvent(
                        report.getUser().getId(),
                        admin.getId(),
                        admin.getNickname(),
                        NotificationType.REPORT_PROCESSED,
                        report.getId(),
                        "신고가 처리되었습니다.",
                        LocalDateTime.now()
                )
        );
    }

    @Override
    @Transactional
    public NoticeDetailResponse createNotice(Long adminId, NoticeCreateRequest request) {
        User admin = findUserById(adminId);
        Team team = null;

        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .isPinned(request.isPinned())
                .scope(request.scope())
                .team(team)
                .admin(admin)
                .build();

        Notice saved = noticeRepository.save(notice);
        return noticeMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, Long adminId, NoticeCreateRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notice.updateTitle(request.title());
        notice.updateContent(request.content());
        notice.updateIsPinned(request.isPinned());
        notice.updateScope(request.scope());

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
            notice.updateTeam(team);
        } else {
            notice.updateTeam(null);
        }

        return noticeMapper.toDetailResponse(notice);
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(n -> !n.isDeleted())
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
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));

        User admin = findUserById(adminId);

        InquiryReply reply = InquiryReply.builder()
                .inquiry(inquiry)
                .admin(admin)
                .content(request.content())
                .build();

        inquiryReplyRepository.save(reply);
        inquiry.updateStatus(InquiryStatus.ANSWERED);

        kafkaProducerService.send(
                Constants.KAFKA_TOPIC_NOTIFICATION_INQUIRY_REPLIED,
                String.valueOf(inquiry.getUser().getId()),
                new NotificationEvent(
                        inquiry.getUser().getId(),
                        admin.getId(),
                        admin.getNickname(),
                        NotificationType.INQUIRY_REPLY,
                        inquiry.getId(),
                        "문의에 답변이 등록되었습니다.",
                        LocalDateTime.now()
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
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.REQUEST_NOT_FOUND));

        User admin = findUserById(adminId);
        entity.process(request.status(), admin, request.rejectReason());

        if (request.status() == RequestStatus.APPROVED) {
            autoCreateFromRequest(entity, request.sportId());
        }

        return requestMapper.toDetailResponse(entity);
    }

    private void autoCreateFromRequest(Request entity, Long sportId) {
        if (entity.getType() == RequestType.SPORT) {
            Sport sport = Sport.builder()
                    .name(entity.getName())
                    .build();
            sportRepository.save(sport);
            log.info("Auto-created Sport '{}' from request #{}", entity.getName(), entity.getId());
        } else if (entity.getType() == RequestType.TEAM) {
            if (sportId == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST,
                        "TEAM 요청 승인 시 sportId가 필요합니다.");
            }

            Sport sport = sportRepository.findById(sportId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SPORT_NOT_FOUND));

            Team team = Team.builder()
                    .sport(sport)
                    .name(entity.getName())
                    .build();
            Team savedTeam = teamRepository.save(team);

            Board teamBoard = Board.builder()
                    .name(entity.getName())
                    .type(BoardType.TEAM)
                    .team(savedTeam)
                    .build();
            boardRepository.save(teamBoard);

            Board newsBoard = Board.builder()
                    .name(entity.getName() + " 뉴스")
                    .type(BoardType.NEWS)
                    .team(savedTeam)
                    .build();
            boardRepository.save(newsBoard);

            log.info("Auto-created Team '{}' with TEAM/NEWS boards from request #{}",
                    entity.getName(), entity.getId());
        }
    }

    private void handleReportAction(String action, PostReport report, User admin, Integer suspensionDays) {
        switch (action.toUpperCase()) {
            case "DELETE_POST" -> report.getPost().softDelete();
            case "SUSPEND_USER" -> {
                int days = (suspensionDays != null) ? suspensionDays : DEFAULT_SUSPENSION_DAYS;
                UserSuspension suspension = UserSuspension.builder()
                        .user(report.getPost().getUser())
                        .admin(admin)
                        .reason("신고 처리: " + report.getReason())
                        .suspendedAt(LocalDateTime.now())
                        .suspendedUntil(LocalDateTime.now().plusDays(days))
                        .build();
                userSuspensionRepository.save(suspension);
                report.getPost().softDelete();
            }
            default -> log.warn("Unknown report action: {}", action);
        }
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

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
