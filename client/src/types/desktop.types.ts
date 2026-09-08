export interface DesktopSettingsInput {
  dbUrl: string; dbUsername: string; dbPassword?: string;
  redisHost: string; redisPort: number; redisPassword?: string;
}
export interface DesktopSettingsView {
  dbUrl: string; dbUsername: string; redisHost: string; redisPort: number;
  dbPasswordConfigured: boolean; redisPasswordConfigured: boolean;
}
export interface DesktopBootstrap {
  setupRequired: boolean; apiBaseUrl: string | null; sessionToken: string | null;
  settings: DesktopSettingsView | null; error?: string;
}
export interface BlobDesktopApi {
  getBootstrap(): Promise<DesktopBootstrap>;
  saveSettings(input: DesktopSettingsInput): Promise<DesktopSettingsView>;
  startBackend(): Promise<DesktopBootstrap>;
  getDependencyStatus(): Promise<{ mysql: { status: string; message: string }; redis: { status: string; message: string } }>;
  onBackendExit?(listener: (message: string) => void): () => void;
}
