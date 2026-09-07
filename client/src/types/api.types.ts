export interface ApiResponse<T> {
  readonly code: string;
  readonly message: string;
  readonly data: T;
  readonly traceId: string;
}

export interface DependencyStatus {
  readonly status: 'UP' | 'DOWN' | 'NOT_CONFIGURED';
  readonly message: string;
}

export interface DependencyStatuses {
  readonly mysql: DependencyStatus;
  readonly redis: DependencyStatus;
}
