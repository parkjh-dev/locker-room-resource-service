package com.lockerroom.resourceservice.board.service;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.board.dto.response.BoardResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.post.dto.response.PostListResponse;
import com.lockerroom.resourceservice.board.model.entity.Board;
import com.lockerroom.resourceservice.board.model.enums.SearchType;

import java.util.List;

public interface BoardService {

    List<BoardResponse> getBoards(Long userId);

    Board validateBoardAccess(Long boardId, Long userId);

    CursorPageResponse<PostListResponse> getPostsByBoard(Long boardId, Long userId,
                                                          SearchType searchType, String keyword,
                                                          CursorPageRequest pageRequest);
}
