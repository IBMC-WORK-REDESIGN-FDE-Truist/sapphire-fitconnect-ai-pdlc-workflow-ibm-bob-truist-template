package com.sapphire.health.temperature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice for {@link TemperatureController}.
 *
 * <p>Exercises HTTP layer including request/response serialisation, status codes,
 * and exception-to-problem-detail mapping. {@link TemperatureService} is mocked.
 */
@WebMvcTest(TemperatureController.class)
@DisplayName("TemperatureController")
class TemperatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemperatureService temperatureService;

    private TemperatureReadingRequest validRequest() {
        return new TemperatureReadingRequest(
            "device-123",
            OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            new BigDecimal("37.0"),
            TemperatureUnit.CELSIUS,
            IngestionSource.DEVICE,
            "oral"
        );
    }

    @Test
    @DisplayName("POST /temperature-readings returns 200 on success")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void shouldReturn200OnSuccess() throws Exception {
        when(temperatureService.ingestSingle(any(UUID.class), any()))
            .thenReturn(IngestionResponse.allAccepted(1));

        mockMvc.perform(post("/temperature-readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accepted").value(1))
            .andExpect(jsonPath("$.rejected").value(0));
    }

    @Test
    @DisplayName("POST /temperature-readings returns 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/temperature-readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /temperature-readings returns 422 when out-of-range")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void shouldReturn422OnOutOfRange() throws Exception {
        when(temperatureService.ingestSingle(any(UUID.class), any()))
            .thenThrow(new OutOfRangeException("Temperature 50.0 CELSIUS is outside physiological range"));

        mockMvc.perform(post("/temperature-readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Temperature Out of Physiological Range"));
    }

    @Test
    @DisplayName("POST /temperature-readings returns 429 when rate limited")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void shouldReturn429OnRateLimit() throws Exception {
        when(temperatureService.ingestSingle(any(UUID.class), any()))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded for device device-123"));

        mockMvc.perform(post("/temperature-readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"));
    }

    @Test
    @DisplayName("POST /temperature-readings/batch returns 207 on partial success")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void shouldReturn207OnPartialBatchSuccess() throws Exception {
        BatchIngestionRequest batchRequest = new BatchIngestionRequest(
            List.of(validRequest(), validRequest())
        );
        IngestionResponse partialResponse = new IngestionResponse(
            1, 1, List.of(new IngestionError(1, "OUT_OF_RANGE", "value out of range"))
        );
        when(temperatureService.ingestBatch(any(UUID.class), any())).thenReturn(partialResponse);

        mockMvc.perform(post("/temperature-readings/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
            .andExpect(status().isMultiStatus())
            .andExpect(jsonPath("$.accepted").value(1))
            .andExpect(jsonPath("$.rejected").value(1));
    }
}
