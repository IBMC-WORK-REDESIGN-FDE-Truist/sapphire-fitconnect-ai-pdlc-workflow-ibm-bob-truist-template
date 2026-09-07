package com.sapphire.health.temperature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TemperatureRecord} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository}, plus an idempotent
 * upsert that silently ignores duplicate readings (same user, device, and timestamp).
 */
@Repository
public interface TemperatureRepository extends JpaRepository<TemperatureRecord, UUID> {

    /**
     * Inserts a new temperature record, ignoring the insert if a record with the same
     * {@code (user_id, device_source_id, recorded_at)} tuple already exists.
     *
     * <p>This query implements idempotent deduplication at the database level.
     * The method returns the number of rows affected (1 on insert, 0 on duplicate).
     *
     * @param userId          the user who owns the reading
     * @param deviceSourceId  the device or source that produced the reading
     * @param recordedAt      timestamp of the measurement
     * @param ingestedAt      timestamp when the platform received the reading
     * @param value           raw measurement value
     * @param unit            measurement unit as a string (CELSIUS or FAHRENHEIT)
     * @param ingestionSource source type as a string (DEVICE or API)
     * @param measurementMethod optional measurement method; may be null
     * @return 1 if the row was inserted, 0 if it already existed
     */
    @Modifying
    @Query(value = """
        INSERT INTO temperature_records
            (id, user_id, device_source_id, recorded_at, ingested_at,
             value, unit, ingestion_source, measurement_method)
        VALUES
            (gen_random_uuid(), :userId, :deviceSourceId, :recordedAt, :ingestedAt,
             :value, :unit, :ingestionSource, :measurementMethod)
        ON CONFLICT (user_id, device_source_id, recorded_at) DO NOTHING
        """,
        nativeQuery = true)
    int upsert(
        @Param("userId")           UUID userId,
        @Param("deviceSourceId")   String deviceSourceId,
        @Param("recordedAt")       OffsetDateTime recordedAt,
        @Param("ingestedAt")       OffsetDateTime ingestedAt,
        @Param("value")            BigDecimal value,
        @Param("unit")             String unit,
        @Param("ingestionSource")  String ingestionSource,
        @Param("measurementMethod") String measurementMethod
    );
}
