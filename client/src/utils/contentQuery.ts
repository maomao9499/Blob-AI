import type { LocationQuery, LocationQueryRaw } from 'vue-router';

export const PAGE_SIZE = 20;
export const queryText = (query: LocationQuery, key: string): string => typeof query[key] === 'string' ? query[key] : '';
export const positiveId = (value: string): number | undefined => Number.isSafeInteger(Number(value)) && Number(value) > 0 ? Number(value) : undefined;
export const routeQuery = (query: object): LocationQueryRaw => {
  const result: LocationQueryRaw = {};
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '' && value !== false) { result[key] = String(value); }
  }
  return result;
};
