package io.coremaker.weather.api.proxy;

import io.coremaker.weather.api.proxy.config.RateLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RateLimitConfigTest {

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Test
    public void testRateLimiterCreation() {
        // Get a rate limiter for a user
        RateLimitConfig.UserRateLimiter limiter = rateLimitConfig.resolveRateLimiter("test-user");
        assertNotNull(limiter);

        // First request should succeed
        assertTrue(limiter.tryAcquire());

        // Consume all available permits
        for (int i = 0; i < 4; i++) {
            assertTrue(limiter.tryAcquire());
        }

        // Should be out of permits now
        assertFalse(limiter.tryAcquire());
    }

    @Test
    public void testRateLimiterWithMultipleUsers() {
        // Different users should have separate rate limits
        RateLimitConfig.UserRateLimiter user1Limiter = rateLimitConfig.resolveRateLimiter("user1");
        RateLimitConfig.UserRateLimiter user2Limiter = rateLimitConfig.resolveRateLimiter("user2");

        // Consume all permits for user1
        for (int i = 0; i < 5; i++) {
            assertTrue(user1Limiter.tryAcquire());
        }

        // User1 should be out of permits
        assertFalse(user1Limiter.tryAcquire());

        // User2 should still have permits
        assertTrue(user2Limiter.tryAcquire());
    }
}
