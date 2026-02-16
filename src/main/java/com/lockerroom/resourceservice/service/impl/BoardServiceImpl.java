package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.BoardResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.PostListResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.model.entity.Board;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.repository.BoardRepository;
import com.lockerroom.resourceservice.repository.PostRepository;
import com.lockerroom.resourceservice.repository.UserTeamRepository;
import com.lockerroom.resourceservice.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final UserTeamRepository userTeamRepository;
    private final PostMapper postMapper;

    @Override
    public List<BoardResponse> getBoards(Long userId) {
        List<Board> publicBoards = boardRepository.findByTypeIn(
                List.of(BoardType.COMMON, BoardType.QNA));

        List<Board> teamBoards = new ArrayList<>();
        if (userId != null) {
            userTeamRepository.findByUserId(userId).forEach(ut ->
                    teamBoards.addAll(boardRepository.findByTeamId(ut.getTeam().getId()))
            );
        }

        List<Board> allBoards = new ArrayList<>(publicBoards);
        allBoards.addAll(teamBoards);

        return allBoards.stream()
                .map(postMapper::toBoardResponse)
                .toList();
    }

    @Override
    public Board validateBoardAccess(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        if (board.getType() == BoardType.TEAM && board.getTeam() != null) {
            if (userId == null || !userTeamRepository.existsByUserIdAndTeamId(userId, board.getTeam().getId())) {
                throw new CustomException(ErrorCode.BOARD_ACCESS_DENIED);
            }
        }

        return board;
    }

    @Override
    public CursorPageResponse<PostListResponse> getPostsByBoard(Long boardId, Long userId,
                                                                  String searchType, String keyword,
                                                                  CursorPageRequest pageRequest) {
        validateBoardAccess(boardId, userId);

        List<Post> posts = postRepository.searchByBoard(boardId, searchType, keyword,
                PageRequest.of(0, pageRequest.getSize() + 1));

        boolean hasNext = posts.size() > pageRequest.getSize();
        List<Post> resultPosts = hasNext ? posts.subList(0, pageRequest.getSize()) : posts;

        List<PostListResponse> items = resultPosts.stream()
                .map(postMapper::toListResponse)
                .toList();

        String nextCursor = hasNext ? String.valueOf(resultPosts.get(resultPosts.size() - 1).getId()) : null;

        return CursorPageResponse.<PostListResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
