package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.response.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileResponse upload(Long userId, MultipartFile file);

    void delete(Long fileId, Long userId);

    List<FileResponse> getFilesByTarget(String targetType, Long targetId);
}
