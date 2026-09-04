package com.sapphire.charting.temperature;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller exposing the temperature trend reporting endpoint.
 *
 * <p>Endpoint: {@code GET /metrics/temperature/trend}
 *
 * <p>All business logic is delegated to {@link TemperatureTrendService}. This controller
 * contains no business logic — it only deserialises query parameters and serialises
 * the response.
 */
@RestController
@RequestMapping("/metrics/temperature")
public class TemperatureTrendController {

    private final TemperatureTrendService trendService;

    public TemperatureTrendController(TemperatureTrendService trendService) {
        this.trendService = trendService;
    }

    /**
     * Returns aggregated temperature trend data for the authenticated user.
     *
     * @param userId         the user ID (typically resolved from JWT in a full implementation)
     * @param range          aggregation granularity — must be DAY, WEEK, or MONTH
     * @param from           inclusive start date (ISO-8601); defaults to 30 days ago when absent
     * @param to             inclusive end date (ISO-8601); defaults to today when absent
     * @param deviceSourceId optional device filter; null to include all devices
     * @return 200 OK with {@link TemperatureTrendResponse}; empty {@code dataPoints} list when no data
     */
    @GetMapping("/trend")
    public ResponseEntity<TemperatureTrendResponse> getTrend(
            @RequestParam UUID userId,
            @RequestParam PeriodType range,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String deviceSourceId) {

        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate resolvedTo   = to   != null ? to   : LocalDate.now();

        TemperatureTrendResponse response = trendService.getTrend(
            userId, range, resolvedFrom, resolvedTo, deviceSourceId
        );
        return ResponseEntity.ok(response);
    }
}
