package com.lockerroom.resourceservice.dto.request;

public record WithdrawRequest(
        String reason,
        String password
) {
}
