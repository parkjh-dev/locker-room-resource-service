package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.NoticeMapper;
import com.lockerroom.resourceservice.model.entity.Notice;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NoticeScope;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.NoticeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeMapper noticeMapper;

    @InjectMocks private NoticeServiceImpl noticeService;

    private User admin;
    private Notice notice;
    private Notice pinnedNotice;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .nickname("admin")
                .role(Role.ADMIN)
                .build();

        notice = Notice.builder()
                .id(1L)
                .title("Notice Title")
                .content("Notice Content")
                .scope(NoticeScope.ALL)
                .admin(admin)
                .build();

        pinnedNotice = Notice.builder()
                .id(2L)
                .title("Pinned Notice")
                .content("Pinned Content")
                .isPinned(true)
                .scope(NoticeScope.ALL)
                .admin(admin)
                .build();
    }

    @Nested
    @DisplayName("getList")
    class GetList {

        @Test
        @DisplayName("should return paginated notices with hasNext=true")
        void getList_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            NoticeListResponse response1 = new NoticeListResponse(
                    2L, "Pinned Notice", true, NoticeScope.ALL, null, null
            );

            when(noticeRepository.findFilteredNotices(any(), any(PageRequest.class)))
                    .thenReturn(List.of(pinnedNotice, notice));
            when(noticeMapper.toListResponse(pinnedNotice)).thenReturn(response1);

            CursorPageResponse<NoticeListResponse> result = noticeService.getList(null, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo("2");
        }

        @Test
        @DisplayName("should return paginated notices with hasNext=false")
        void getList_noNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            NoticeListResponse response1 = new NoticeListResponse(
                    2L, "Pinned Notice", true, NoticeScope.ALL, null, null
            );
            NoticeListResponse response2 = new NoticeListResponse(
                    1L, "Notice Title", false, NoticeScope.ALL, null, null
            );

            when(noticeRepository.findFilteredNotices(any(), any(PageRequest.class)))
                    .thenReturn(List.of(pinnedNotice, notice));
            when(noticeMapper.toListResponse(pinnedNotice)).thenReturn(response1);
            when(noticeMapper.toListResponse(notice)).thenReturn(response2);

            CursorPageResponse<NoticeListResponse> result = noticeService.getList(null, pageRequest);

            assertThat(result.getItems()).hasSize(2);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return empty list when no notices")
        void getList_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(noticeRepository.findFilteredNotices(any(), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            CursorPageResponse<NoticeListResponse> result = noticeService.getList(null, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("should return notice detail successfully")
        void getDetail_success() {
            NoticeDetailResponse expectedResponse = new NoticeDetailResponse(
                    1L, "Notice Title", "Notice Content",
                    false, NoticeScope.ALL, null, null,
                    "admin", null, null
            );

            when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
            when(noticeMapper.toDetailResponse(notice)).thenReturn(expectedResponse);

            NoticeDetailResponse result = noticeService.getDetail(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Notice Title");
            assertThat(result.content()).isEqualTo("Notice Content");
        }

        @Test
        @DisplayName("should throw exception when notice not found")
        void getDetail_notFound_throwsException() {
            when(noticeRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> noticeService.getDetail(999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when notice is deleted")
        void getDetail_deleted_throwsException() {
            notice.softDelete();
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

            CustomException exception = assertThrows(CustomException.class,
                    () -> noticeService.getDetail(1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        }
    }
}
