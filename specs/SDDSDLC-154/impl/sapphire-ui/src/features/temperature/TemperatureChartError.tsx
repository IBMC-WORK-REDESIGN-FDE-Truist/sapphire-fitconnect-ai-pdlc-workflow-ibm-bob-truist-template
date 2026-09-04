// File: sapphire-ui/src/features/temperature/TemperatureChartError.tsx

import React from 'react';

interface TemperatureChartErrorProps {
  /** Callback invoked when the user clicks the "Try again" button. */
  onRetry: () => void;
}

/**
 * Error state shown when the temperature trend query fails.
 * Provides a "Try again" button that triggers a query refetch.
 */
export function TemperatureChartError({ onRetry }: TemperatureChartErrorProps): React.ReactElement {
  return (
    <div
      role="alert"
      aria-label="Failed to load temperature data"
      style={{ padding: '2rem', textAlign: 'center', color: 'var(--color-danger, #cf2a2a)' }}
    >
      <p>Failed to load temperature data.</p>
      <button
        type="button"
        onClick={onRetry}
        style={{
          marginTop: '0.5rem',
          padding: '0.5rem 1rem',
          cursor: 'pointer',
          borderRadius: 4,
          border: '1px solid currentColor',
          background: 'transparent',
          color: 'inherit',
        }}
      >
        Try again
      </button>
    </div>
  );
}
