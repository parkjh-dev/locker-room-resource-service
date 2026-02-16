package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "read", target = "isRead")
    NotificationResponse toResponse(Notification notification);
}
