package com.lockerroom.resourceservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InquiryReplyRequest(
        @NotBlank String content
) {
}
