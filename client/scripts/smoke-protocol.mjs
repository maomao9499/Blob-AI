import { _electron as electron } from '@playwright/test';
import { mkdtemp, mkdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const dataDir = await mkdtemp(join(tmpdir(), 'blob-protocol-smoke-'));
const application = await electron.launch({ args: [resolve('dist-electron/main.cjs')], env: { ...process.env, BLOB_USER_DATA_DIR: dataDir }, timeout: 30_000 });
const diagnostics = [];
application.process().stdout?.on('data', chunk => diagnostics.push(`stdout: ${chunk.toString()}`));
application.process().stderr?.on('data', chunk => diagnostics.push(`stderr: ${chunk.toString()}`));
application.process().on('exit', (code, signal) => diagnostics.push(`exit: code=${code} signal=${signal}`));
try {
  const page = await application.firstWindow();
  page.on('console', message => diagnostics.push(`renderer ${message.type()}: ${message.text()}`));
  page.on('pageerror', error => diagnostics.push(`renderer error: ${error.message}`));
  await page.getByRole('heading', { name: '设置与依赖' }).waitFor({ timeout: 20_000 });
  await page.getByRole('heading', { name: '本地服务配置' }).waitFor();
  console.log(JSON.stringify({ url: page.url(), title: await page.title(), setupVisible: true }));
  await mkdir('out/smoke', { recursive: true });
  await page.screenshot({ path: 'out/smoke/desktop-setup.png' });
} catch (error) {
  console.error(diagnostics.join('\n'));
  throw error;
} finally { await application.close().catch(() => undefined); }
