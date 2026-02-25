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
import com.lockerroom.resourceservice.repository.RequestRepository;
import com.lockerroom.resourceservice.repository.UserRepository;
import com.lockerroom.resourceservice.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public RequestDetailResponse create(Long userId, RequestCreateRequest request) {
        User user = findUserById(userId);

        Request entity = Request.builder()
                .user(user)
                .type(request.type())
                .name(request.name())
                .reason(request.reason())
                .build();

        Request saved = requestRepository.save(entity);

        return requestMapper.toDetailResponse(saved);
    }

    @Override
    public CursorPageResponse<RequestListResponse> getMyList(Long userId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Request> requests = (cursorId != null)
                ? requestRepository.findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(userId, cursorId, pageable)
                : requestRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(userId, pageable);

        boolean hasNext = requests.size() > pageRequest.getSize();
        List<Request> resultRequests = hasNext ? requests.subList(0, pageRequest.getSize()) : requests;

        List<RequestListResponse> items = resultRequests.stream()
                .map(requestMapper::toListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultRequests.get(resultRequests.size() - 1).getId())
                : null;

        return CursorPageResponse.<RequestListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public RequestDetailResponse getDetail(Long requestId, Long userId) {
        Request request = findRequestById(requestId);

        if (!request.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        return requestMapper.toDetailResponse(request);
    }

    private Request findRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.REQUEST_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
