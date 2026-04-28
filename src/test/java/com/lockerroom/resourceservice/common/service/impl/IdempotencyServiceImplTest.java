package com.lockerroom.resourceservice.common.service.impl;

import com.lockerroom.resourceservice.common.service.impl.IdempotencyServiceImpl;

import com.lockerroom.resourceservice.infrastructure.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> providerOf(StringRedisTemplate redis) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        return provider;
    }

    @Nested
    @DisplayName("Redis mode")
    class RedisMode {

        @Test
        @DisplayName("tryClaim returns true when SETNX succeeds")
        void tryClaim_first() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));
            String key = "key1";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq(redisKey),
                    eq(IdempotencyServiceImpl.IN_FLIGHT_PLACEHOLDER), any(Duration.class)))
                    .thenReturn(true);

            assertThat(service.tryClaim(key)).isTrue();
        }

        @Test
        @DisplayName("tryClaim returns false when key already exists")
        void tryClaim_existing() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));
            String key = "key2";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq(redisKey),
                    eq(IdempotencyServiceImpl.IN_FLIGHT_PLACEHOLDER), any(Duration.class)))
                    .thenReturn(false);

            assertThat(service.tryClaim(key)).isFalse();
        }

        @Test
        @DisplayName("getExistingResponse returns stored value")
        void getExistingResponse() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));
            String key = "key3";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(redisKey)).thenReturn("{\"code\":\"SUCCESS\"}");

            assertThat(service.getExistingResponse(key)).isEqualTo("{\"code\":\"SUCCESS\"}");
        }

        @Test
        @DisplayName("isInFlight detects placeholder")
        void isInFlight() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));

            assertThat(service.isInFlight(IdempotencyServiceImpl.IN_FLIGHT_PLACEHOLDER)).isTrue();
            assertThat(service.isInFlight("real-response")).isFalse();
            assertThat(service.isInFlight(null)).isFalse();
        }

        @Test
        @DisplayName("saveResponse writes value with TTL")
        void saveResponse() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));
            String key = "key4";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            service.saveResponse(key, "{\"code\":\"SUCCESS\"}");

            verify(valueOperations).set(redisKey, "{\"code\":\"SUCCESS\"}",
                    Duration.ofHours(Constants.IDEMPOTENCY_TTL_HOURS));
        }

        @Test
        @DisplayName("releaseClaim deletes the key")
        void releaseClaim() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(redisTemplate));
            String key = "key5";
            String redisKey = Constants.REDIS_IDEMPOTENCY_KEY + key;

            service.releaseClaim(key);

            verify(redisTemplate).delete(redisKey);
        }
    }

    @Nested
    @DisplayName("Caffeine fallback (no Redis)")
    class FallbackMode {

        @Test
        @DisplayName("tryClaim succeeds for new key, fails on second attempt")
        void tryClaim_lifecycle() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(null));

            assertThat(service.tryClaim("k")).isTrue();
            assertThat(service.tryClaim("k")).isFalse();
        }

        @Test
        @DisplayName("getExistingResponse returns placeholder before save, real value after")
        void getExistingResponse_lifecycle() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(null));

            service.tryClaim("k");
            assertThat(service.isInFlight(service.getExistingResponse("k"))).isTrue();

            service.saveResponse("k", "{\"code\":\"SUCCESS\"}");
            assertThat(service.getExistingResponse("k")).isEqualTo("{\"code\":\"SUCCESS\"}");
            assertThat(service.isInFlight(service.getExistingResponse("k"))).isFalse();
        }

        @Test
        @DisplayName("releaseClaim allows re-claim")
        void releaseClaim_allowsReclaim() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(null));

            assertThat(service.tryClaim("k")).isTrue();
            assertThat(service.tryClaim("k")).isFalse();

            service.releaseClaim("k");

            assertThat(service.tryClaim("k")).isTrue();
        }

        @Test
        @DisplayName("getExistingResponse returns null for unknown key")
        void getExistingResponse_unknown() {
            IdempotencyServiceImpl service = new IdempotencyServiceImpl(providerOf(null));

            assertThat(service.getExistingResponse("missing")).isNull();
        }
    }
}
