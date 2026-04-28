package com.lockerroom.resourceservice.request.service;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.request.dto.request.RequestCreateRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestListResponse;

public interface RequestService {

    RequestDetailResponse create(Long userId, RequestCreateRequest request);

    CursorPageResponse<RequestListResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    RequestDetailResponse getDetail(Long requestId, Long userId);
}
