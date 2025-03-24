package io.coremaker.weather.api.proxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private String city;
    private double temperature;
    private double windSpeed;
    private double windDirection;
    private LocalDateTime timestamp;
}