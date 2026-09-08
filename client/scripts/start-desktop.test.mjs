import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { test } from 'node:test';

class FakeChildProcess extends EventEmitter {
  constructor(pid) {
    super();
    this.pid = pid;
    this.exitCode = null;
    this.signalCode = null;
    this.killed = false;
  }

  finish(code) {
    this.exitCode = code;
    this.emit('exit', code, null);
  }

  kill(signal = 'SIGTERM') {
    if (this.exitCode !== null || this.signalCode !== null) {
      return false;
    }
    this.killed = true;
    this.signalCode = signal;
    queueMicrotask(() => this.emit('exit', null, signal));
    return true;
  }
}

const loadLauncher = async () => {
  try {
    return await import('./start-desktop.mjs');
  } catch (error) {
    assert.fail(`缺少桌面开发启动协调器: ${error instanceof Error ? error.message : String(error)}`);
  }
};

test('desktop launcher waits for Vite, runs the Electron build, and cleans the renderer after Electron closes', async () => {
  const { startDesktop } = await loadLauncher();
  const launches = [];
  let rendererReady = false;
  const spawnProcess = (command, args) => {
    const child = new FakeChildProcess(launches.length + 100);
    launches.push({ command, args, child });
    if (launches.length > 1) {
      queueMicrotask(() => child.finish(0));
    }
    return child;
  };

  const exitCode = await startDesktop({
    spawnProcess,
    waitForRenderer: async () => {
      assert.equal(launches.length, 1);
      rendererReady = true;
    },
    terminateProcess: async child => child.kill(),
  });

  assert.equal(exitCode, 0);
  assert.equal(rendererReady, true);
  assert.deepEqual(launches.map(({ command, args }) => [command, args]), [
    ['npm', ['run', 'dev']],
    ['npm', ['run', 'build:electron']],
    ['npm', ['exec', '--', 'electron', '.', '--blob-dev']],
  ]);
  assert.equal(launches[0].child.killed, true);
});

test('desktop launcher stops before compiling when Vite exits during startup', async () => {
  const { startDesktop } = await loadLauncher();
  const launches = [];
  const spawnProcess = (command, args) => {
    const child = new FakeChildProcess(launches.length + 200);
    launches.push({ command, args, child });
    queueMicrotask(() => child.finish(1));
    return child;
  };

  await assert.rejects(
    startDesktop({
      spawnProcess,
      waitForRenderer: () => new Promise(() => undefined),
      terminateProcess: async child => child.kill(),
    }),
    /Vite.*退出/,
  );
  assert.equal(launches.length, 1);
});
