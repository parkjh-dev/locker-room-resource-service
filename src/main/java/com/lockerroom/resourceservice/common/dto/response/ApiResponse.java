package com.lockerroom.resourceservice.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "모든 API 응답을 감싸는 공통 래퍼. 성공/실패 모두 동일 구조 사용.")
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "응답 코드. 성공은 SUCCESS, 실패는 ErrorCode enum 값(USER_NOT_FOUND 등)", example = "SUCCESS")
    private final String code;

    @Schema(description = "사용자에게 노출되는 메시지. 성공 시 안내문, 실패 시 에러 사유.", example = "성공")
    private final String message;

    @Schema(description = "응답 본문 데이터. 응답 종류에 따라 단일 객체/리스트/페이지 모두 가능. 데이터가 없으면 null로 직렬화 제외.")
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "성공", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("SUCCESS", "성공", null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>("SUCCESS", message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
