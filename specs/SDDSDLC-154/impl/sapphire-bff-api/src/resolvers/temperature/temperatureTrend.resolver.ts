// temperatureTrend query resolver
// File: sapphire-bff-api/src/resolvers/temperature/temperatureTrend.resolver.ts

import { AppContext } from '../../context';
import { TemperatureTrendPayload } from './temperatureDataLoader';

interface TemperatureTrendArgs {
  range: string;
  deviceSourceId?: string;
  from?: string;
  to?: string;
}

/**
 * Resolver for Query.temperatureTrend.
 *
 * Extracts the userId from the validated JWT subject claim, then delegates data
 * fetching to the temperatureTrend DataLoader. JWT validation is performed by
 * the context middleware (authMiddleware) before any resolver runs — if the token
 * is absent or invalid, the context carries no userId and this resolver returns a
 * 401 AuthenticationError.
 *
 * @param _parent   unused root parent value
 * @param args      query arguments (range, optional from/to/deviceSourceId)
 * @param context   request context containing the validated JWT and DataLoader instances
 * @returns TemperatureTrendPayload with dataPoints array (never null, may be empty)
 */
export async function temperatureTrend(
  _parent: unknown,
  args: TemperatureTrendArgs,
  context: AppContext
): Promise<TemperatureTrendPayload> {
  const userId: string | undefined = context.jwt?.sub;

  if (!userId) {
    throw new Error('UNAUTHENTICATED: valid JWT required');
  }

  return context.loaders.temperatureTrend.load({
    userId,
    range: args.range,
    from: args.from,
    to: args.to,
    deviceSourceId: args.deviceSourceId,
  });
}
