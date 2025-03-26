package io.coremaker.weather.api.proxy;

import io.coremaker.weather.api.proxy.config.RateLimitConfig;
import io.coremaker.weather.api.proxy.config.RateLimitConfig.UserRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void testRateLimitingWithConcurrentRequests() throws InterruptedException {
        final int NUM_THREADS = 20;
        final int NUM_USERS = 5;
        final int REQUESTS_PER_USER = 10;

        // Track successful acquisitions per user
        final AtomicInteger[] successCounters = new AtomicInteger[NUM_USERS];
        for (int i = 0; i < NUM_USERS; i++) {
            successCounters[i] = new AtomicInteger(0);
        }

        // Create rate limiters ahead of time
        List<UserRateLimiter> rateLimiters = new ArrayList<>();
        for (int i = 0; i < NUM_USERS; i++) {
            rateLimiters.add(rateLimitConfig.resolveRateLimiter("user-" + i));
        }

        // Use a latch to ensure all threads start at roughly the same time
        final CountDownLatch startLatch = new CountDownLatch(1);
        // Use a latch to wait for all threads to finish
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            // Submit tasks to the thread pool
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadNum = i;
                executor.submit(() -> {
                    try {
                        // Wait for the signal to start
                        startLatch.await();

                        // Each thread will try to acquire permits for multiple users
                        for (int j = 0; j < REQUESTS_PER_USER; j++) {
                            // Determine which user this thread will simulate for this request
                            // We distribute threads across users
                            int userIndex = (threadNum + j) % NUM_USERS;
                            UserRateLimiter limiter = rateLimiters.get(userIndex);

                            // Try to acquire a permit
                            if (limiter.tryAcquire()) {
                                // If successful, increment the counter for this user
                                successCounters[userIndex].incrementAndGet();
                            }

                            // Small delay to simulate processing time
                            Thread.sleep(5);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        // Signal that this thread is done
                        endLatch.countDown();
                    }
                });
            }

            // Start all threads at the same time
            startLatch.countDown();

            // Wait for all threads to finish (with a timeout)
            boolean completed = endLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "Test timed out");

        } finally {
            // Ensure the executor is always shut down
            shutdownAndAwaitTermination(executor);
        }

        // Verify that each user was rate limited correctly
        for (int i = 0; i < NUM_USERS; i++) {
            int successCount = successCounters[i].get();
            System.out.println("User " + i + " successful requests: " + successCount);

            // Each user should have at most 5 successful acquisitions (the rate limit)
            assertTrue(successCount <= 5,
                    "User " + i + " exceeded rate limit: " + successCount + " > 5");
        }

        // Verify that at least some requests were successful for each user
        for (int i = 0; i < NUM_USERS; i++) {
            int successCount = successCounters[i].get();
            assertTrue(successCount > 0,
                    "User " + i + " had no successful requests");
        }

        // Verify that some requests were rate limited
        boolean someRateLimited = false;
        for (int i = 0; i < NUM_USERS; i++) {
            if (successCounters[i].get() < REQUESTS_PER_USER) {
                someRateLimited = true;
                break;
            }
        }
        assertTrue(someRateLimited, "No rate limiting occurred");
    }

    // Helper method for properly shutting down an ExecutorService
    private void shutdownAndAwaitTermination(ExecutorService executor) {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
