package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.model.entity.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(source = "s3Path", target = "url")
    FileResponse toResponse(FileEntity file);

    List<FileResponse> toResponseList(List<FileEntity> files);
}
