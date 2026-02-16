package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.BoardResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.PostListResponse;
import com.lockerroom.resourceservice.model.entity.Board;

import java.util.List;

public interface BoardService {

    List<BoardResponse> getBoards(Long userId);

    Board validateBoardAccess(Long boardId, Long userId);

    CursorPageResponse<PostListResponse> getPostsByBoard(Long boardId, Long userId,
                                                          String searchType, String keyword,
                                                          CursorPageRequest pageRequest);
}
