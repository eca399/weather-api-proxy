package io.coremaker.weather.api.proxy.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@Configuration
@EnableScheduling
@EnableFeignClients(basePackages = "io.coremaker.weather.api.proxy.client")
public class AppConfig {
    @Bean
    public Cache<String, WeatherResponse> weatherCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }
}
