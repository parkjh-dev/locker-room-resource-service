package com.lockerroom.resourceservice.dto.request;

import com.lockerroom.resourceservice.model.enums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InquiryCreateRequest(
        @NotNull InquiryType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String content,
        List<Long> fileIds
) {
}
