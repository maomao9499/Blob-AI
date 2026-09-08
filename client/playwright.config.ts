import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  workers: 1,
  use: { baseURL: 'http://127.0.0.1:5174', channel: process.env.BLOB_E2E_CHANNEL || undefined, headless: true, viewport: { width: 1440, height: 1000 }, trace: 'retain-on-failure' },
  webServer: { command: 'npm run dev -- --port 5174', url: 'http://127.0.0.1:5174', reuseExistingServer: false, env: { BLOB_DEV_API_TARGET: process.env.BLOB_E2E_API_TARGET ?? 'http://127.0.0.1:18080' } },
});
