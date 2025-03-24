package io.coremaker.weather.api.proxy.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.coremaker.weather.api.proxy.exception.ExternalApiException;
import io.coremaker.weather.api.proxy.model.NominatimResponse;
import io.coremaker.weather.api.proxy.model.OpenMeteoResponse;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {
    private final RestTemplate restTemplate;
    private final Cache<String, WeatherResponse> weatherCache;

    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OPEN_METEO_API_URL = "https://api.open-meteo.com/v1/forecast";


    public WeatherResponse getWeatherInfoForCity(final String city) {
        var cachedWeatherResponse = weatherCache.getIfPresent(city);
        if (cachedWeatherResponse != null) {
            log.info("Cache hit for city {}.", city);
            return cachedWeatherResponse;
        }

        log.info("Cache miss for city {}, calling external APIs", city);

        try {
            var cityCoordinates = getCityCoordinates(city);
            var weatherResponse = getWeatherInfoForCoordinates(cityCoordinates, city);
            weatherCache.put(city, weatherResponse);
            return weatherResponse;
        } catch (final RestClientException e) {
            log.error("Error calling external API for city {}", city, e);
            throw new ExternalApiException("Failed to fetch weather data: " + e.getMessage());
        }
    }

    private NominatimResponse getCityCoordinates(final String city) {
        var url = UriComponentsBuilder.fromUriString(NOMINATIM_API_URL)
                .queryParam("q", city)
                .queryParam("format", "json")
                .build()
                .toUriString();
        var locations = restTemplate.getForEntity(url, NominatimResponse[].class).getBody();
        if (locations == null || locations.length == 0) {
            throw new ExternalApiException("Location coordinates not found for " + city);
        }
        return locations[0];
    }

    private WeatherResponse getWeatherInfoForCoordinates(final NominatimResponse coordinates, final String city) {
        var url = UriComponentsBuilder.fromUriString(OPEN_METEO_API_URL)
                .queryParam("latitude", coordinates.getLat())
                .queryParam("longitude", coordinates.getLon())
                .queryParam("current_weather", true)
                .build()
                .toUriString();

        var weatherData = restTemplate.getForEntity(url, OpenMeteoResponse.class).getBody();
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