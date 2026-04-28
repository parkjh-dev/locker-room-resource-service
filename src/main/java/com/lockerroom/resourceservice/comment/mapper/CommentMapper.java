package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.AuthorInfo;
import com.lockerroom.resourceservice.dto.response.CommentResponse;
import com.lockerroom.resourceservice.dto.response.UserCommentListResponse;
import com.lockerroom.resourceservice.model.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "author", expression = "java(toAuthorInfo(comment, teamName))")
    @Mapping(target = "replies", expression = "java(java.util.List.of())")
    @Mapping(source = "comment.aiGenerated", target = "isAiGenerated")
    CommentResponse toResponse(Comment comment, @org.mapstruct.Context String teamName);

    @Mapping(target = "author", expression = "java(toAuthorInfo(comment, teamName))")
    @Mapping(target = "replies", source = "replies")
    @Mapping(source = "comment.aiGenerated", target = "isAiGenerated")
    CommentResponse toResponseWithReplies(Comment comment, List<CommentResponse> replies, @org.mapstruct.Context String teamName);

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "post.title", target = "postTitle")
    UserCommentListResponse toUserCommentListResponse(Comment comment);

    default AuthorInfo toAuthorInfo(Comment comment, String teamName) {
        return new AuthorInfo(comment.getUser().getId(), comment.getUser().getNickname(), teamName, comment.getUser().getProfileImageUrl());
    }
}
