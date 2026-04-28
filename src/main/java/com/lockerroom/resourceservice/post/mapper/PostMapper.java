package com.lockerroom.resourceservice.post.mapper;

import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;

import com.lockerroom.resourceservice.post.dto.response.ReportResponse;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.post.dto.response.PostListResponse;

import com.lockerroom.resourceservice.post.dto.response.PostDetailResponse;

import com.lockerroom.resourceservice.file.dto.response.FileResponse;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;

import com.lockerroom.resourceservice.post.model.entity.Post;
import com.lockerroom.resourceservice.post.model.entity.PostReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(source = "user.nickname", target = "authorNickname")
    @Mapping(source = "aiGenerated", target = "isAiGenerated")
    PostListResponse toListResponse(Post post);

    default PostDetailResponse toDetailResponse(Post post, boolean isLiked, List<FileResponse> files, String teamName) {
        return new PostDetailResponse(
                post.getId(),
                post.getBoard().getId(),
                post.getBoard().getName(),
                new AuthorInfo(post.getUser().getId(), post.getUser().getNickname(), teamName, post.getUser().getProfileImageUrl()),
                post.getTitle(),
                post.getContent(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.isAiGenerated(),
                isLiked,
                files,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    @Mapping(source = "board.id", target = "boardId")
    @Mapping(source = "board.name", target = "boardName")
    UserPostListResponse toUserPostListResponse(Post post);

    @Mapping(source = "board.id", target = "boardId")
    @Mapping(source = "board.name", target = "boardName")
    @Mapping(source = "user.nickname", target = "authorNickname")
    UserLikeListResponse toUserLikeListResponse(Post post);

    @Mapping(source = "id", target = "reportId")
    @Mapping(source = "post.id", target = "postId")
    ReportResponse toReportResponse(PostReport report);

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "post.title", target = "postTitle")
    @Mapping(source = "user.nickname", target = "reporterNickname")
    ReportListResponse toReportListResponse(PostReport report);
}
