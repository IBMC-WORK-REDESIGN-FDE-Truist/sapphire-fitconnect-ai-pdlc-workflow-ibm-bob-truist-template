package com.sapphire.charting.temperature;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Port interface for reading raw temperature records from the health-service data source.
 *
 * <p>Implementations may use a shared PostgreSQL connection, a dedicated read replica,
 * or an HTTP client calling the health-service REST API. The rollup job depends only on
 * this interface, keeping it decoupled from the underlying data source.
 */
public interface RawTemperatureQueryPort {

    /**
     * Returns all raw temperature records with {@code recorded_at} strictly after {@code since},
     * converted to Celsius.
     *
     * @param since exclusive lower bound timestamp
     * @return list of raw records; never null; may be empty
     */
    List<RawTemperatureRecord> findSince(OffsetDateTime since);
}
