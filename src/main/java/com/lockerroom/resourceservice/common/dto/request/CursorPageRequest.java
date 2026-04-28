package com.lockerroom.resourceservice.common.dto.request;

import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Schema(description = "커서 기반 페이지네이션 요청. 모든 list API가 이 형식 사용.")
@Getter
@Setter
public class CursorPageRequest {

    @Schema(description = "다음 페이지 커서. 첫 호출 시 비워두고 응답의 nextCursor 값을 그대로 다시 전달.", example = "NDI=", nullable = true)
    private String cursor;

    @Schema(description = "한 번에 가져올 아이템 수 (1~MAX). 기본값 사용 권장.", example = "20", defaultValue = "20")
    @Min(1)
    @Max(Constants.MAX_PAGE_SIZE)
    private int size = Constants.DEFAULT_PAGE_SIZE;

    @Schema(description = "정렬 기준. 도메인별 사용 가능 값이 다름 (대부분 createdAt,desc 기본).", example = "createdAt,desc", defaultValue = "createdAt,desc")
    private String sort = Constants.DEFAULT_SORT;

    public Long decodeCursor() {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Long.parseLong(decoded);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String encodeCursor(Long id) {
        if (id == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }
}
