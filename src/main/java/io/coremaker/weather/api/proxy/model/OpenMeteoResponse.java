package io.coremaker.weather.api.proxy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoResponse {

    @JsonProperty("current_weather")
    private CurrentWeather currentWeather;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentWeather {
        private double temperature;

        @JsonProperty("windspeed")
        private double windSpeed;

        @JsonProperty("winddirection")
        private double windDirection;
    }
}