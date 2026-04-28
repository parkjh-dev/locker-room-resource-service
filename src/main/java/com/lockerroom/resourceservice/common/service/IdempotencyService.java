package com.lockerroom.resourceservice.service;

public interface IdempotencyService {

    /**
     * Atomically claim the key. Returns true only if this caller is the first one;
     * subsequent callers see the placeholder until the response is saved.
     */
    boolean tryClaim(String idempotencyKey);

    /**
     * Returns the stored response, the placeholder marker, or null when no entry exists.
     */
    String getExistingResponse(String idempotencyKey);

    /**
     * Indicates the existing entry is still being processed by another request.
     */
    boolean isInFlight(String value);

    /**
     * Overwrite the placeholder with the actual response.
     */
    void saveResponse(String idempotencyKey, String response);

    /**
     * Drop the placeholder so a future retry can claim again (used on failure paths).
     */
    void releaseClaim(String idempotencyKey);
}
