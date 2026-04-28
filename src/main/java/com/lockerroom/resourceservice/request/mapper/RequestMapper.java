package com.lockerroom.resourceservice.request.mapper;

import com.lockerroom.resourceservice.request.dto.response.AdminRequestListResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestListResponse;
import com.lockerroom.resourceservice.request.model.entity.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestListResponse toListResponse(Request request);

    RequestDetailResponse toDetailResponse(Request request);

    @Mapping(source = "user.nickname", target = "userNickname")
    AdminRequestListResponse toAdminListResponse(Request request);
}
