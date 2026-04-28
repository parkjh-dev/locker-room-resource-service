package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.AdminInquiryListResponse;
import com.lockerroom.resourceservice.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.dto.response.InquiryListResponse;
import com.lockerroom.resourceservice.dto.response.InquiryReplyResponse;
import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.model.entity.Inquiry;
import com.lockerroom.resourceservice.model.entity.InquiryReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FileMapper.class})
public interface InquiryMapper {

    InquiryListResponse toListResponse(Inquiry inquiry);

    @Mapping(source = "user.nickname", target = "userNickname")
    AdminInquiryListResponse toAdminListResponse(Inquiry inquiry);

    @Mapping(source = "admin.nickname", target = "adminNickname")
    InquiryReplyResponse toReplyResponse(InquiryReply reply);

    List<InquiryReplyResponse> toReplyResponseList(List<InquiryReply> replies);

    default InquiryDetailResponse toDetailResponse(Inquiry inquiry, List<FileResponse> files, List<InquiryReplyResponse> replies) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                files,
                replies,
                inquiry.getCreatedAt()
        );
    }
}
