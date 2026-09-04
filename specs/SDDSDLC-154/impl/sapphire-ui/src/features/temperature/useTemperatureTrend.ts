// useTemperatureTrend custom hook
// File: sapphire-ui/src/features/temperature/useTemperatureTrend.ts

import { useQuery } from '@apollo/client';
import { useSearchParams } from 'react-router-dom';
import {
  GetTemperatureTrendDocument,
  GetTemperatureTrendQuery,
  GetTemperatureTrendQueryVariables,
} from './__generated__/temperature.generated';
import { TemperatureRange, TemperatureUnit } from './temperature.types';

/**
 * Result shape returned by useTemperatureTrend.
 */
export interface UseTemperatureTrendResult {
  data: GetTemperatureTrendQuery | undefined;
  loading: boolean;
  error: Error | undefined;
  refetch: () => void;
}

/**
 * Custom hook that wraps the GetTemperatureTrend Apollo query.
 *
 * Reads `range` and `unit` from URL search params so that filter selections are
 * deep-linkable and shareable. Falls back to 'WEEK' range and 'CELSIUS' unit when
 * the params are absent or invalid.
 *
 * @param deviceSourceId optional device filter; undefined to include all devices
 * @returns query result with data, loading, error, and a refetch function
 */
export function useTemperatureTrend(
  deviceSourceId?: string
): UseTemperatureTrendResult {
  const [searchParams] = useSearchParams();

  const rawRange = searchParams.get('range')?.toUpperCase();
  const range: TemperatureRange =
    rawRange === 'DAY' || rawRange === 'WEEK' || rawRange === 'MONTH'
      ? rawRange
      : 'WEEK';

  // unit is used by the TemperatureChart for client-side conversion; not passed to BFF
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const rawUnit = searchParams.get('unit')?.toUpperCase();
  const _unit: TemperatureUnit =
    rawUnit === 'FAHRENHEIT' ? 'FAHRENHEIT' : 'CELSIUS';

  const variables: GetTemperatureTrendQueryVariables = {
    range: range as GetTemperatureTrendQueryVariables['range'],
    deviceSourceId: deviceSourceId ?? undefined,
  };

  const { data, loading, error, refetch } = useQuery<
    GetTemperatureTrendQuery,
    GetTemperatureTrendQueryVariables
  >(GetTemperatureTrendDocument, {
    variables,
    fetchPolicy: 'cache-and-network',
  });

  return {
    data,
    loading,
    error: error as Error | undefined,
    refetch,
  };
}
