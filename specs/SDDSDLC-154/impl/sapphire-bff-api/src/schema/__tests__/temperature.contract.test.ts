// GraphQL schema snapshot test for temperature types
// File: sapphire-bff-api/src/schema/__tests__/temperature.contract.test.ts

import { buildSchema, printSchema } from 'graphql';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Snapshot test for the temperature GraphQL schema extension.
 *
 * Verifies all new types, fields, queries, and mutations added by SDDSDLC-154 remain
 * structurally stable. If schema changes are intentional, update the snapshot with
 * `jest --updateSnapshot`.
 *
 * Run in the pre-merge CI stage to prevent accidental breaking changes.
 */
describe('temperature GraphQL schema contract', () => {
  const schemaPath = path.resolve(__dirname, '../../schema/temperature.graphql');
  const schemaSDL = fs.readFileSync(schemaPath, 'utf-8');

  it('should parse without errors', () => {
    expect(() => buildSchema(schemaSDL, { assumeValidSDL: true })).not.toThrow();
  });

  it('should contain TemperatureTrendPayload type', () => {
    expect(schemaSDL).toContain('type TemperatureTrendPayload');
  });

  it('should contain TemperatureDataPoint type', () => {
    expect(schemaSDL).toContain('type TemperatureDataPoint');
  });

  it('should extend Query with temperatureTrend', () => {
    expect(schemaSDL).toContain('temperatureTrend(');
  });

  it('should extend Mutation with ingestTemperature', () => {
    expect(schemaSDL).toContain('ingestTemperature(');
  });

  it('should extend Mutation with ingestTemperatureBatch', () => {
    expect(schemaSDL).toContain('ingestTemperatureBatch(');
  });

  it('should match schema snapshot', () => {
    expect(schemaSDL).toMatchSnapshot();
  });
});
