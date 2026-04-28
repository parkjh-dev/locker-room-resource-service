package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.dto.response.InquiryListResponse;

public interface InquiryService {

    InquiryDetailResponse create(Long userId, InquiryCreateRequest request);

    CursorPageResponse<InquiryListResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    InquiryDetailResponse getDetail(Long inquiryId, Long userId);
}
