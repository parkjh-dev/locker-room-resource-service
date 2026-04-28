package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.AdminRequestListResponse;
import com.lockerroom.resourceservice.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.dto.response.RequestListResponse;
import com.lockerroom.resourceservice.model.entity.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestListResponse toListResponse(Request request);

    RequestDetailResponse toDetailResponse(Request request);

    @Mapping(source = "user.nickname", target = "userNickname")
    AdminRequestListResponse toAdminListResponse(Request request);
}
