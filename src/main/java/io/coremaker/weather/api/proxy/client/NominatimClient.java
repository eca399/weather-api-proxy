package io.coremaker.weather.api.proxy.client;

import io.coremaker.weather.api.proxy.model.NominatimResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "nominatim", url = "${nominatim.api.url}")
public interface NominatimClient {

    @GetMapping("/search")
    List<NominatimResponse> getLocationCoordinates(@RequestParam("q") String city,
                                                   @RequestParam("format") String format);
}