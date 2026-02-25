package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.CommentMapper;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.mapper.UserMapper;
import com.lockerroom.resourceservice.model.entity.*;
import com.lockerroom.resourceservice.repository.*;
import com.lockerroom.resourceservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserWithdrawalRepository userWithdrawalRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    public UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        List<UserTeam> userTeams = userTeamRepository.findByUserId(userId);
        List<UserTeamInfo> teams = userMapper.toUserTeamInfoList(userTeams);

        return userMapper.toResponse(user, teams);
    }

    @Override
    @Transactional
    public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        if (request.nickname() != null) {
            if (userRepository.existsByNickname(request.nickname()) &&
                    !user.getNickname().equals(request.nickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.updateNickname(request.nickname());
        }

        if (request.newPassword() != null) {
            user.updatePassword(request.newPassword());
        }

        return getMyInfo(userId);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUserById(userId);

        UserWithdrawal withdrawal = UserWithdrawal.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .reason(request.reason())
                .withdrawnAt(LocalDateTime.now())
                .build();

        userWithdrawalRepository.save(withdrawal);
        user.softDelete();
    }

    @Override
    public CursorPageResponse<UserPostListResponse> getMyPosts(Long userId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();

        List<Post> posts;
        if (cursorId == null) {
            posts = postRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    userId, PageRequest.of(0, pageRequest.getSize() + 1));
        } else {
            posts = postRepository.findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
                    userId, cursorId, PageRequest.of(0, pageRequest.getSize() + 1));
        }

        boolean hasNext = posts.size() > pageRequest.getSize();
        List<Post> resultPosts = hasNext ? posts.subList(0, pageRequest.getSize()) : posts;

        List<UserPostListResponse> items = resultPosts.stream()
                .map(postMapper::toUserPostListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultPosts.get(resultPosts.size() - 1).getId())
                : null;

        return CursorPageResponse.<UserPostListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public CursorPageResponse<UserCommentListResponse> getMyComments(Long userId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();

        List<Comment> comments;
        if (cursorId == null) {
            comments = commentRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    userId, PageRequest.of(0, pageRequest.getSize() + 1));
        } else {
            comments = commentRepository.findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
                    userId, cursorId, PageRequest.of(0, pageRequest.getSize() + 1));
        }

        boolean hasNext = comments.size() > pageRequest.getSize();
        List<Comment> resultComments = hasNext ? comments.subList(0, pageRequest.getSize()) : comments;

        List<UserCommentListResponse> items = resultComments.stream()
                .map(commentMapper::toUserCommentListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultComments.get(resultComments.size() - 1).getId())
                : null;

        return CursorPageResponse.<UserCommentListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public CursorPageResponse<UserLikeListResponse> getMyLikes(Long userId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.decodeCursor();

        List<PostLike> likes = postLikeRepository.findByUserIdWithPost(
                userId, cursorId, PageRequest.of(0, pageRequest.getSize() + 1));

        boolean hasNext = likes.size() > pageRequest.getSize();
        List<PostLike> resultLikes = hasNext ? likes.subList(0, pageRequest.getSize()) : likes;

        List<UserLikeListResponse> items = resultLikes.stream()
                .map(pl -> postMapper.toUserLikeListResponse(pl.getPost()))
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultLikes.get(resultLikes.size() - 1).getId())
                : null;

        return CursorPageResponse.<UserLikeListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
