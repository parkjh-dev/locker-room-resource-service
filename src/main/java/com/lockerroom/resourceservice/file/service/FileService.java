package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.model.enums.TargetType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileResponse upload(Long userId, MultipartFile file, TargetType targetType);

    void delete(Long fileId, Long userId);

    List<FileResponse> getFilesByTarget(String targetType, Long targetId);

    void linkFilesToTarget(List<Long> fileIds, TargetType targetType, Long targetId, Long userId);

    void deleteFilesByTarget(TargetType targetType, Long targetId);
}
