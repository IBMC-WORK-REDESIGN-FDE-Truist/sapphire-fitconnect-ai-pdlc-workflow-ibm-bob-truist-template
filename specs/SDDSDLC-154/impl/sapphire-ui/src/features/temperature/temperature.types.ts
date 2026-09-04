// TypeScript interfaces for the temperature feature
// File: sapphire-ui/src/features/temperature/temperature.types.ts

/** Unit of measurement for body temperature display. */
export type TemperatureUnit = 'CELSIUS' | 'FAHRENHEIT';

/** Aggregation range granularity for temperature trends. */
export type TemperatureRange = 'DAY' | 'WEEK' | 'MONTH';

/**
 * A single pre-aggregated temperature data point from the BFF trend query.
 * All values are stored in Celsius; conversion happens client-side via temperatureUtils.
 */
export interface TemperatureDataPoint {
  periodStart: string;  // ISO-8601 date string
  periodEnd: string;    // ISO-8601 date string
  minCelsius: number;
  maxCelsius: number;
  avgCelsius: number;
  recordCount: number;
}

/**
 * Full response payload from the temperatureTrend GraphQL query.
 */
export interface TemperatureTrendPayload {
  userId: string;
  range: TemperatureRange;
  from: string;  // ISO-8601 date string
  to: string;    // ISO-8601 date string
  dataPoints: TemperatureDataPoint[];
}
