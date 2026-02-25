package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.BoardResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.PostListResponse;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards(
            @CurrentUserId(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoards(userId)));
    }

    @GetMapping("/{boardId}/posts")
    public ResponseEntity<ApiResponse<CursorPageResponse<PostListResponse>>> getPostsByBoard(
            @PathVariable Long boardId,
            @CurrentUserId(required = false) Long userId,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.getPostsByBoard(boardId, userId, searchType, keyword, pageRequest)));
    }
}
