package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.response.FileResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.model.entity.FileEntity;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.FileRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private FileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileMapper fileMapper;
    @Mock private MultipartFile multipartFile;

    private FileServiceImpl fileService;

    private User user;
    private User otherUser;

    @SuppressWarnings("unchecked")
    private static ObjectProvider<S3Client> providerOf(S3Client client) {
        ObjectProvider<S3Client> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    @BeforeEach
    void setUp() {
        // S3Client null → local fallback mode
        fileService = new FileServiceImpl(fileRepository, userRepository, fileMapper,
                providerOf(null), "test-bucket");

        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .nickname("otheruser")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("should upload file successfully without S3")
        void upload_success_noS3() throws Exception {
            FileEntity savedFile = FileEntity.builder()
                    .id(1L)
                    .user(user)
                    .targetType(TargetType.POST)
                    .targetId(0L)
                    .originalName("test.png")
                    .storedName("uuid_test.png")
                    .s3Path("uploads/uuid_test.png")
                    .size(1024L)
                    .mimeType("image/png")
                    .build();
            FileResponse response = new FileResponse(1L, "test.png", "uploads/uuid_test.png", 1024L, "image/png");

            // PNG magic bytes: 89 50 4E 47
            byte[] pngMagic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getOriginalFilename()).thenReturn("test.png");
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(pngMagic));
            when(fileRepository.save(any(FileEntity.class))).thenReturn(savedFile);
            when(fileMapper.toResponse(savedFile)).thenReturn(response);

            FileResponse result = fileService.upload(1L, multipartFile, TargetType.POST);

            assertThat(result).isNotNull();
            assertThat(result.originalName()).isEqualTo("test.png");
            assertThat(result.size()).isEqualTo(1024L);
            verify(fileRepository).save(any(FileEntity.class));
        }

        @Test
        @DisplayName("should throw exception when file size exceeds limit")
        void upload_fileSizeExceeded() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getSize()).thenReturn(Constants.MAX_IMAGE_FILE_SIZE + 1);

            CustomException exception = assertThrows(CustomException.class,
                    () -> fileService.upload(1L, multipartFile, TargetType.POST));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void upload_userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> fileService.upload(999L, multipartFile, TargetType.POST));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("upload with S3")
    class UploadWithS3 {

        @Mock private S3Client s3Client;

        @Test
        @DisplayName("should upload file to S3 successfully")
        void upload_withS3_success() throws Exception {
            FileServiceImpl s3FileService = new FileServiceImpl(
                    fileRepository, userRepository, fileMapper, providerOf(s3Client), "test-bucket");

            FileEntity savedFile = FileEntity.builder()
                    .id(1L).user(user).targetType(TargetType.POST).targetId(0L)
                    .originalName("doc.pdf").storedName("uuid_doc.pdf")
                    .s3Path("uploads/uuid_doc.pdf").size(2048L).mimeType("application/pdf")
                    .build();
            FileResponse response = new FileResponse(1L, "doc.pdf", "uploads/uuid_doc.pdf", 2048L, "application/pdf");

            // PDF magic bytes: 25 50 44 46 (%PDF)
            byte[] pdfMagic = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0, 0, 0, 0};

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(multipartFile.getSize()).thenReturn(2048L);
            when(multipartFile.getOriginalFilename()).thenReturn("doc.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream(pdfMagic))   // for magic number validation
                    .thenReturn(new ByteArrayInputStream(pdfMagic));  // for S3 upload
            when(fileRepository.save(any(FileEntity.class))).thenReturn(savedFile);
            when(fileMapper.toResponse(savedFile)).thenReturn(response);

            FileResponse result = s3FileService.upload(1L, multipartFile, TargetType.POST);

            assertThat(result.originalName()).isEqualTo("doc.pdf");
            verify(s3Client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                    any(software.amazon.awssdk.core.sync.RequestBody.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete file successfully without S3")
        void delete_success_noS3() {
            FileEntity file = FileEntity.builder()
                    .id(1L).user(user).targetType(TargetType.POST).targetId(1L)
                    .originalName("test.png").storedName("uuid_test.png")
                    .s3Path("uploads/uuid_test.png").size(1024L).mimeType("image/png")
                    .build();

            when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

            fileService.delete(1L, 1L);

            verify(fileRepository).delete(file);
        }

        @Test
        @DisplayName("should throw exception when file not found")
        void delete_fileNotFound() {
            when(fileRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> fileService.delete(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user is not the uploader")
        void delete_notOwner() {
            FileEntity file = FileEntity.builder()
                    .id(1L).user(user).targetType(TargetType.POST).targetId(1L)
                    .originalName("test.png").storedName("uuid_test.png")
                    .s3Path("uploads/uuid_test.png").size(1024L).mimeType("image/png")
                    .build();

            when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

            CustomException exception = assertThrows(CustomException.class,
                    () -> fileService.delete(1L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("getFilesByTarget")
    class GetFilesByTarget {

        @Test
        @DisplayName("should return files for target")
        void getFilesByTarget_success() {
            FileEntity file = FileEntity.builder()
                    .id(1L).user(user).targetType(TargetType.POST).targetId(1L)
                    .originalName("test.png").storedName("uuid_test.png")
                    .s3Path("uploads/uuid_test.png").size(1024L).mimeType("image/png")
                    .build();
            FileResponse response = new FileResponse(1L, "test.png", "uploads/uuid_test.png", 1024L, "image/png");

            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(List.of(file));
            when(fileMapper.toResponseList(List.of(file))).thenReturn(List.of(response));

            List<FileResponse> result = fileService.getFilesByTarget("POST", 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).originalName()).isEqualTo("test.png");
        }

        @Test
        @DisplayName("should return empty list when no files")
        void getFilesByTarget_empty() {
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.POST, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<FileResponse> result = fileService.getFilesByTarget("POST", 1L);

            assertThat(result).isEmpty();
        }
    }
}
