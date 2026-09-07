// State UI components for the temperature chart
// File: sapphire-ui/src/features/temperature/TemperatureChartSkeleton.tsx

import React from 'react';

/**
 * Loading skeleton for the temperature trend chart.
 * Dimensions match the live chart to prevent layout shift on load.
 */
export function TemperatureChartSkeleton(): React.ReactElement {
  return (
    <div
      role="status"
      aria-label="Loading temperature data"
      style={{
        width: '100%',
        height: 280,
        backgroundColor: 'var(--color-surface, #f7f8fa)',
        borderRadius: 8,
        animation: 'pulse 1.5s ease-in-out infinite',
      }}
    />
  );
}
