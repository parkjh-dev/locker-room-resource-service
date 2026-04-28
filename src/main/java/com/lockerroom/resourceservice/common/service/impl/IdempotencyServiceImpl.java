package com.lockerroom.resourceservice.common.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lockerroom.resourceservice.common.service.IdempotencyService;
import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    static final String IN_FLIGHT_PLACEHOLDER = "__IN_FLIGHT__";
    private static final long FALLBACK_MAX_SIZE = 10_000;

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final Cache<String, String> fallbackCache;

    public IdempotencyServiceImpl(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
        this.fallbackCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS))
                .maximumSize(FALLBACK_MAX_SIZE)
                .build();
        if (redisProvider.getIfAvailable() == null) {
            log.info("StringRedisTemplate not available — using Caffeine fallback for idempotency");
        }
    }

    @Override
    public boolean tryClaim(String idempotencyKey) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        String redisKey = redisKey(idempotencyKey);
        if (redis != null) {
            Boolean claimed = redis.opsForValue().setIfAbsent(redisKey, IN_FLIGHT_PLACEHOLDER,
                    Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS));
            return Boolean.TRUE.equals(claimed);
        }
        return fallbackCache.asMap().putIfAbsent(idempotencyKey, IN_FLIGHT_PLACEHOLDER) == null;
    }

    @Override
    public String getExistingResponse(String idempotencyKey) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            return redis.opsForValue().get(redisKey(idempotencyKey));
        }
        return fallbackCache.getIfPresent(idempotencyKey);
    }

    @Override
    public boolean isInFlight(String value) {
        return IN_FLIGHT_PLACEHOLDER.equals(value);
    }

    @Override
    public void saveResponse(String idempotencyKey, String response) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            redis.opsForValue().set(redisKey(idempotencyKey), response,
                    Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS));
            log.debug("Idempotency response saved to Redis for key: {}", idempotencyKey);
        } else {
            fallbackCache.put(idempotencyKey, response);
            log.debug("Idempotency response saved to Caffeine for key: {}", idempotencyKey);
        }
    }

    @Override
    public void releaseClaim(String idempotencyKey) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            redis.delete(redisKey(idempotencyKey));
        } else {
            fallbackCache.invalidate(idempotencyKey);
        }
    }

    private String redisKey(String idempotencyKey) {
        return Constants.REDIS_IDEMPOTENCY_KEY + idempotencyKey;
    }
}
