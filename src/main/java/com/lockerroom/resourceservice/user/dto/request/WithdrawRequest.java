package com.lockerroom.resourceservice.user.dto.request;

public record WithdrawRequest(
        String reason,
        String password
) {
}
