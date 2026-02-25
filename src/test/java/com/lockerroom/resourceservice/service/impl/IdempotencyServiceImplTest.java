package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("Redis mode")
    class RedisMode {

        @Test
        @DisplayName("isDuplicate should return true when key exists in Redis")
        void isDuplicate_exists_returnsTrue() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(redisTemplate);
            String key = "test-key";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.hasKey(redisKey)).thenReturn(true);

            assertThat(service.isDuplicate(key)).isTrue();
        }

        @Test
        @DisplayName("isDuplicate should return false when key does not exist in Redis")
        void isDuplicate_notExists_returnsFalse() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(redisTemplate);
            String key = "new-key";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.hasKey(redisKey)).thenReturn(false);

            assertThat(service.isDuplicate(key)).isFalse();
        }

        @Test
        @DisplayName("getExistingResponse should return stored response from Redis")
        void getExistingResponse_returnsValue() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(redisTemplate);
            String key = "test-key";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;
            String storedResponse = "{\"code\":\"SUCCESS\"}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(redisKey)).thenReturn(storedResponse);

            String result = service.getExistingResponse(key);

            assertThat(result).isEqualTo(storedResponse);
        }

        @Test
        @DisplayName("getExistingResponse should return null when key not found")
        void getExistingResponse_notFound_returnsNull() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(redisTemplate);
            String key = "missing-key";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(redisKey)).thenReturn(null);

            String result = service.getExistingResponse(key);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("saveResponse should store response in Redis with TTL")
        void saveResponse_savesToRedis() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(redisTemplate);
            String key = "test-key";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;
            String response = "{\"code\":\"SUCCESS\"}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            service.saveResponse(key, response);

            verify(valueOperations).set(redisKey, response,
                    Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS));
        }
    }

    @Nested
    @DisplayName("Fallback mode (no Redis)")
    class FallbackMode {

        @Test
        @DisplayName("isDuplicate should return false for new key in fallback")
        void isDuplicate_newKey_returnsFalse() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(null);

            assertThat(service.isDuplicate("new-key")).isFalse();
        }

        @Test
        @DisplayName("isDuplicate should return true after saving in fallback")
        void isDuplicate_afterSave_returnsTrue() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(null);
            String key = "existing-key";

            service.saveResponse(key, "{\"code\":\"SUCCESS\"}");

            assertThat(service.isDuplicate(key)).isTrue();
        }

        @Test
        @DisplayName("getExistingResponse should return saved response in fallback")
        void getExistingResponse_afterSave_returnsValue() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(null);
            String key = "test-key";
            String response = "{\"code\":\"SUCCESS\",\"data\":{\"id\":1}}";

            service.saveResponse(key, response);

            assertThat(service.getExistingResponse(key)).isEqualTo(response);
        }

        @Test
        @DisplayName("getExistingResponse should return null for missing key in fallback")
        void getExistingResponse_missingKey_returnsNull() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(null);

            assertThat(service.getExistingResponse("missing")).isNull();
        }

        @Test
        @DisplayName("saveResponse should store in memory when no Redis")
        void saveResponse_storesInMemory() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(null);
            String key = "key1";
            String response = "{\"result\":\"ok\"}";

            service.saveResponse(key, response);

            assertThat(service.isDuplicate(key)).isTrue();
            assertThat(service.getExistingResponse(key)).isEqualTo(response);
        }
    }
}
