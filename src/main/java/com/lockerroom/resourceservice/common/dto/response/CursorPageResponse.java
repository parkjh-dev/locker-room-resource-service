package com.lockerroom.resourceservice.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "커서 기반 페이지네이션 응답. items + nextCursor + hasNext 3개 필드.")
@Getter
@Builder
public class CursorPageResponse<T> {

    @Schema(description = "현재 페이지의 아이템 목록")
    private final List<T> items;

    @Schema(description = "다음 페이지 요청 시 사용할 커서. 마지막 페이지면 null.", example = "NDI=", nullable = true)
    private final String nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private final boolean hasNext;
}
