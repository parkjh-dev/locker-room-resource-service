package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.model.entity.FileEntity;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.FileRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.FileService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileMapper fileMapper;
    private final S3Client s3Client;
    private final String bucketName;

    public FileServiceImpl(FileRepository fileRepository,
                           UserRepository userRepository,
                           FileMapper fileMapper,
                           @Autowired(required = false) S3Client s3Client,
                           @Value("${aws.s3.bucket}") String bucketName) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileMapper = fileMapper;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        if (s3Client == null) {
            log.info("S3Client not available — file upload/delete will use local path only");
        }
    }

    @Override
    @Transactional
    public FileResponse upload(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (file.getSize() > Constants.MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String originalName = file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "_" + originalName;
        String s3Path = "uploads/" + storedName;

        if (s3Client != null) {
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Path)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build();
                s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                log.info("File uploaded to S3: {}", s3Path);
            } catch (IOException e) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        } else {
            log.info("S3 not available — file recorded with path only: {}", s3Path);
        }

        FileEntity fileEntity = FileEntity.builder()
                .user(user)
                .targetType(TargetType.POST)
                .targetId(0L)
                .originalName(originalName)
                .storedName(storedName)
                .s3Path(s3Path)
                .size(file.getSize())
                .mimeType(file.getContentType())
                .build();

        FileEntity saved = fileRepository.save(fileEntity);

        return fileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long fileId, Long userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        if (!file.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (s3Client != null) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(file.getS3Path())
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("File deleted from S3: {}", file.getS3Path());
        } else {
            log.info("S3 not available — file soft-deleted without S3 removal: {}", file.getS3Path());
        }

        file.softDelete();
    }

    @Override
    public List<FileResponse> getFilesByTarget(String targetType, Long targetId) {
        TargetType type = TargetType.valueOf(targetType);
        return fileMapper.toResponseList(
                fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(type, targetId));
    }
}
