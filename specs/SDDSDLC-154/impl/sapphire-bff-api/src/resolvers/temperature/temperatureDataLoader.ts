// DataLoader for batching temperatureTrend queries to the charting-api REST endpoint.
// File: sapphire-bff-api/src/resolvers/temperature/temperatureDataLoader.ts

import DataLoader from 'dataloader';
import axios from 'axios';

/** Key shape used to batch-load temperature trend data. */
export interface TemperatureTrendKey {
  userId: string;
  range: string;
  from?: string;
  to?: string;
  deviceSourceId?: string;
}

/** Shape returned by the charting-api GET /metrics/temperature/trend endpoint. */
export interface TemperatureTrendPayload {
  userId: string;
  range: string;
  from: string;
  to: string;
  dataPoints: Array<{
    periodStart: string;
    periodEnd: string;
    minCelsius: number;
    maxCelsius: number;
    avgCelsius: number;
    recordCount: number;
  }>;
}

const CHARTING_API_BASE_URL =
  process.env.CHARTING_API_BASE_URL ?? 'http://sapphire-charting-api:8080';

/**
 * Creates a DataLoader that batches concurrent temperatureTrend requests from a single
 * GraphQL execution into individual HTTP calls to the charting-api.
 *
 * Note: DataLoader batches by default; each key still results in its own HTTP call since
 * the charting-api /trend endpoint returns results per user/range combination. If the
 * charting-api gains a bulk endpoint, this loader can be updated to issue a single request.
 *
 * @returns a DataLoader keyed by TemperatureTrendKey
 */
export function createTemperatureTrendDataLoader(): DataLoader<
  TemperatureTrendKey,
  TemperatureTrendPayload
> {
  return new DataLoader<TemperatureTrendKey, TemperatureTrendPayload>(
    async (keys) => {
      return Promise.all(
        keys.map(async (key) => {
          const params: Record<string, string> = {
            userId: key.userId,
            range: key.range,
            ...(key.from && { from: key.from }),
            ...(key.to && { to: key.to }),
            ...(key.deviceSourceId && { deviceSourceId: key.deviceSourceId }),
          };

          const response = await axios.get<TemperatureTrendPayload>(
            `${CHARTING_API_BASE_URL}/metrics/temperature/trend`,
            { params }
          );
          return response.data;
        })
      );
    },
    {
      // Cache key: unique per user+range+from+to+device combination
      cacheKeyFn: (key) =>
        `${key.userId}:${key.range}:${key.from ?? ''}:${key.to ?? ''}:${key.deviceSourceId ?? ''}`,
    }
  );
}
