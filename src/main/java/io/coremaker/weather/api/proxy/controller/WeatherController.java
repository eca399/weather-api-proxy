package io.coremaker.weather.api.proxy.controller;

import io.coremaker.weather.api.proxy.config.RateLimitConfig;
import io.coremaker.weather.api.proxy.exception.RateLimitExceededException;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import io.coremaker.weather.api.proxy.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {
    private final WeatherService weatherService;
    private final RateLimitConfig rateLimitConfig;

    @GetMapping
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam String city,
            @RequestHeader(name = "USER_ID") String userId) {

        var rateLimiter = rateLimitConfig.resolveRateLimiter(userId);
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for user {}", userId);
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
        }

        var weatherResponse = weatherService.getWeatherInfoForCity(city);
        return ResponseEntity.ok(weatherResponse);
    }
}
