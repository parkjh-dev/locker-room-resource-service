package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.RequestCreateRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.dto.response.RequestListResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.RequestMapper;
import com.lockerroom.resourceservice.model.entity.Request;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.RequestStatus;
import com.lockerroom.resourceservice.model.enums.RequestType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.repository.RequestRepository;
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
class RequestServiceImplTest {

    @Mock private RequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private RequestMapper requestMapper;

    @InjectMocks private RequestServiceImpl requestService;

    private User user;
    private User otherUser;
    private Request request;

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

        request = Request.builder()
                .id(1L)
                .user(user)
                .type(RequestType.TEAM)
                .name("New Team")
                .reason("We need this team")
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create request successfully")
        void create_success() {
            RequestCreateRequest createRequest = new RequestCreateRequest(
                    RequestType.TEAM, "New Team", "We need this team"
            );
            RequestDetailResponse expectedResponse = new RequestDetailResponse(
                    1L, RequestType.TEAM, "New Team", "We need this team",
                    RequestStatus.PENDING, null, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(requestRepository.save(any(Request.class))).thenReturn(request);
            when(requestMapper.toDetailResponse(request)).thenReturn(expectedResponse);

            RequestDetailResponse result = requestService.create(1L, createRequest);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.type()).isEqualTo(RequestType.TEAM);
            assertThat(result.name()).isEqualTo("New Team");
            assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
            verify(requestRepository).save(any(Request.class));
        }

        @Test
        @DisplayName("should create SPORT type request successfully")
        void create_sportType_success() {
            RequestCreateRequest createRequest = new RequestCreateRequest(
                    RequestType.SPORT, "New Sport", "Please add this sport"
            );
            Request sportRequest = Request.builder()
                    .id(2L)
                    .user(user)
                    .type(RequestType.SPORT)
                    .name("New Sport")
                    .reason("Please add this sport")
                    .build();
            RequestDetailResponse expectedResponse = new RequestDetailResponse(
                    2L, RequestType.SPORT, "New Sport", "Please add this sport",
                    RequestStatus.PENDING, null, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(requestRepository.save(any(Request.class))).thenReturn(sportRequest);
            when(requestMapper.toDetailResponse(sportRequest)).thenReturn(expectedResponse);

            RequestDetailResponse result = requestService.create(1L, createRequest);

            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RequestType.SPORT);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void create_userNotFound_throwsException() {
            RequestCreateRequest createRequest = new RequestCreateRequest(
                    RequestType.TEAM, "Team", "Reason"
            );
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> requestService.create(999L, createRequest));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getMyList")
    class GetMyList {

        @Test
        @DisplayName("should return paginated requests with hasNext=true")
        void getMyList_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            Request request2 = Request.builder()
                    .id(2L)
                    .user(user)
                    .type(RequestType.SPORT)
                    .name("Sport Request")
                    .reason("reason")
                    .build();

            RequestListResponse response1 = new RequestListResponse(
                    1L, RequestType.TEAM, "New Team", RequestStatus.PENDING, null
            );

            when(requestRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(request, request2));
            when(requestMapper.toListResponse(request)).thenReturn(response1);

            CursorPageResponse<RequestListResponse> result = requestService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(1L));
        }

        @Test
        @DisplayName("should return paginated requests with hasNext=false")
        void getMyList_noNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            RequestListResponse response1 = new RequestListResponse(
                    1L, RequestType.TEAM, "New Team", RequestStatus.PENDING, null
            );

            when(requestRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(List.of(request));
            when(requestMapper.toListResponse(request)).thenReturn(response1);

            CursorPageResponse<RequestListResponse> result = requestService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return empty list when no requests")
        void getMyList_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(requestRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(Collections.emptyList());

            CursorPageResponse<RequestListResponse> result = requestService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("should return request detail successfully")
        void getDetail_success() {
            RequestDetailResponse expectedResponse = new RequestDetailResponse(
                    1L, RequestType.TEAM, "New Team", "We need this team",
                    RequestStatus.PENDING, null, null, null
            );

            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
            when(requestMapper.toDetailResponse(request)).thenReturn(expectedResponse);

            RequestDetailResponse result = requestService.getDetail(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("New Team");
        }

        @Test
        @DisplayName("should throw exception when request not found")
        void getDetail_notFound_throwsException() {
            when(requestRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> requestService.getDetail(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when request is deleted")
        void getDetail_deleted_throwsException() {
            request.softDelete();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            CustomException exception = assertThrows(CustomException.class,
                    () -> requestService.getDetail(1L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user is not the owner")
        void getDetail_accessDenied_throwsException() {
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            CustomException exception = assertThrows(CustomException.class,
                    () -> requestService.getDetail(1L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_ACCESS_DENIED);
        }
    }
}
