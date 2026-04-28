package com.lockerroom.resourceservice.sport.mapper;

import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SportMapper {

    @Mapping(source = "active", target = "isActive")
    SportResponse toSportResponse(Sport sport);
}
