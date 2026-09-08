export interface DesktopSettingsInput {
  dbUrl: string;
  dbUsername: string;
  dbPassword?: string;
  redisHost: string;
  redisPort: number;
  redisPassword?: string;
}

export interface PublicDesktopSettings {
  dbUrl: string;
  dbUsername: string;
  redisHost: string;
  redisPort: number;
  dbPasswordConfigured: boolean;
  redisPasswordConfigured: boolean;
}

export interface DesktopBootstrap {
  setupRequired: boolean;
  apiBaseUrl: string | null;
  sessionToken: string | null;
  settings: PublicDesktopSettings | null;
  error?: string;
}

export interface DependencyStatus {
  status: 'UP' | 'DOWN' | 'NOT_CONFIGURED' | 'UNKNOWN';
  message: string;
}

export interface BlobDesktopApi {
  getBootstrap(): Promise<DesktopBootstrap>;
  saveSettings(input: DesktopSettingsInput): Promise<PublicDesktopSettings>;
  startBackend(): Promise<DesktopBootstrap>;
  getDependencyStatus(): Promise<{ mysql: DependencyStatus; redis: DependencyStatus }>;
  onBackendExit(listener: (message: string) => void): () => void;
}

export interface BackendConnection {
  apiBaseUrl: string;
  sessionToken: string;
  pid: number;
}
