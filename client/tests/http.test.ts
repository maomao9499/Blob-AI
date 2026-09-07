import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it } from 'vitest';

import { clearAccessToken, http, setAccessToken } from '@/api/http';

interface ProbeResponse {
  readonly status: string;
}

const createResponse = (
  config: InternalAxiosRequestConfig,
): AxiosResponse => ({
  config,
  data: {
    code: 'OK',
    message: 'success',
    data: { status: 'UP' },
    traceId: 'trace-test',
  },
  headers: {},
  status: 200,
  statusText: 'OK',
});

describe('http', () => {
  afterEach(() => {
    clearAccessToken();
  });

  it('unwraps the shared API envelope', async () => {
    const response = await http.get<ProbeResponse>('/system/ping', {
      adapter: async (config) => createResponse(config),
    });

    expect(response).toEqual({ status: 'UP' });
  });

  it('adds the in-memory desktop token without persisting it', async () => {
    let authorizationHeader: unknown;
    setAccessToken('desktop-token');

    await http.get<ProbeResponse>('/system/ping', {
      adapter: async (config) => {
        authorizationHeader = config.headers.get('Authorization');
        return createResponse(config);
      },
    });

    expect(authorizationHeader).toBe('Bearer desktop-token');
    expect(localStorage.length).toBe(0);
  });
});
