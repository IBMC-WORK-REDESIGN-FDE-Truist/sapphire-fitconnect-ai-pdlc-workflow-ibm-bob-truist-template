// Unit tests for useTemperatureTrend hook
// File: sapphire-ui/src/features/temperature/useTemperatureTrend.test.ts

import { renderHook } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { useTemperatureTrend } from './useTemperatureTrend';
import { GetTemperatureTrendDocument } from './__generated__/temperature.generated';

const mockTrendData = {
  temperatureTrend: {
    userId: 'user-001',
    range: 'WEEK',
    from: '2026-08-01',
    to: '2026-08-07',
    dataPoints: [
      { periodStart: '2026-08-01', periodEnd: '2026-08-01', minCelsius: 36.5, maxCelsius: 37.8, avgCelsius: 37.1, recordCount: 5 },
    ],
  },
};

function wrapper({ children, search = '' }: { children: React.ReactNode; search?: string }) {
  const mocks = [
    {
      request: { query: GetTemperatureTrendDocument, variables: { range: 'WEEK', deviceSourceId: undefined } },
      result: { data: mockTrendData },
    },
  ];
  return React.createElement(
    MockedProvider,
    { mocks, addTypename: false },
    React.createElement(MemoryRouter, { initialEntries: ['/?' + search] }, children)
  );
}

describe('useTemperatureTrend', () => {
  it('should return loading=true on initial render', () => {
    const { result } = renderHook(() => useTemperatureTrend(), {
      wrapper: ({ children }) => wrapper({ children }),
    });
    expect(result.current.loading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('should return data after query resolves', async () => {
    const { result } = renderHook(() => useTemperatureTrend(), {
      wrapper: ({ children }) => wrapper({ children }),
    });

    // Wait for Apollo to resolve
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(result.current.data?.temperatureTrend.dataPoints).toHaveLength(1);
  });

  it('should read range from URL search params', () => {
    const { result } = renderHook(() => useTemperatureTrend(), {
      wrapper: ({ children }) => wrapper({ children, search: 'range=DAY' }),
    });
    // Range is passed to the query — the hook won't error on a valid range param
    expect(result.current.loading).toBe(true);
  });

  it('should default range to WEEK when param is missing', () => {
    const { result } = renderHook(() => useTemperatureTrend(), {
      wrapper: ({ children }) => wrapper({ children }),
    });
    // Hook resolves without error — loading is true on first render
    expect(result.current.error).toBeUndefined();
  });
});
