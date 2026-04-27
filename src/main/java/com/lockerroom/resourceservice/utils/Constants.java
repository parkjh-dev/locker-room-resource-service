package com.lockerroom.resourceservice.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {


    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT = "createdAt";

    // File
    public static final long MAX_IMAGE_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_DOCUMENT_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    public static final int MAX_FILE_COUNT = 5;

    // Allowed MIME types
    public static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    public static final java.util.Set<String> ALLOWED_DOCUMENT_TYPES = java.util.Set.of(
            "application/pdf", "text/plain"
    );
    public static final java.util.Set<String> ALLOWED_MIME_TYPES;

    static {
        var all = new java.util.HashSet<>(ALLOWED_IMAGE_TYPES);
        all.addAll(ALLOWED_DOCUMENT_TYPES);
        ALLOWED_MIME_TYPES = java.util.Set.copyOf(all);
    }

    // Redis key prefix
    public static final String REDIS_KEY_PREFIX = "resource-service:";
    public static final String REDIS_IDEMPOTENCY_KEY = REDIS_KEY_PREFIX + "idempotency:";
    public static final long IDEMPOTENCY_TTL_HOURS = 24;

    // Kafka topics
    public static final String KAFKA_TOPIC_QNA_POST_CREATED = "qna-post.created";
    public static final String KAFKA_TOPIC_NOTIFICATION_COMMENT = "notification.comment";
    public static final String KAFKA_TOPIC_NOTIFICATION_REPLY = "notification.reply";
    public static final String KAFKA_TOPIC_NOTIFICATION_INQUIRY_REPLIED = "notification.inquiry-replied";
    public static final String KAFKA_TOPIC_NOTIFICATION_REPORT_PROCESSED = "notification.report-processed";
}
