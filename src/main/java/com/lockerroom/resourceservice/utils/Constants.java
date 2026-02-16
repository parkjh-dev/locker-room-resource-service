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
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MAX_FILE_COUNT = 5;

    // Redis key prefix
    public static final String REDIS_KEY_PREFIX = "resource-service:";
    public static final String REDIS_USER_KEY = REDIS_KEY_PREFIX + "user:";
    public static final String REDIS_POST_KEY = REDIS_KEY_PREFIX + "post:";
    public static final String REDIS_IDEMPOTENCY_KEY = REDIS_KEY_PREFIX + "idempotency:";
    public static final long IDEMPOTENCY_TTL_HOURS = 24;

    // Kafka topics
    public static final String KAFKA_TOPIC_QNA_POST_CREATED = "qna-post.created";
    public static final String KAFKA_TOPIC_NOTIFICATION_COMMENT = "notification.comment";
    public static final String KAFKA_TOPIC_NOTIFICATION_REPLY = "notification.reply";
    public static final String KAFKA_TOPIC_NOTIFICATION_INQUIRY_REPLIED = "notification.inquiry-replied";
    public static final String KAFKA_TOPIC_NOTIFICATION_REPORT_PROCESSED = "notification.report-processed";
}
