// Temperature metric card — dashboard component
// File: sapphire-ui/src/features/temperature/TemperatureMetricCard.tsx

import React, { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTemperatureTrend } from './useTemperatureTrend';
import { TemperatureChart } from './TemperatureChart';
import { TemperatureChartSkeleton } from './TemperatureChartSkeleton';
import { TemperatureChartEmpty } from './TemperatureChartEmpty';
import { TemperatureChartError } from './TemperatureChartError';
import { TemperatureRange, TemperatureUnit } from './temperature.types';

const RANGE_OPTIONS: TemperatureRange[] = ['DAY', 'WEEK', 'MONTH'];
const UNIT_OPTIONS: TemperatureUnit[] = ['CELSIUS', 'FAHRENHEIT'];

/**
 * Dashboard metric card for body temperature trends.
 *
 * Manages range and unit selection via URL query params (`?range=WEEK&unit=CELSIUS`),
 * making selections deep-linkable. All three required UI states (loading, error, empty)
 * are rendered via dedicated sub-components.
 */
export function TemperatureMetricCard(): React.ReactElement {
  const [searchParams, setSearchParams] = useSearchParams();

  const rawRange = searchParams.get('range')?.toUpperCase() as TemperatureRange | null;
  const range: TemperatureRange =
    rawRange && RANGE_OPTIONS.includes(rawRange) ? rawRange : 'WEEK';

  const rawUnit = searchParams.get('unit')?.toUpperCase() as TemperatureUnit | null;
  const unit: TemperatureUnit =
    rawUnit && UNIT_OPTIONS.includes(rawUnit) ? rawUnit : 'CELSIUS';

  const { data, loading, error, refetch } = useTemperatureTrend();

  const handleRangeChange = useCallback(
    (newRange: TemperatureRange) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set('range', newRange);
        return next;
      });
    },
    [setSearchParams]
  );

  const handleUnitToggle = useCallback(
    (newUnit: TemperatureUnit) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set('unit', newUnit);
        return next;
      });
    },
    [setSearchParams]
  );

  const renderChart = (): React.ReactElement => {
    if (loading) return <TemperatureChartSkeleton />;
    if (error)   return <TemperatureChartError onRetry={refetch} />;
    if (!data?.temperatureTrend?.dataPoints?.length) return <TemperatureChartEmpty />;
    return <TemperatureChart data={data.temperatureTrend} unit={unit} />;
  };

  return (
    <section aria-labelledby="temperature-metric-title">
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
        <h2 id="temperature-metric-title" style={{ margin: 0, fontSize: '1rem' }}>
          Body Temperature
        </h2>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {/* Range selector */}
          <div role="group" aria-label="Date range">
            {RANGE_OPTIONS.map((r) => (
              <button
                key={r}
                type="button"
                aria-pressed={range === r}
                onClick={() => handleRangeChange(r)}
                style={{
                  padding: '0.25rem 0.5rem',
                  fontWeight: range === r ? 'bold' : 'normal',
                  cursor: 'pointer',
                  border: '1px solid var(--color-border, #e5e7eb)',
                  background: range === r ? 'var(--color-accent, #3b82d4)' : 'transparent',
                  color: range === r ? '#fff' : 'inherit',
                  borderRadius: 4,
                }}
              >
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          {/* Unit toggle */}
          <div role="group" aria-label="Temperature unit">
            {UNIT_OPTIONS.map((u) => (
              <button
                key={u}
                type="button"
                aria-pressed={unit === u}
                onClick={() => handleUnitToggle(u)}
                style={{
                  padding: '0.25rem 0.5rem',
                  fontWeight: unit === u ? 'bold' : 'normal',
                  cursor: 'pointer',
                  border: '1px solid var(--color-border, #e5e7eb)',
                  background: unit === u ? 'var(--color-accent, #3b82d4)' : 'transparent',
                  color: unit === u ? '#fff' : 'inherit',
                  borderRadius: 4,
                }}
              >
                {u === 'CELSIUS' ? '°C' : '°F'}
              </button>
            ))}
          </div>
        </div>
      </header>

      {renderChart()}
    </section>
  );
}
