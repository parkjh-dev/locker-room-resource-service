package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.model.entity.Notice;
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
