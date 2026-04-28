package com.lockerroom.resourceservice.board.mapper;

import com.lockerroom.resourceservice.board.dto.response.BoardResponse;
import com.lockerroom.resourceservice.board.model.entity.Board;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BoardMapper {

    BoardResponse toBoardResponse(Board board);
}
