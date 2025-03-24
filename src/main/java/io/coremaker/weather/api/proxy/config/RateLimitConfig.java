package io.coremaker.weather.api.proxy.config;


import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<String, UserRateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    public RateLimitConfig() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ZERO)  // Don't wait if no permits available
                .build();
        this.rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
    }

    public UserRateLimiter resolveRateLimiter(final String userId) {
        return userRateLimiters.computeIfAbsent(userId, id -> {
            var rateLimiter = rateLimiterRegistry.rateLimiter(id);
            return new UserRateLimiter(rateLimiter);
        });
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanUpInactiveRateLimiters() {
        var now = System.currentTimeMillis();
        var inactivityThreshold = 30 * 60 * 1000; // 30 minutes

        userRateLimiters.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessTime > inactivityThreshold);
    }


    @Getter
    public static class UserRateLimiter {
        private final RateLimiter rateLimiter;
        private long lastAccessTime;

        public UserRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public boolean tryAcquire() {
            lastAccessTime = System.currentTimeMillis();
            return rateLimiter.acquirePermission();
        }
    }
}