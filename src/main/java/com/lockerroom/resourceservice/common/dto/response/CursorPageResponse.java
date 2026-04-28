package com.lockerroom.resourceservice.common.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CursorPageResponse<T> {

    private final List<T> items;
    private final String nextCursor;
    private final boolean hasNext;
}
