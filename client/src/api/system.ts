import { http } from '@/api/http';
import type { DependencyStatuses } from '@/types/api.types';

interface PingResponse {
  readonly status: string;
}

export const systemApi = {
  getDependencies: (): Promise<DependencyStatuses> =>
    http.get<DependencyStatuses>('/system/dependencies'),
  ping: (): Promise<PingResponse> => http.get<PingResponse>('/system/ping'),
};
