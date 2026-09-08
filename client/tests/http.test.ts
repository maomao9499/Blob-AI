import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it } from 'vitest';

import { ApiError, clearAccessToken, http, setAccessToken } from '@/api/http';

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
        authorizationHeader = config.headers.get('X-Blob-Desktop-Token');
        return createResponse(config);
      },
    });

    expect(authorizationHeader).toBe('desktop-token');
    expect(localStorage.length).toBe(0);
  });

  it('preserves the backend conflict message and trace from an HTTP 409 response', async () => {
    const request = http.delete('/tags/42', {
      adapter: async config => {
        throw new AxiosError('Request failed with status code 409', 'ERR_BAD_REQUEST', config, undefined, {
          config, status: 409, statusText: 'Conflict', headers: {},
          data: { code: 'CONFLICT', message: '标签仍被日志使用，无法删除', data: null, traceId: 'trace-conflict' },
        });
      },
    });
    await expect(request).rejects.toBeInstanceOf(ApiError);
    await expect(request).rejects.toMatchObject({ code: 'CONFLICT', message: '标签仍被日志使用，无法删除', traceId: 'trace-conflict' });
  });
});
