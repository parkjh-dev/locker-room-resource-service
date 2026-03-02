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
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.repository.BoardRepository;
import com.lockerroom.resourceservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock private BoardRepository boardRepository;
    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks private BoardServiceImpl boardService;

    private Board commonBoard;
    private Board qnaBoard;
    private Board noticeBoard;

    @BeforeEach
    void setUp() {
        commonBoard = Board.builder()
                .id(1L)
                .name("공통 게시판")
                .type(BoardType.COMMON)
                .build();

        qnaBoard = Board.builder()
                .id(2L)
                .name("Q&A 게시판")
                .type(BoardType.QNA)
                .build();

        noticeBoard = Board.builder()
                .id(3L)
                .name("공지 게시판")
                .type(BoardType.NOTICE)
                .build();
    }

    @Nested
    @DisplayName("getBoards")
    class GetBoards {

        @Test
        @DisplayName("should return COMMON, QNA, NOTICE boards")
        void getBoards_success() {
            BoardResponse commonResponse = new BoardResponse(1L, "공통 게시판", BoardType.COMMON);
            BoardResponse qnaResponse = new BoardResponse(2L, "Q&A 게시판", BoardType.QNA);
            BoardResponse noticeResponse = new BoardResponse(3L, "공지 게시판", BoardType.NOTICE);

            when(boardRepository.findByTypeIn(List.of(BoardType.COMMON, BoardType.QNA, BoardType.NOTICE)))
                    .thenReturn(List.of(commonBoard, qnaBoard, noticeBoard));
            when(postMapper.toBoardResponse(commonBoard)).thenReturn(commonResponse);
            when(postMapper.toBoardResponse(qnaBoard)).thenReturn(qnaResponse);
            when(postMapper.toBoardResponse(noticeBoard)).thenReturn(noticeResponse);

            List<BoardResponse> result = boardService.getBoards(1L);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(BoardResponse::type)
                    .containsExactly(BoardType.COMMON, BoardType.QNA, BoardType.NOTICE);
        }

        @Test
        @DisplayName("should return same boards for null userId")
        void getBoards_noAuth() {
            BoardResponse commonResponse = new BoardResponse(1L, "공통 게시판", BoardType.COMMON);

            when(boardRepository.findByTypeIn(List.of(BoardType.COMMON, BoardType.QNA, BoardType.NOTICE)))
                    .thenReturn(List.of(commonBoard));
            when(postMapper.toBoardResponse(commonBoard)).thenReturn(commonResponse);

            List<BoardResponse> result = boardService.getBoards(null);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("validateBoardAccess")
    class ValidateBoardAccess {

        @Test
        @DisplayName("should pass for existing board")
        void validateBoardAccess_success() {
            when(boardRepository.findById(1L)).thenReturn(Optional.of(commonBoard));

            Board result = boardService.validateBoardAccess(1L, null);

            assertThat(result).isEqualTo(commonBoard);
        }

        @Test
        @DisplayName("should throw exception when board not found")
        void validateBoardAccess_boardNotFound() {
            when(boardRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> boardService.validateBoardAccess(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getPostsByBoard")
    class GetPostsByBoard {

        @Test
        @DisplayName("should return paginated posts for board")
        void getPostsByBoard_success() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            User user = User.builder().id(1L).nickname("testuser").build();
            Post post = Post.builder().id(1L).board(commonBoard).user(user).title("Test").content("Content").build();
            PostListResponse postResponse = new PostListResponse(
                    1L, "Test", "testuser", 0, 0, 0, false, null);

            when(boardRepository.findById(1L)).thenReturn(Optional.of(commonBoard));
            when(postRepository.searchByBoard(eq(1L), isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(post));
            when(postMapper.toListResponse(post)).thenReturn(postResponse);

            CursorPageResponse<PostListResponse> result =
                    boardService.getPostsByBoard(1L, null, null, null, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("should set hasNext when more results exist")
        void getPostsByBoard_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            User user = User.builder().id(1L).nickname("testuser").build();
            Post post1 = Post.builder().id(1L).board(commonBoard).user(user).title("Test1").content("C").build();
            Post post2 = Post.builder().id(2L).board(commonBoard).user(user).title("Test2").content("C").build();
            PostListResponse postResponse = new PostListResponse(
                    1L, "Test1", "testuser", 0, 0, 0, false, null);

            when(boardRepository.findById(1L)).thenReturn(Optional.of(commonBoard));
            when(postRepository.searchByBoard(eq(1L), isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(List.of(post1, post2));
            when(postMapper.toListResponse(post1)).thenReturn(postResponse);

            CursorPageResponse<PostListResponse> result =
                    boardService.getPostsByBoard(1L, null, null, null, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(1L));
        }
    }
}
