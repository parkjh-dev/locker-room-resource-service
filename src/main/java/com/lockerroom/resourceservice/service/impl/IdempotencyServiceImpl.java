package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.service.IdempotencyService;
import com.lockerroom.resourceservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    // Redis 미사용 시 인메모리 폴백
    private final Map<String, String> fallbackStore = new ConcurrentHashMap<>();

    public IdempotencyServiceImpl(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.info("StringRedisTemplate not available — using in-memory fallback for idempotency");
        }
    }

    @Override
    public boolean isDuplicate(String idempotencyKey) {
        String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + idempotencyKey;
        if (redisTemplate != null) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        }
        return fallbackStore.containsKey(idempotencyKey);
    }

    @Override
    public String getExistingResponse(String idempotencyKey) {
        String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + idempotencyKey;
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(redisKey);
        }
        return fallbackStore.get(idempotencyKey);
    }

    @Override
    public void saveResponse(String idempotencyKey, String response) {
        String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + idempotencyKey;
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(redisKey, response,
                    Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS));
            log.debug("Idempotency response saved to Redis for key: {}", idempotencyKey);
        } else {
            fallbackStore.put(idempotencyKey, response);
            log.debug("Idempotency response saved to in-memory store for key: {}", idempotencyKey);
        }
    }
}
