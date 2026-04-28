package com.lockerroom.resourceservice.board.service.impl;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.board.dto.response.BoardResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.post.dto.response.PostListResponse;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.board.mapper.BoardMapper;
import com.lockerroom.resourceservice.post.mapper.PostMapper;
import com.lockerroom.resourceservice.board.model.entity.Board;
import com.lockerroom.resourceservice.post.model.entity.Post;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.board.model.enums.SearchType;
import com.lockerroom.resourceservice.board.repository.BoardRepository;
import com.lockerroom.resourceservice.post.repository.PostRepository;
import com.lockerroom.resourceservice.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final BoardMapper boardMapper;
    private final PostMapper postMapper;

    @Override
    public List<BoardResponse> getBoards(Long userId) {
        List<Board> boards = boardRepository.findByTypeIn(
                List.of(BoardType.COMMON, BoardType.QNA, BoardType.NOTICE));

        return boards.stream()
                .map(boardMapper::toBoardResponse)
                .toList();
    }

    @Override
    public Board validateBoardAccess(Long boardId, Long userId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }

    @Override
    public CursorPageResponse<PostListResponse> getPostsByBoard(Long boardId, Long userId,
                                                                  SearchType searchType, String keyword,
                                                                  CursorPageRequest pageRequest) {
        validateBoardAccess(boardId, userId);

        Long cursorId = pageRequest.decodeCursor();

        String searchTypeName = (searchType != null) ? searchType.name() : null;
        List<Post> posts = postRepository.searchByBoard(boardId, searchTypeName, keyword,
                cursorId, PageRequest.of(0, pageRequest.getSize() + 1));

        boolean hasNext = posts.size() > pageRequest.getSize();
        List<Post> resultPosts = hasNext ? posts.subList(0, pageRequest.getSize()) : posts;

        List<PostListResponse> items = resultPosts.stream()
                .map(postMapper::toListResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultPosts.get(resultPosts.size() - 1).getId())
                : null;

        return CursorPageResponse.<PostListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
