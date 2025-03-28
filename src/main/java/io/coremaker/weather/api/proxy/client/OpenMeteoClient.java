package io.coremaker.weather.api.proxy.client;

import io.coremaker.weather.api.proxy.model.OpenMeteoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openmeteo", url = "${open.meteo.api.url}")
public interface OpenMeteoClient {

    @GetMapping("/forecast")
    OpenMeteoResponse getWeatherData(@RequestParam("latitude") String latitude,
                                            @RequestParam("longitude") String longitude,
                                            @RequestParam("current_weather") boolean currentWeather);
}