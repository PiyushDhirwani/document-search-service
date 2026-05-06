package com.distributed_document.document_search_service.config;

import com.distributed_document.document_search_service.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    private final Map<String, TenantRateLimit> tenantLimits = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            return true; // Let validation handle missing tenant
        }

        TenantRateLimit rateLimit = tenantLimits.computeIfAbsent(tenantId, k -> new TenantRateLimit());

        long now = System.currentTimeMillis();
        if (now - rateLimit.windowStart > 60_000) {
            rateLimit.windowStart = now;
            rateLimit.count.set(0);
        }

        int currentCount = rateLimit.count.incrementAndGet();
        if (currentCount > requestsPerMinute) {
            log.warn("Rate limit exceeded for tenant: {}, count: {}", tenantId, currentCount);
            throw new RateLimitExceededException(
                    "Rate limit exceeded for tenant: " + tenantId + ". Max " + requestsPerMinute + " requests/minute.");
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(requestsPerMinute - currentCount));
        return true;
    }

    private static class TenantRateLimit {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
