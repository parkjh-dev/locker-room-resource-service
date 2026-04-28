package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.dto.response.NoticeListResponse;

public interface NoticeService {

    CursorPageResponse<NoticeListResponse> getList(CursorPageRequest pageRequest);

    NoticeDetailResponse getDetail(Long noticeId);
}
