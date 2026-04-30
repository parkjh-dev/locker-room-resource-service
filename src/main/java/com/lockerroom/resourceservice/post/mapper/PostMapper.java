package com.lockerroom.resourceservice.post.mapper;

import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;

import com.lockerroom.resourceservice.post.dto.response.ReportResponse;

import com.lockerroom.resourceservice.post.dto.response.UserLikeListResponse;

import com.lockerroom.resourceservice.post.dto.response.UserPostListResponse;

import com.lockerroom.resourceservice.post.dto.response.PollResponse;

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

    /** hasPoll까지 결정된 후 호출. Service에서 hasPoll 채워서 사용. */
    default PostListResponse toListResponse(Post post, boolean hasPoll) {
        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getUser().getNickname(),
                post.getCategory(),
                hasPoll,
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.isAiGenerated(),
                post.getCreatedAt()
        );
    }

    /** 호환용 — hasPoll 정보 없을 때 (기존 호출처용, 점진 교체). */
    default PostListResponse toListResponse(Post post) {
        return toListResponse(post, false);
    }

    default PostDetailResponse toDetailResponse(Post post,
                                                boolean isLiked,
                                                List<FileResponse> files,
                                                String teamName,
                                                PollResponse poll) {
        return new PostDetailResponse(
                post.getId(),
                post.getBoard().getId(),
                post.getBoard().getName(),
                new AuthorInfo(post.getUser().getId(), post.getUser().getNickname(), teamName, post.getUser().getProfileImageUrl()),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                poll,
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
