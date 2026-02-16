package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.RequestCreateRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.dto.response.RequestListResponse;

public interface RequestService {

    RequestDetailResponse create(Long userId, RequestCreateRequest request);

    CursorPageResponse<RequestListResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    RequestDetailResponse getDetail(Long requestId, Long userId);
}
