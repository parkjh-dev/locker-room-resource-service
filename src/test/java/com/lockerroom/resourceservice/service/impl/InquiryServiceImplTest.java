package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.FileMapper;
import com.lockerroom.resourceservice.mapper.InquiryMapper;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.InquiryType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.FileRepository;
import com.lockerroom.resourceservice.repository.InquiryReplyRepository;
import com.lockerroom.resourceservice.repository.InquiryRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceImplTest {

    @Mock private InquiryRepository inquiryRepository;
    @Mock private InquiryReplyRepository inquiryReplyRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileRepository fileRepository;
    @Mock private InquiryMapper inquiryMapper;
    @Mock private FileMapper fileMapper;

    @InjectMocks private InquiryServiceImpl inquiryService;

    private User user;
    private User otherUser;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
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

        inquiry = Inquiry.builder()
                .id(1L)
                .user(user)
                .type(InquiryType.BUG)
                .title("로그인 오류")
                .content("구글 로그인 시 오류 발생")
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create inquiry successfully")
        void create_success() {
            InquiryCreateRequest request = new InquiryCreateRequest(
                    InquiryType.BUG, "로그인 오류", "구글 로그인 시 오류 발생", null);
            InquiryDetailResponse response = new InquiryDetailResponse(
                    1L, InquiryType.BUG, "로그인 오류", "구글 로그인 시 오류 발생",
                    InquiryStatus.PENDING, Collections.emptyList(), Collections.emptyList(), null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(inquiryRepository.save(any(Inquiry.class))).thenReturn(inquiry);
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.INQUIRY, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(1L))
                    .thenReturn(Collections.emptyList());
            when(inquiryMapper.toReplyResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryMapper.toDetailResponse(eq(inquiry), anyList(), anyList())).thenReturn(response);

            InquiryDetailResponse result = inquiryService.create(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(InquiryType.BUG);
            assertThat(result.title()).isEqualTo("로그인 오류");
            assertThat(result.status()).isEqualTo(InquiryStatus.PENDING);
            verify(inquiryRepository).save(any(Inquiry.class));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void create_userNotFound() {
            InquiryCreateRequest request = new InquiryCreateRequest(
                    InquiryType.GENERAL, "문의", "내용", null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> inquiryService.create(999L, request));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getMyList")
    class GetMyList {

        @Test
        @DisplayName("should return paginated inquiry list")
        void getMyList_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            InquiryListResponse listResponse = new InquiryListResponse(
                    1L, InquiryType.BUG, "로그인 오류", InquiryStatus.PENDING, null);

            when(inquiryRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(inquiry));
            when(inquiryMapper.toListResponse(inquiry)).thenReturn(listResponse);

            CursorPageResponse<InquiryListResponse> result =
                    inquiryService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).type()).isEqualTo(InquiryType.BUG);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("should set hasNext when more results exist")
        void getMyList_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            Inquiry inquiry2 = Inquiry.builder()
                    .id(2L).user(user).type(InquiryType.GENERAL).title("문의2").content("내용2").build();
            InquiryListResponse listResponse = new InquiryListResponse(
                    1L, InquiryType.BUG, "로그인 오류", InquiryStatus.PENDING, null);

            when(inquiryRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(inquiry, inquiry2));
            when(inquiryMapper.toListResponse(inquiry)).thenReturn(listResponse);

            CursorPageResponse<InquiryListResponse> result =
                    inquiryService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(1L));
        }

        @Test
        @DisplayName("should return empty list when no inquiries")
        void getMyList_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            when(inquiryRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(Collections.emptyList());

            CursorPageResponse<InquiryListResponse> result =
                    inquiryService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("should return inquiry detail for owner")
        void getDetail_owner_success() {
            InquiryDetailResponse response = new InquiryDetailResponse(
                    1L, InquiryType.BUG, "로그인 오류", "구글 로그인 시 오류 발생",
                    InquiryStatus.PENDING, Collections.emptyList(), Collections.emptyList(), null);

            when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.INQUIRY, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(1L))
                    .thenReturn(Collections.emptyList());
            when(inquiryMapper.toReplyResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryMapper.toDetailResponse(eq(inquiry), anyList(), anyList())).thenReturn(response);

            InquiryDetailResponse result = inquiryService.getDetail(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("로그인 오류");
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void getDetail_notOwner_throwsException() {
            when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

            CustomException exception = assertThrows(CustomException.class,
                    () -> inquiryService.getDetail(1L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED);
        }

        @Test
        @DisplayName("should throw exception when inquiry not found")
        void getDetail_notFound() {
            when(inquiryRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> inquiryService.getDetail(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when inquiry is deleted")
        void getDetail_deleted() {
            inquiry.softDelete();
            when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

            CustomException exception = assertThrows(CustomException.class,
                    () -> inquiryService.getDetail(1L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
        }

        @Test
        @DisplayName("should return detail with replies when inquiry has replies")
        void getDetail_withReplies() {
            InquiryReply reply = InquiryReply.builder().id(1L).content("답변입니다").build();
            InquiryReplyResponse replyResponse = new InquiryReplyResponse(1L, "admin", "답변입니다", null);
            InquiryDetailResponse response = new InquiryDetailResponse(
                    1L, InquiryType.BUG, "로그인 오류", "구글 로그인 시 오류 발생",
                    InquiryStatus.ANSWERED, Collections.emptyList(), List.of(replyResponse), null);

            when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));
            when(fileRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType.INQUIRY, 1L))
                    .thenReturn(Collections.emptyList());
            when(fileMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());
            when(inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(reply));
            when(inquiryMapper.toReplyResponseList(List.of(reply)))
                    .thenReturn(List.of(replyResponse));
            when(inquiryMapper.toDetailResponse(eq(inquiry), anyList(), anyList())).thenReturn(response);

            InquiryDetailResponse result = inquiryService.getDetail(1L, 1L);

            assertThat(result.replies()).hasSize(1);
            assertThat(result.replies().get(0).content()).isEqualTo("답변입니다");
        }
    }
}
