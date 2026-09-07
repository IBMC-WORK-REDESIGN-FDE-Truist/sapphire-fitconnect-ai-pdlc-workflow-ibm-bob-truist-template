// Unit tests for TemperatureChart component
// File: sapphire-ui/src/features/temperature/TemperatureChart.test.tsx

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TemperatureChart } from './TemperatureChart';
import { TemperatureTrendPayload } from './temperature.types';

const samplePayload: TemperatureTrendPayload = {
  userId: 'user-001',
  range: 'WEEK',
  from: '2026-08-01',
  to: '2026-08-07',
  dataPoints: [
    { periodStart: '2026-08-01', periodEnd: '2026-08-01', minCelsius: 36.5, maxCelsius: 37.8, avgCelsius: 37.1, recordCount: 5 },
    { periodStart: '2026-08-02', periodEnd: '2026-08-02', minCelsius: 36.8, maxCelsius: 38.0, avgCelsius: 37.3, recordCount: 3 },
  ],
};

const emptyPayload: TemperatureTrendPayload = {
  userId: 'user-001',
  range: 'WEEK',
  from: '2026-08-01',
  to: '2026-08-07',
  dataPoints: [],
};

describe('TemperatureChart', () => {
  it('should render data points in Celsius', () => {
    render(<TemperatureChart data={samplePayload} unit="CELSIUS" />);
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('2026-08-01')).toBeInTheDocument();
    // Avg for first data point: 37.1°C
    expect(screen.getByText('37.1°C')).toBeInTheDocument();
  });

  it('should convert values to Fahrenheit when unit=FAHRENHEIT', () => {
    render(<TemperatureChart data={samplePayload} unit="FAHRENHEIT" />);
    // 37.1°C → 98.78 → 98.8°F
    expect(screen.getByText('98.8°F')).toBeInTheDocument();
  });

  it('should render a fallback message when dataPoints is empty', () => {
    render(<TemperatureChart data={emptyPayload} unit="CELSIUS" />);
    expect(screen.getByText('No data points to display.')).toBeInTheDocument();
  });

  it('should have an accessible aria-label with the unit', () => {
    render(<TemperatureChart data={samplePayload} unit="FAHRENHEIT" />);
    expect(
      screen.getByRole('region', { name: /Temperature trend chart \(°F\)/i })
    ).toBeInTheDocument();
  });
});
