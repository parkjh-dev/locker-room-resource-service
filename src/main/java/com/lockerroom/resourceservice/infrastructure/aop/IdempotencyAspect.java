package com.lockerroom.resourceservice.infrastructure.aop;

import com.lockerroom.resourceservice.infrastructure.aop.Idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.common.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }

        String compositeKey = buildCompositeKey(idempotencyKey);

        if (!idempotencyService.tryClaim(compositeKey)) {
            String existing = idempotencyService.getExistingResponse(compositeKey);
            if (existing == null || idempotencyService.isInFlight(existing)) {
                log.debug("Concurrent in-flight request for idempotency key: {}", idempotencyKey);
                throw new CustomException(ErrorCode.IDEMPOTENCY_DUPLICATE);
            }
            log.debug("Replaying cached response for idempotency key: {}", idempotencyKey);
            return deserializeResponse(existing);
        }

        boolean savedReal = false;
        try {
            Object result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity) {
                idempotencyService.saveResponse(compositeKey, serializeResponse(responseEntity));
                savedReal = true;
            }
            return result;
        } finally {
            if (!savedReal) {
                idempotencyService.releaseClaim(compositeKey);
            }
        }
    }

    private String buildCompositeKey(String idempotencyKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName() + ":" + idempotencyKey;
        }
        return idempotencyKey;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return attrs.getRequest();
    }

    private String serializeResponse(ResponseEntity<?> responseEntity) {
        try {
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("statusCode", responseEntity.getStatusCode().value());
            wrapper.put("body", responseEntity.getBody());
            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            log.warn("Failed to serialize idempotency response", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Object> deserializeResponse(String json) {
        try {
            Map<String, Object> wrapper = objectMapper.readValue(json, Map.class);
            Number statusCode = (Number) wrapper.get("statusCode");
            Object body = wrapper.get("body");
            return ResponseEntity.status(statusCode.intValue()).body(body);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached idempotency response", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
