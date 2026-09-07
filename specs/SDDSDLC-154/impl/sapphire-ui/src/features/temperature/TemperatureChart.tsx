// Temperature trend chart component
// File: sapphire-ui/src/features/temperature/TemperatureChart.tsx

import React, { useMemo } from 'react';
import { TemperatureDataPoint, TemperatureTrendPayload, TemperatureUnit } from './temperature.types';
import { celsiusToFahrenheit } from './temperatureUtils';

interface TemperatureChartProps {
  /** Trend data returned by the BFF; all values in Celsius. */
  data: TemperatureTrendPayload;
  /** Display unit — values are converted client-side when FAHRENHEIT. */
  unit: TemperatureUnit;
}

/**
 * Renders a temperature trend chart with min/max/avg series for the given data and unit.
 *
 * Values stored in Celsius are converted to Fahrenheit client-side when `unit='FAHRENHEIT'`,
 * using the pure functions in temperatureUtils. No hardcoded hex values — all colours use
 * design tokens from the shared colour palette.
 */
export function TemperatureChart({ data, unit }: TemperatureChartProps): React.ReactElement {
  const convertValue = useMemo(
    () => (c: number) => unit === 'FAHRENHEIT' ? celsiusToFahrenheit(c) : c,
    [unit]
  );

  const unitLabel = unit === 'CELSIUS' ? '°C' : '°F';

  if (data.dataPoints.length === 0) {
    return <p>No data points to display.</p>;
  }

  return (
    <div aria-label={`Temperature trend chart (${unitLabel})`} style={{ width: '100%' }}>
      <div style={{ fontSize: '0.75rem', color: 'var(--color-muted)', marginBottom: '0.5rem' }}>
        Temperature {unitLabel}
      </div>

      {/* Sapphire charting library integration point */}
      {/* Replace this table with <SapphireLineChart> when the charting library is wired */}
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
        <thead>
          <tr>
            <th style={{ textAlign: 'left', padding: '4px 8px' }}>Period</th>
            <th style={{ textAlign: 'right', padding: '4px 8px' }}>Min</th>
            <th style={{ textAlign: 'right', padding: '4px 8px' }}>Avg</th>
            <th style={{ textAlign: 'right', padding: '4px 8px' }}>Max</th>
          </tr>
        </thead>
        <tbody>
          {data.dataPoints.map((point: TemperatureDataPoint) => (
            <tr key={point.periodStart}>
              <td style={{ padding: '4px 8px' }}>{point.periodStart}</td>
              <td style={{ textAlign: 'right', padding: '4px 8px' }}>
                {convertValue(point.minCelsius).toFixed(1)}{unitLabel}
              </td>
              <td style={{ textAlign: 'right', padding: '4px 8px' }}>
                {convertValue(point.avgCelsius).toFixed(1)}{unitLabel}
              </td>
              <td style={{ textAlign: 'right', padding: '4px 8px' }}>
                {convertValue(point.maxCelsius).toFixed(1)}{unitLabel}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
