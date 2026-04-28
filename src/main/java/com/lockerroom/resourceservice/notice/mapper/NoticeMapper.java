package com.lockerroom.resourceservice.notice.mapper;

import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.notice.model.entity.Notice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    @Mapping(source = "pinned", target = "isPinned")
    NoticeListResponse toListResponse(Notice notice);

    @Mapping(source = "admin.nickname", target = "adminNickname")
    @Mapping(source = "pinned", target = "isPinned")
    NoticeDetailResponse toDetailResponse(Notice notice);
}
