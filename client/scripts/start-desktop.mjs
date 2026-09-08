import { spawn } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const CLIENT_DIRECTORY = resolve(dirname(fileURLToPath(import.meta.url)), '..'); // 前端工程目录。
const DEVELOPMENT_URL = 'http://127.0.0.1:5173'; // Electron 开发渲染地址。
const RENDERER_STARTUP_TIMEOUT_MS = 15_000; // Vite 启动等待上限。
const PROCESS_SHUTDOWN_TIMEOUT_MS = 2_000; // 子进程优雅退出等待上限。
const POLL_INTERVAL_MS = 100; // Vite 可用性检查间隔。
const NPM_COMMAND = process.platform === 'win32' ? 'npm.cmd' : 'npm'; // 当前平台 npm 命令。

const delay = (milliseconds) => new Promise(resolveDelay => setTimeout(resolveDelay, milliseconds));
const hasExited = child => child.exitCode !== null || child.signalCode !== null;

const observeExit = child => {
  if (hasExited(child)) {
    return Promise.resolve({ code: child.exitCode, signal: child.signalCode });
  }
  return new Promise((resolveExit, rejectExit) => {
    const handleError = error => {
      child.removeListener('exit', handleExit);
      rejectExit(error);
    };
    const handleExit = (code, signal) => {
      child.removeListener('error', handleError);
      resolveExit({ code, signal });
    };
    child.once('error', handleError);
    child.once('exit', handleExit);
  });
};

const waitForSuccessfulExit = async (child, label) => {
  const { code, signal } = await observeExit(child);
  if (code === 0) {
    return code;
  }
  throw new Error(`${label}失败（code=${code ?? 'null'}, signal=${signal ?? 'none'}）`);
};

const raceWithRenderer = async (stage, rendererExit) => {
  const result = await Promise.race([
    stage.then(value => ({ source: 'stage', value })),
    rendererExit.then(exit => ({ source: 'renderer', exit })),
  ]);
  if (result.source === 'renderer') {
    throw new Error(`Vite 开发服务器提前退出（code=${result.exit.code ?? 'null'}, signal=${result.exit.signal ?? 'none'}）`);
  }
  return result.value;
};

export const waitForDevServer = async (url = DEVELOPMENT_URL) => {
  const deadline = Date.now() + RENDERER_STARTUP_TIMEOUT_MS;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(1_000) });
      if (response.ok) {
        return;
      }
    } catch {
      // Vite 尚未监听时继续等待，最终由统一超时错误说明原因。
    }
    await delay(POLL_INTERVAL_MS);
  }
  throw new Error(`等待 Vite 启动超时：${url}`);
};

export const terminateProcessTree = async child => {
  if (!child || hasExited(child)) {
    return;
  }
  const exit = observeExit(child).catch(() => undefined);
  try {
    if (process.platform !== 'win32' && child.pid) {
      process.kill(-child.pid, 'SIGTERM');
    } else {
      child.kill('SIGTERM');
    }
  } catch {
    child.kill('SIGTERM');
  }
  await Promise.race([exit, delay(PROCESS_SHUTDOWN_TIMEOUT_MS)]);
  if (!hasExited(child)) {
    try {
      if (process.platform !== 'win32' && child.pid) {
        process.kill(-child.pid, 'SIGKILL');
      } else {
        child.kill('SIGKILL');
      }
    } catch {
      child.kill('SIGKILL');
    }
    await exit;
  }
};

export const startDesktop = async ({
  spawnProcess = spawn,
  waitForRenderer = waitForDevServer,
  terminateProcess = terminateProcessTree,
} = {}) => {
  const commonOptions = { cwd: CLIENT_DIRECTORY, env: process.env, stdio: 'inherit' };
  let renderer;
  let activeChild;
  const stopChildren = signal => {
    if (activeChild && activeChild !== renderer) {
      void terminateProcess(activeChild, signal);
    }
    if (renderer) {
      void terminateProcess(renderer, signal);
    }
  };
  const handleInterrupt = () => stopChildren('SIGINT');
  const handleTermination = () => stopChildren('SIGTERM');
  process.once('SIGINT', handleInterrupt);
  process.once('SIGTERM', handleTermination);

  try {
    renderer = spawnProcess(NPM_COMMAND, ['run', 'dev'], { ...commonOptions, detached: process.platform !== 'win32' });
    activeChild = renderer;
    const rendererExit = observeExit(renderer);
    await raceWithRenderer(waitForRenderer(DEVELOPMENT_URL), rendererExit);

    activeChild = spawnProcess(NPM_COMMAND, ['run', 'build:electron'], commonOptions);
    await raceWithRenderer(waitForSuccessfulExit(activeChild, 'Electron 主进程编译'), rendererExit);

    activeChild = spawnProcess(NPM_COMMAND, ['exec', '--', 'electron', '.', '--blob-dev'], commonOptions);
    return await raceWithRenderer(waitForSuccessfulExit(activeChild, 'Electron'), rendererExit);
  } finally {
    process.removeListener('SIGINT', handleInterrupt);
    process.removeListener('SIGTERM', handleTermination);
    if (activeChild && activeChild !== renderer) {
      await terminateProcess(activeChild);
    }
    await terminateProcess(renderer);
  }
};

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  startDesktop().catch(error => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  });
}
