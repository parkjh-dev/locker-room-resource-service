package com.lockerroom.resourceservice.user.service.impl;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.post.repository.PostLikeRepository;

import com.lockerroom.resourceservice.post.repository.PostRepository;

import com.lockerroom.resourceservice.post.model.entity.PostLike;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.comment.dto.response.UserCommentListResponse;

import com.lockerroom.resourceservice.comment.repository.CommentRepository;

import com.lockerroom.resourceservice.comment.model.entity.Comment;

import com.lockerroom.resourceservice.user.dto.response.UserTeamInfo;

import com.lockerroom.resourceservice.user.dto.response.UserResponse;

import com.lockerroom.resourceservice.user.repository.UserWithdrawalRepository;

import com.lockerroom.resourceservice.user.repository.UserTeamRepository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.UserWithdrawal;

import com.lockerroom.resourceservice.user.model.entity.UserTeam;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;

import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.user.dto.request.UserUpdateRequest;
import com.lockerroom.resourceservice.user.dto.request.WithdrawRequest;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.comment.mapper.CommentMapper;
import com.lockerroom.resourceservice.post.mapper.PostMapper;
import com.lockerroom.resourceservice.user.mapper.UserMapper;
import com.lockerroom.resourceservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    public UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        List<UserTeam> userTeams = userTeamRepository.findByUserIdWithSport(userId);
        Map<Long, String> teamNameByUserTeamId = batchResolveTeamNames(userTeams);
        List<UserTeamInfo> teams = userTeams.stream()
                .map(ut -> userMapper.toUserTeamInfo(ut, teamNameByUserTeamId.get(ut.getId())))
                .toList();

        return userMapper.toResponse(user, teams);
    }

    private Map<Long, String> batchResolveTeamNames(List<UserTeam> userTeams) {
        if (userTeams.isEmpty()) {
            return Map.of();
        }

        List<Long> footballTeamIds = new ArrayList<>();
        List<Long> baseballTeamIds = new ArrayList<>();
        for (UserTeam ut : userTeams) {
            String sportEn = ut.getSport().getNameEn();
            if ("Football".equalsIgnoreCase(sportEn)) {
                footballTeamIds.add(ut.getTeamId());
            } else if ("Baseball".equalsIgnoreCase(sportEn)) {
                baseballTeamIds.add(ut.getTeamId());
            }
        }

        Map<Long, String> teamNameByTeamId = new HashMap<>();
        if (!footballTeamIds.isEmpty()) {
            footballTeamRepository.findAllById(footballTeamIds)
                    .forEach(ft -> teamNameByTeamId.put(ft.getId(), ft.getNameKo()));
        }
        if (!baseballTeamIds.isEmpty()) {
            baseballTeamRepository.findAllById(baseballTeamIds)
                    .forEach(bt -> teamNameByTeamId.put(bt.getId(), bt.getNameKo()));
        }

        Map<Long, String> result = new HashMap<>();
        for (UserTeam ut : userTeams) {
            result.put(ut.getId(), teamNameByTeamId.get(ut.getTeamId()));
        }
        return result;
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

        if (request.profileImageUrl() != null) {
            user.updateProfileImageUrl(request.profileImageUrl());
        }

        if (request.newPassword() != null) {
            if (request.currentPassword() == null ||
                    !request.currentPassword().equals(user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
            user.updatePassword(request.newPassword());
        }

        return getMyInfo(userId);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUserById(userId);

        if (request.password() == null ||
                !request.password().equals(user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String resolveTeamNameFromUserTeam(UserTeam ut) {
        String sportNameEn = ut.getSport().getNameEn();
        if ("Football".equalsIgnoreCase(sportNameEn)) {
            return footballTeamRepository.findById(ut.getTeamId())
                    .map(FootballTeam::getNameKo)
                    .orElse(null);
        } else if ("Baseball".equalsIgnoreCase(sportNameEn)) {
            return baseballTeamRepository.findById(ut.getTeamId())
                    .map(BaseballTeam::getNameKo)
                    .orElse(null);
        }
        return null;
    }
}
