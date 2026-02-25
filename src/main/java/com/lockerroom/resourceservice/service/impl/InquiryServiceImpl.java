package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.mapper.InquiryMapper;
import com.lockerroom.resourceservice.model.entity.FileEntity;
import com.lockerroom.resourceservice.model.entity.Inquiry;
import com.lockerroom.resourceservice.model.entity.InquiryReply;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.FileRepository;
import com.lockerroom.resourceservice.repository.InquiryReplyRepository;
import com.lockerroom.resourceservice.repository.InquiryRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.FileService;
import com.lockerroom.resourceservice.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final InquiryMapper inquiryMapper;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public InquiryDetailResponse create(Long userId, InquiryCreateRequest request) {
        User user = findUserById(userId);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);

        fileService.linkFilesToTarget(request.fileIds(), TargetType.INQUIRY, saved.getId(), userId);

        return toDetailResponse(saved);
    }

    @Override
    public CursorPageResponse<InquiryListResponse> getMyList(Long userId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Inquiry> inquiries = (cursorId != null)
                ? inquiryRepository.findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(userId, cursorId, pageable)
                : inquiryRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(userId, pageable);

        boolean hasNext = inquiries.size() > pageRequest.getSize();
        List<Inquiry> resultInquiries = hasNext ? inquiries.subList(0, pageRequest.getSize()) : inquiries;

        List<InquiryListResponse> items = resultInquiries.stream()
                .map(inquiryMapper::toListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultInquiries.get(resultInquiries.size() - 1).getId())
                : null;

        return CursorPageResponse.<InquiryListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public InquiryDetailResponse getDetail(Long inquiryId, Long userId) {
        Inquiry inquiry = findInquiryById(inquiryId);

        if (!inquiry.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED);
        }

        return toDetailResponse(inquiry);
    }

    private InquiryDetailResponse toDetailResponse(Inquiry inquiry) {
        List<FileEntity> files = fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(
                TargetType.INQUIRY, inquiry.getId());
        List<FileResponse> fileResponses = fileMapper.toResponseList(files);

        List<InquiryReply> replies = inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(inquiry.getId());
        List<InquiryReplyResponse> replyResponses = inquiryMapper.toReplyResponseList(replies);

        return inquiryMapper.toDetailResponse(inquiry, fileResponses, replyResponses);
    }

    private Inquiry findInquiryById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
