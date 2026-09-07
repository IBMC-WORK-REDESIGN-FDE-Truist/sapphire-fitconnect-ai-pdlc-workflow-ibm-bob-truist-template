/**
 * Temperature unit conversion utilities.
 * File: sapphire-ui/src/features/temperature/temperatureUtils.ts
 *
 * Pure functions with no side effects. Safe to use in tests and renderers.
 */

/**
 * Converts a temperature value from Celsius to Fahrenheit.
 *
 * @param c - temperature in degrees Celsius
 * @returns equivalent temperature in degrees Fahrenheit
 */
export function celsiusToFahrenheit(c: number): number {
  return (c * 9) / 5 + 32;
}

/**
 * Converts a temperature value from Fahrenheit to Celsius.
 *
 * @param f - temperature in degrees Fahrenheit
 * @returns equivalent temperature in degrees Celsius
 */
export function fahrenheitToCelsius(f: number): number {
  return ((f - 32) * 5) / 9;
}

/**
 * Formats a temperature value to one decimal place with its unit symbol.
 *
 * @param value - temperature value
 * @param unit  - display unit
 * @returns formatted string, e.g. "37.2°C" or "99.0°F"
 */
export function formatTemperature(value: number, unit: 'CELSIUS' | 'FAHRENHEIT'): string {
  const symbol = unit === 'CELSIUS' ? '°C' : '°F';
  return `${value.toFixed(1)}${symbol}`;
}
