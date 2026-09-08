import { spawn, type ChildProcess, type SpawnOptions } from 'node:child_process';
import { createServer } from 'node:net';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { randomBytes } from 'node:crypto';
import { StringDecoder } from 'node:string_decoder';
import type { DesktopSettingsInput, BackendConnection } from './desktop-api.cjs';
import { RotatingLog } from './RotatingLog.cjs';

interface Options {
  javaPath: string;
  jarPath: string;
  uploadDir: string;
  logDir: string;
  startupTimeoutMs?: number;
  shutdownTimeoutMs?: number;
  allowDevOrigin?: boolean;
  spawnProcess?: (command: string, args: string[], options: SpawnOptions) => ChildProcess;
  findFreePort?: () => Promise<number>;
  fetchHealth?: (url: string, signal: AbortSignal) => Promise<boolean>;
  onUnexpectedExit?: (message: string) => void;
}

export function findFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (!address || typeof address === 'string') { server.close(); reject(new Error('无法分配后端端口')); return; }
      server.close(error => error ? reject(error) : resolve(address.port));
    });
  });
}

export class BackendProcessManager {
  private child: ChildProcess | null = null;
  private connection: BackendConnection | null = null;
  private starting: Promise<BackendConnection> | null = null;
  private stopping: Promise<void> | null = null;
  private startupAbort: AbortController | null = null;
  private expectedExit = false;
  constructor(private readonly options: Options) {}
  getConnection(): BackendConnection | null { return this.connection; }

  start(settings: DesktopSettingsInput): Promise<BackendConnection> {
    if (this.connection) return Promise.resolve(this.connection);
    if (this.starting) return this.starting;
    this.starting = this.startInternal(settings).finally(() => { this.starting = null; });
    return this.starting;
  }

  private async startInternal(settings: DesktopSettingsInput): Promise<BackendConnection> {
    if (this.stopping) await this.stopping;
    const controller = new AbortController();
    this.startupAbort = controller;
    this.expectedExit = false;
    const port = await (this.options.findFreePort ?? findFreePort)();
    await mkdir(this.options.uploadDir, { recursive: true, mode: 0o700 });
    if (controller.signal.aborted) throw new Error('后端启动已取消');
    const token = randomBytes(32).toString('hex');
    const origin = `http://127.0.0.1:${port}`;
    const log = new RotatingLog(join(this.options.logDir, 'backend.log'), [settings.dbPassword ?? '', settings.redisPassword ?? '', token, settings.dbUrl]);
    // Only selected inherited process variables are needed to launch Java.
    const env: NodeJS.ProcessEnv = {};
    for (const key of ['PATH', 'HOME', 'TMPDIR', 'LANG', 'LC_ALL', 'USER']) if (process.env[key]) env[key] = process.env[key];
    Object.assign(env, { BLOB_DB_URL: settings.dbUrl, BLOB_DB_USERNAME: settings.dbUsername, BLOB_DB_PASSWORD: settings.dbPassword ?? '', BLOB_REDIS_HOST: settings.redisHost, BLOB_REDIS_PORT: String(settings.redisPort), BLOB_REDIS_PASSWORD: settings.redisPassword ?? '', BLOB_UPLOAD_DIR: this.options.uploadDir, BLOB_DESKTOP_TOKEN: token });
    if (this.options.allowDevOrigin) env.BLOB_ALLOW_DEV_ORIGIN = 'true';
    const child = (this.options.spawnProcess ?? spawn)(this.options.javaPath, ['-jar', this.options.jarPath, '--server.address=127.0.0.1', `--server.port=${port}`], { env, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true });
    this.child = child;
    let failed = false;
    child.once('error', () => { failed = true; });
    child.once('exit', () => {
      failed = true;
      const wasReady = this.connection !== null;
      if (this.child === child) { this.child = null; this.connection = null; }
      if (!this.expectedExit && wasReady) this.options.onUnexpectedExit?.('本地后端意外退出，请在设置页重试启动');
    });
    for (const stream of [child.stdout, child.stderr]) {
      const decoder = new StringDecoder('utf8');
      let pending = '';
      let discarding = false;
      stream?.on('data', (chunk: Buffer) => {
        pending += decoder.write(chunk);
        let newline: number;
        while ((newline = pending.indexOf('\n')) >= 0) {
          const line = pending.slice(0, newline + 1);
          pending = pending.slice(newline + 1);
          if (!discarding && line.length <= 8192) void log.write(line);
          discarding = false;
        }
        if (pending.length > 8192) { pending = ''; discarding = true; }
      });
      stream?.on('end', () => { pending += decoder.end(); if (!discarding && pending) void log.write(`${pending}\n`); });
    }
    const deadline = Date.now() + (this.options.startupTimeoutMs ?? 30000);
    const health = this.options.fetchHealth ?? (async (url: string, signal: AbortSignal) => { const response = await fetch(url, { signal, redirect: 'error' }); const data = await response.json() as { status?: string }; return response.ok && data.status === 'UP'; });
    let delay = 100;
    try {
      while (Date.now() < deadline && !controller.signal.aborted && !failed) {
        const signal = AbortSignal.any([controller.signal, AbortSignal.timeout(Math.max(1, Math.min(1500, deadline - Date.now())))]);
        const ready = await health(`${origin}/actuator/health/liveness`, signal).catch(() => false);
        if (ready && !failed && !controller.signal.aborted && child.pid) {
          this.connection = { apiBaseUrl: origin, sessionToken: token, pid: child.pid };
          return this.connection;
        }
        await new Promise<void>(resolve => setTimeout(resolve, Math.min(delay, Math.max(0, deadline - Date.now()))));
        delay = Math.min(1000, delay * 2);
      }
      throw new Error(controller.signal.aborted ? '后端启动已取消' : failed ? '后端启动失败，请检查 MySQL 配置和本地日志' : '后端启动超时，请检查 MySQL 配置后重试');
    } catch (error) {
      await this.stop();
      throw error;
    } finally { if (this.startupAbort === controller) this.startupAbort = null; }
  }

  stop(): Promise<void> {
    this.startupAbort?.abort();
    if (this.stopping) return this.stopping;
    this.expectedExit = true;
    this.connection = null;
    const child = this.child;
    if (!child || child.exitCode !== null || child.signalCode !== null) return Promise.resolve();
    this.stopping = new Promise<void>(resolve => {
      const timer = setTimeout(() => { if (child.exitCode === null && child.signalCode === null) child.kill('SIGKILL'); }, this.options.shutdownTimeoutMs ?? 8000);
      child.once('exit', () => { clearTimeout(timer); resolve(); });
      child.once('error', () => { clearTimeout(timer); resolve(); });
      child.kill('SIGTERM');
    }).finally(() => { if (this.child === child) this.child = null; this.stopping = null; });
    return this.stopping;
  }
}
