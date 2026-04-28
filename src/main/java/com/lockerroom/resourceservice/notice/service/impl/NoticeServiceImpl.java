package com.lockerroom.resourceservice.notice.service.impl;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.notice.mapper.NoticeMapper;
import com.lockerroom.resourceservice.notice.model.entity.Notice;
import com.lockerroom.resourceservice.notice.repository.NoticeRepository;
import com.lockerroom.resourceservice.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;

    @Override
    public CursorPageResponse<NoticeListResponse> getList(CursorPageRequest pageRequest) {
        List<Notice> notices = noticeRepository.findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(
                PageRequest.of(0, pageRequest.getSize() + 1));

        boolean hasNext = notices.size() > pageRequest.getSize();
        List<Notice> resultNotices = hasNext ? notices.subList(0, pageRequest.getSize()) : notices;

        List<NoticeListResponse> items = resultNotices.stream()
                .map(noticeMapper::toListResponse)
                .toList();

        String nextCursor = hasNext ? String.valueOf(resultNotices.get(resultNotices.size() - 1).getId()) : null;

        return CursorPageResponse.<NoticeListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public NoticeDetailResponse getDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        return noticeMapper.toDetailResponse(notice);
    }
}
