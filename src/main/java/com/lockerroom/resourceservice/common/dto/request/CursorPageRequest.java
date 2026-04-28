package com.lockerroom.resourceservice.common.dto.request;

import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@Setter
public class CursorPageRequest {

    private String cursor;

    @Min(1)
    @Max(Constants.MAX_PAGE_SIZE)
    private int size = Constants.DEFAULT_PAGE_SIZE;

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
