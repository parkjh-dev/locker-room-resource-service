package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.*;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.kafka.KafkaProducerService;
import com.lockerroom.resourceservice.kafka.event.NotificationEvent;
import com.lockerroom.resourceservice.mapper.*;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.*;
import com.lockerroom.resourceservice.service.AdminService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserSuspensionRepository userSuspensionRepository;
    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final NoticeRepository noticeRepository;
    private final TeamRepository teamRepository;
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
    public CursorPageResponse<AdminUserListResponse> getUsers(CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<User> users = (cursorId != null)
                ? userRepository.findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(cursorId, pageable)
                : userRepository.findByDeletedAtIsNullOrderByIdDesc(pageable);

        boolean hasNext = users.size() > pageRequest.getSize();
        List<User> resultUsers = hasNext ? users.subList(0, pageRequest.getSize()) : users;

        List<AdminUserListResponse> items = resultUsers.stream()
                .map(u -> {
                    boolean isSuspended = userSuspensionRepository
                            .findActiveByUserId(u.getId(), LocalDateTime.now())
                            .isPresent();
                    return userMapper.toAdminListResponse(u, isSuspended);
                })
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultUsers.get(resultUsers.size() - 1).getId())
                : null;

        return CursorPageResponse.<AdminUserListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
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
    public CursorPageResponse<ReportListResponse> getReports(CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<PostReport> reports = (cursorId != null)
                ? postReportRepository.findByStatusAndIdLessThanOrderByIdDesc(ReportStatus.PENDING, cursorId, pageable)
                : postReportRepository.findByStatusOrderByIdDesc(ReportStatus.PENDING, pageable);

        boolean hasNext = reports.size() > pageRequest.getSize();
        List<PostReport> resultReports = hasNext ? reports.subList(0, pageRequest.getSize()) : reports;

        List<ReportListResponse> items = resultReports.stream()
                .map(postMapper::toReportListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultReports.get(resultReports.size() - 1).getId())
                : null;

        return CursorPageResponse.<ReportListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    @Transactional
    public void processReport(Long reportId, Long adminId, ReportProcessRequest request) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        User admin = findUserById(adminId);
        report.process(request.status(), admin);

        if ("DELETE".equalsIgnoreCase(request.action())) {
            report.getPost().softDelete();
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
    public CursorPageResponse<AdminInquiryListResponse> getInquiries(CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Inquiry> inquiries = (cursorId != null)
                ? inquiryRepository.findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(cursorId, pageable)
                : inquiryRepository.findByDeletedAtIsNullOrderByIdDesc(pageable);

        boolean hasNext = inquiries.size() > pageRequest.getSize();
        List<Inquiry> resultInquiries = hasNext ? inquiries.subList(0, pageRequest.getSize()) : inquiries;

        List<AdminInquiryListResponse> items = resultInquiries.stream()
                .map(inquiryMapper::toAdminListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultInquiries.get(resultInquiries.size() - 1).getId())
                : null;

        return CursorPageResponse.<AdminInquiryListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
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
    public CursorPageResponse<AdminRequestListResponse> getRequests(CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Request> requests = (cursorId != null)
                ? requestRepository.findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(cursorId, pageable)
                : requestRepository.findByDeletedAtIsNullOrderByIdDesc(pageable);

        boolean hasNext = requests.size() > pageRequest.getSize();
        List<Request> resultRequests = hasNext ? requests.subList(0, pageRequest.getSize()) : requests;

        List<AdminRequestListResponse> items = resultRequests.stream()
                .map(requestMapper::toAdminListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultRequests.get(resultRequests.size() - 1).getId())
                : null;

        return CursorPageResponse.<AdminRequestListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    @Transactional
    public RequestDetailResponse processRequest(Long requestId, Long adminId, RequestProcessRequest request) {
        Request entity = requestRepository.findById(requestId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.REQUEST_NOT_FOUND));

        User admin = findUserById(adminId);
        entity.process(request.status(), admin, request.rejectReason());

        return requestMapper.toDetailResponse(entity);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
