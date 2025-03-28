package io.coremaker.weather.api.proxy.service;

import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import io.coremaker.weather.api.proxy.client.NominatimClient;
import io.coremaker.weather.api.proxy.client.OpenMeteoClient;
import io.coremaker.weather.api.proxy.exception.ExternalApiException;
import io.coremaker.weather.api.proxy.model.NominatimResponse;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {
    private final Cache<String, WeatherResponse> weatherCache;
    private final NominatimClient nominatimClient;
    private final OpenMeteoClient openMeteoClient;

    public WeatherResponse getWeatherInfoForCity(final String city) {
        var cachedWeatherResponse = weatherCache.getIfPresent(city);
        if (cachedWeatherResponse != null) {
            log.info("Cache hit for city {}.", city);
            return cachedWeatherResponse;
        }

        log.info("Cache miss for city {}, calling external APIs", city);

        try {
            var locationCoordinates = getLocationCoordinates(city);
            var weatherResponse = getWeatherInfoForCoordinates(locationCoordinates, city);
            weatherCache.put(city, weatherResponse);
            return weatherResponse;
        } catch (FeignException e) {
            log.error("Error calling external API for city {}", city, e);
            throw new ExternalApiException("Failed to fetch weather data: " + e.getMessage());
        }
    }

    private NominatimResponse getLocationCoordinates(final String city) {
        var locations = nominatimClient.getLocationCoordinates(city, "json");
        if (locations == null || locations.isEmpty()) {
            throw new ExternalApiException("Location coordinates not found for " + city);
        }
        return locations.get(0);
    }

    private WeatherResponse getWeatherInfoForCoordinates(final NominatimResponse coordinates, final String city) {
        var weatherData = openMeteoClient.getWeatherData(
                coordinates.getLat(),
                coordinates.getLon(),
                true);
        if (weatherData == null || weatherData.getCurrentWeather() == null) {
            throw new ExternalApiException("No weather data found for " + city);
        }

        var cityName = Optional.ofNullable(coordinates.getName()).orElse(city);

        return WeatherResponse.builder()
                .city(cityName)
                .temperature(weatherData.getCurrentWeather().getTemperature())
                .windSpeed(weatherData.getCurrentWeather().getWindSpeed())
                .windDirection(weatherData.getCurrentWeather().getWindDirection())
                .timestamp(LocalDateTime.now())
                .build();
    }
}