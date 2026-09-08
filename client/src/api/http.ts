import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';

import type { ApiResponse } from '@/types/api.types';
import { pinia } from '@/stores/runtime';
import { useDesktopStore } from '@/stores/modules/desktop';

const SUCCESS_CODE = 'OK';

export class ApiError extends Error {
  readonly code: string;
  readonly traceId: string;

  constructor(code: string, message: string, traceId: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.traceId = traceId;
  }
}

const axiosClient = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
});

axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const desktop = useDesktopStore(pinia);
    config.baseURL = desktop.apiBaseUrl;
    if (desktop.sessionToken) {
      config.headers.set('X-Blob-Desktop-Token', desktop.sessionToken);
    }
    return config;
  },
);

const getErrorMessage = (error: unknown): string => {
  if (error instanceof AxiosError && error.code === 'ECONNABORTED') {
    return '后端响应超时，请稍后重试';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '请求失败，请稍后重试';
};

const request = async <T>(config: AxiosRequestConfig): Promise<T> => {
  try {
    const response = await axiosClient.request<ApiResponse<T>>(config);
    const envelope = response.data;
    if (envelope.code !== SUCCESS_CODE) {
      throw new ApiError(envelope.code, envelope.message, envelope.traceId);
    }
    return envelope.data;
  } catch (error: unknown) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (axios.isAxiosError<ApiResponse<unknown>>(error) && error.response?.data?.code) {
      const body = error.response.data;
      throw new ApiError(body.code, body.message, body.traceId);
    }
    throw new Error(getErrorMessage(error));
  }
};

export const setAccessToken = (token: string): void => {
  const desktop = useDesktopStore(pinia);
  desktop.configure(desktop.apiBaseUrl, token);
};

export const clearAccessToken = (): void => {
  useDesktopStore(pinia).configure('/api/v1', null);
};

export const configureBackend = (baseUrl: string, token: string | null): void => {
  useDesktopStore(pinia).configure(baseUrl, token);
};

export const http = {
  get: <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    request<T>({ ...config, method: 'GET', url }),
  post: <T, B>(url: string, body: B, config?: AxiosRequestConfig<B>): Promise<T> =>
    request<T>({ ...config, data: body, method: 'POST', url }),
  put: <T, B>(url: string, body: B, config?: AxiosRequestConfig<B>): Promise<T> =>
    request<T>({ ...config, data: body, method: 'PUT', url }),
  delete: <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    request<T>({ ...config, method: 'DELETE', url }),
};
