package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.model.entity.Board;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.entity.PostReport;
import com.lockerroom.resourceservice.model.entity.Sport;
import com.lockerroom.resourceservice.model.entity.Team;
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

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    BoardResponse toBoardResponse(Board board);

    @Mapping(source = "active", target = "isActive")
    SportResponse toSportResponse(Sport sport);

    @Mapping(source = "active", target = "isActive")
    TeamResponse toTeamResponse(Team team);
}
