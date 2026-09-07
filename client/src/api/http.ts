import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';

import type { ApiResponse } from '@/types/api.types';

const API_BASE_URL = window.blobDesktop?.apiBaseUrl ?? '/api/v1';
const SUCCESS_CODE = 'OK';

let accessToken: string | null = null;

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
  baseURL: API_BASE_URL,
  timeout: 10_000,
});

axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    if (accessToken) {
      config.headers.set('Authorization', `Bearer ${accessToken}`);
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
    throw new Error(getErrorMessage(error));
  }
};

export const setAccessToken = (token: string): void => {
  accessToken = token;
};

export const clearAccessToken = (): void => {
  accessToken = null;
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
