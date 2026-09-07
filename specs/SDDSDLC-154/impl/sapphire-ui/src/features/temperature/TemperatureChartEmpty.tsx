// File: sapphire-ui/src/features/temperature/TemperatureChartEmpty.tsx

import React from 'react';

/**
 * Empty-state placeholder shown when no temperature data exists for the selected range.
 */
export function TemperatureChartEmpty(): React.ReactElement {
  return (
    <div
      role="status"
      aria-label="No temperature data"
      style={{ padding: '2rem', textAlign: 'center', color: 'var(--color-muted, #57606a)' }}
    >
      <p>No temperature data yet. Connect a device to get started.</p>
    </div>
  );
}
