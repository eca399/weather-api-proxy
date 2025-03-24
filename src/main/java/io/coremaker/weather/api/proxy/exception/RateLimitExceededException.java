package io.coremaker.weather.api.proxy.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(final String message) {
        super(message);
    }
}