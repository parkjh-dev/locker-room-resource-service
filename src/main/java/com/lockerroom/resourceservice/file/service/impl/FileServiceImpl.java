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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileMapper fileMapper;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final String bucketName;

    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}
    );
    private static final byte[] RIFF_HEADER = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50};
    private static final int MAX_ORIGINAL_NAME_LENGTH = 200;
    private static final String DEFAULT_FILE_NAME = "file";

    public FileServiceImpl(FileRepository fileRepository,
                           UserRepository userRepository,
                           FileMapper fileMapper,
                           ObjectProvider<S3Client> s3ClientProvider,
                           @Value("${aws.s3.bucket}") String bucketName) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileMapper = fileMapper;
        this.s3ClientProvider = s3ClientProvider;
        this.bucketName = bucketName;
        if (s3ClientProvider.getIfAvailable() == null) {
            log.info("S3Client not available — file upload/delete will use local path only");
        }
    }

    @Override
    @Transactional
    public FileResponse upload(Long userId, MultipartFile file, TargetType targetType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String mimeType = file.getContentType();
        validateMimeType(mimeType);
        validateFileSize(file.getSize(), mimeType);
        validateMagicNumber(file, mimeType);

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + originalName;
        String s3Path = "uploads/" + storedName;

        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client != null) {
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Path)
                        .contentType(mimeType)
                        .contentLength(file.getSize())
                        .contentDisposition("attachment; filename=\"" + originalName + "\"")
                        .serverSideEncryption(ServerSideEncryption.AES256)
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
                .targetType(targetType)
                .targetId(0L)
                .originalName(originalName)
                .storedName(storedName)
                .s3Path(s3Path)
                .size(file.getSize())
                .mimeType(mimeType)
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

        deleteFromS3(file);
        fileRepository.delete(file);
    }

    @Override
    public List<FileResponse> getFilesByTarget(String targetType, Long targetId) {
        TargetType type = TargetType.valueOf(targetType);
        return fileMapper.toResponseList(
                fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(type, targetId));
    }

    @Override
    @Transactional
    public void linkFilesToTarget(List<Long> fileIds, TargetType targetType, Long targetId, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        if (fileIds.size() > Constants.MAX_FILE_COUNT) {
            throw new CustomException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        List<FileEntity> files = fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds);

        for (FileEntity file : files) {
            if (!file.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            file.updateTargetId(targetId);
        }
    }

    @Override
    @Transactional
    public void deleteFilesByTarget(TargetType targetType, Long targetId) {
        List<FileEntity> files = fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(targetType, targetId);
        for (FileEntity file : files) {
            deleteFromS3(file);
        }
        fileRepository.deleteAll(files);
    }

    private void deleteFromS3(FileEntity file) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client != null) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(file.getS3Path())
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("File deleted from S3: {}", file.getS3Path());
        } else {
            log.info("S3 not available — skipping S3 deletion: {}", file.getS3Path());
        }
    }

    private void validateMimeType(String mimeType) {
        if (mimeType == null || !Constants.ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private void validateFileSize(long size, String mimeType) {
        long maxSize = Constants.ALLOWED_IMAGE_TYPES.contains(mimeType)
                ? Constants.MAX_IMAGE_FILE_SIZE
                : Constants.MAX_DOCUMENT_FILE_SIZE;

        if (size > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private void validateMagicNumber(MultipartFile file, String mimeType) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            if (bytesRead < 3) {
                throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }

            if ("text/plain".equals(mimeType)) {
                rejectExecutableHeaders(header, bytesRead);
                return;
            }

            if ("image/webp".equals(mimeType)) {
                if (!startsWith(header, RIFF_HEADER) || !regionMatches(header, 8, WEBP_SIGNATURE)) {
                    throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
                }
                return;
            }

            byte[] expected = MAGIC_NUMBERS.get(mimeType);
            if (expected != null && !startsWith(header, expected)) {
                throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void rejectExecutableHeaders(byte[] header, int bytesRead) {
        // NUL byte in first 8 bytes implies binary, not text.
        for (int i = 0; i < Math.min(bytesRead, 8); i++) {
            if (header[i] == 0x00) {
                throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        }
        // Common executable signatures: ELF (0x7F E L F), Mach-O (0xCFFAEDFE / 0xFEEDFACE...),
        // Windows PE (MZ: 0x4D 0x5A), Java class (0xCAFEBABE).
        if (bytesRead >= 4) {
            if (header[0] == 0x7F && header[1] == 0x45 && header[2] == 0x4C && header[3] == 0x46) {
                throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
            if ((header[0] & 0xFF) == 0xCA && (header[1] & 0xFF) == 0xFE
                    && (header[2] & 0xFF) == 0xBA && (header[3] & 0xFF) == 0xBE) {
                throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        }
        if (bytesRead >= 2 && header[0] == 0x4D && header[1] == 0x5A) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String sanitizeOriginalName(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_FILE_NAME;
        }
        // Strip any directory prefix (e.g. "..\foo/bar.png" → "bar.png") and reject NUL.
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
        String base = (slash >= 0) ? raw.substring(slash + 1) : raw;
        // Drop control chars (0x00-0x1F, 0x7F) and replace path/whitespace separators.
        String cleaned = base.replaceAll("[\\p{Cntrl}]", "")
                .replaceAll("[\\s/\\\\]+", "_");
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) {
            return DEFAULT_FILE_NAME;
        }
        if (cleaned.length() > MAX_ORIGINAL_NAME_LENGTH) {
            cleaned = cleaned.substring(cleaned.length() - MAX_ORIGINAL_NAME_LENGTH);
        }
        return cleaned;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean regionMatches(byte[] data, int offset, byte[] target) {
        if (data.length < offset + target.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (data[offset + i] != target[i]) return false;
        }
        return true;
    }
}
