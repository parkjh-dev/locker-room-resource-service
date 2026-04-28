package com.lockerroom.resourceservice.notification.mapper;

import com.lockerroom.resourceservice.notification.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.notification.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "read", target = "isRead")
    NotificationResponse toResponse(Notification notification);
}
