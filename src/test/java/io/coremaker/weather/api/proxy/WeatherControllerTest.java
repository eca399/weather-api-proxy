package io.coremaker.weather.api.proxy;

import com.github.benmanes.caffeine.cache.Cache;
import io.coremaker.weather.api.proxy.client.NominatimClient;
import io.coremaker.weather.api.proxy.client.OpenMeteoClient;
import io.coremaker.weather.api.proxy.model.NominatimResponse;
import io.coremaker.weather.api.proxy.model.OpenMeteoResponse;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {

	public static final String LAT = "51.5074456";
	public static final String LON = "-0.1277653";
	public static final String LONDON = "London";
	public static final double TEMPERATURE = 16.5;
	public static final double WIND_SPEED = 14.2;
	public static final double WIND_DIRECTION = 124;
	public static final String CITY = "city";
	public static final String USER_ID = "USER_ID";
	public static final String USER_ID_VALUE = "USER_ID_VALUE";
	public static final String USER_ID_VALUE_2 = "USER_ID_VALUE_2";
	public static final String PATH = "/weather";
	public static final String JSON = "json";
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NominatimClient nominatimClient;

	@MockitoBean
	private OpenMeteoClient openMeteoClient;

	@Autowired
	private Cache<String, WeatherResponse> weatherCache;

	@BeforeEach
	void setUp() {
		weatherCache.invalidateAll();

		var location = new NominatimResponse();
		location.setLat(LAT);
		location.setLon(LON);
		location.setName(LONDON);

		var currentWeather = new OpenMeteoResponse.CurrentWeather();
		currentWeather.setTemperature(TEMPERATURE);
		currentWeather.setWindSpeed(WIND_SPEED);
		currentWeather.setWindDirection(WIND_DIRECTION);

		var meteoResponse = new OpenMeteoResponse();
		meteoResponse.setCurrentWeather(currentWeather);

		when(nominatimClient.getLocationCoordinates(eq(LONDON), eq(JSON)))
				.thenReturn(Collections.singletonList(location));

		when(openMeteoClient.getWeatherData(anyString(), anyString(), anyBoolean()))
				.thenReturn(meteoResponse);
	}

	@Test
	public void testGetWeatherSuccess() throws Exception {
		mockMvc.perform(get("/weather")
				.param(CITY, LONDON)
				.header(USER_ID, USER_ID_VALUE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city", is(LONDON)))
				.andExpect(jsonPath("$.temperature", is(TEMPERATURE)))
				.andExpect(jsonPath("$.windSpeed", is(WIND_SPEED)))
				.andExpect(jsonPath("$.windDirection", is(WIND_DIRECTION)));

		// Verify the external APIs were called
		verify(nominatimClient, times(1))
				.getLocationCoordinates(eq(LONDON), eq(JSON));
		verify(openMeteoClient, times(1))
				.getWeatherData(anyString(), anyString(), anyBoolean());
	}

	@Test
	public void testGetWeatherWithCacheHit() throws Exception {
		performRequestWithSuccess(USER_ID_VALUE);
		performRequestWithSuccess(USER_ID_VALUE);

		// Verify the external APIs were called only once
		verify(nominatimClient, times(1))
				.getLocationCoordinates(LONDON, JSON);
		verify(openMeteoClient, times(1))
				.getWeatherData(anyString(), anyString(), anyBoolean());
	}

	private void performRequestWithSuccess(final String userId) throws Exception {
		mockMvc.perform(get(PATH).param(CITY, LONDON).header(USER_ID, userId))
				.andExpect(status().isOk());
	}

	@Test
	public void testGetWeatherWithRateLimitExceeded() throws Exception {
		for (int i = 0; i < 5; i++) {
			performRequestWithSuccess(USER_ID_VALUE_2);
		}
		mockMvc.perform(get(PATH).param(CITY, LONDON).header(USER_ID, USER_ID_VALUE_2))
				.andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()))
				.andExpect(jsonPath("$.status", is(HttpStatus.TOO_MANY_REQUESTS.value())))
				.andExpect(jsonPath("$.message").exists());
	}
}