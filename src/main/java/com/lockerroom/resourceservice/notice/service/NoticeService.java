package com.lockerroom.resourceservice.notice.service;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeListResponse;

public interface NoticeService {

    CursorPageResponse<NoticeListResponse> getList(CursorPageRequest pageRequest);

    NoticeDetailResponse getDetail(Long noticeId);
}
