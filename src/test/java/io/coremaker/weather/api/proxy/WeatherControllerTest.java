package io.coremaker.weather.api.proxy;

import com.github.benmanes.caffeine.cache.Cache;
import io.coremaker.weather.api.proxy.model.NominatimResponse;
import io.coremaker.weather.api.proxy.model.OpenMeteoResponse;
import io.coremaker.weather.api.proxy.model.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.contains;
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
	public static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
	public static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";
	public static final String URI_PATH = "/weather";
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RestTemplate restTemplate;

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

		when(restTemplate
				.getForEntity(contains(NOMINATIM_URL), eq(NominatimResponse[].class)))
				.thenReturn(new ResponseEntity<>(new NominatimResponse[] {location},HttpStatus.OK));

		when(restTemplate.getForEntity(contains(OPEN_METEO_URL), eq(OpenMeteoResponse.class)))
				.thenReturn(new ResponseEntity<>(meteoResponse, HttpStatus.OK));
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
		verify(restTemplate, times(1))
				.getForEntity(contains(NOMINATIM_URL), eq(NominatimResponse[].class));
		verify(restTemplate, times(1))
				.getForEntity(contains(OPEN_METEO_URL), eq(OpenMeteoResponse.class));
	}

	@Test
	public void testGetWeatherWithCacheHit() throws Exception {
		performRequestWithSuccess(USER_ID_VALUE);
		performRequestWithSuccess(USER_ID_VALUE);

		// Verify the external APIs were called only once
		verify(restTemplate, times(1))
				.getForEntity(contains(NOMINATIM_URL), eq(NominatimResponse[].class));
		verify(restTemplate, times(1))
				.getForEntity(contains(OPEN_METEO_URL), eq(OpenMeteoResponse.class));
	}

	private void performRequestWithSuccess(final String userId) throws Exception {
		mockMvc.perform(get(URI_PATH).param(CITY, LONDON).header(USER_ID, userId))
				.andExpect(status().isOk());
	}

	@Test
	public void testGetWeatherWithRateLimitExceeded() throws Exception {
		for (int i = 0; i < 5; i++) {
			performRequestWithSuccess(USER_ID_VALUE_2);
		}
		mockMvc.perform(get(URI_PATH).param(CITY, LONDON).header(USER_ID, USER_ID_VALUE_2))
				.andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()))
				.andExpect(jsonPath("$.status", is(HttpStatus.TOO_MANY_REQUESTS.value())))
				.andExpect(jsonPath("$.message").exists());
	}
}
