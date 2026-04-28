package com.lockerroom.resourceservice.board.controller;

import com.lockerroom.resourceservice.board.dto.response.BoardResponse;
import com.lockerroom.resourceservice.board.model.enums.SearchType;
import com.lockerroom.resourceservice.board.service.BoardService;
import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.post.dto.response.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "게시판", description = "게시판 목록 및 게시판별 게시글 조회 (모두 익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "게시판 목록", description = "공통/Q&A/공지 게시판 목록을 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards(
            @CurrentUserId(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoards(userId)));
    }

    @Operation(summary = "게시판별 게시글 목록", description = "특정 게시판의 게시글을 검색/페이지네이션 조건으로 조회합니다. 익명 접근 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BOARD_NOT_FOUND")
    })
    @SecurityRequirements
    @GetMapping("/{boardId}/posts")
    public ResponseEntity<ApiResponse<CursorPageResponse<PostListResponse>>> getPostsByBoard(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "검색 대상 (TITLE, CONTENT, AUTHOR 등)") @RequestParam(required = false) SearchType searchType,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.getPostsByBoard(boardId, userId, searchType, keyword, pageRequest)));
    }
}
