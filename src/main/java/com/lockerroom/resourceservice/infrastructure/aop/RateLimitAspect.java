package com.lockerroom.resourceservice.infrastructure.aop;

import com.lockerroom.resourceservice.infrastructure.aop.RateLimit;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate-limit:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            // Without Redis we cannot enforce a global rate limit; permit the request.
            return joinPoint.proceed();
        }

        String key = KEY_PREFIX + rateLimit.bucket() + ":" + currentPrincipal();
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(rateLimit.windowSeconds()));
        }
        if (count != null && count > rateLimit.max()) {
            log.warn("Rate limit exceeded for bucket {} principal {} (count={})",
                    rateLimit.bucket(), currentPrincipal(), count);
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return joinPoint.proceed();
    }

    private String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "anonymous";
        }
        return auth.getName();
    }
}
