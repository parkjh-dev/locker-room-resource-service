package com.lockerroom.resourceservice.service;

public interface IdempotencyService {

    boolean isDuplicate(String idempotencyKey);

    String getExistingResponse(String idempotencyKey);

    void saveResponse(String idempotencyKey, String response);
}
