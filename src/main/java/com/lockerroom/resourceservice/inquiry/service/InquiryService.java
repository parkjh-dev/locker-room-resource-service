package com.lockerroom.resourceservice.inquiry.service;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.inquiry.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.inquiry.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.inquiry.dto.response.InquiryListResponse;

public interface InquiryService {

    InquiryDetailResponse create(Long userId, InquiryCreateRequest request);

    CursorPageResponse<InquiryListResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    InquiryDetailResponse getDetail(Long inquiryId, Long userId);
}
