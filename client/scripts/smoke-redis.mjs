import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

// Explicit opt-in: this acceptance test temporarily stops the user's Homebrew Redis.
if (process.env.BLOB_REDIS_SMOKE_ALLOW_SERVICE_STOP !== 'yes') throw new Error('Set BLOB_REDIS_SMOKE_ALLOW_SERVICE_STOP=yes after approving temporary Redis interruption.');
const execute = promisify(execFile);
const base = 'http://127.0.0.1:18081/api/v1'; // Dedicated blob_test backend only.
let id;
let serviceTouched = false;
const call = async (path, method = 'GET', body) => {
  const response = await fetch(base + path, { method, headers: { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  const envelope = await response.json();
  assert.equal(response.ok, true, `API ${method} ${path}: ${envelope.code}`);
  return envelope.data;
};
const waitRedis = async (expected) => {
  for (let attempt = 0; attempt < 20; attempt++) {
    if ((await call('/system/dependencies')).redis.status === expected) return;
    await delay(500);
  }
  throw new Error(`Redis did not become ${expected}`);
};
const input = { title: `Redis recovery acceptance ${Date.now()}`, contentMd: 'before Redis outage', entryType: 'LEARNING', entryDate: '2026-09-08', tagIds: [] };
try {
  await waitRedis('UP');
  id = (await call('/journals', 'POST', input)).id;
  assert.equal((await call(`/journals/${id}`)).contentMd, input.contentMd);
  serviceTouched = true;
  await execute('brew', ['services', 'stop', 'redis']);
  await waitRedis('DOWN');
  console.log('PASS Redis stopped; dependency diagnostics report DOWN');
  input.contentMd = 'after Redis outage';
  await call(`/journals/${id}`, 'PUT', input);
  assert.equal((await call(`/journals/${id}`)).contentMd, input.contentMd);
  const client = fileURLToPath(new URL('..', import.meta.url));
  const result = await execute('npm', ['run', '--silent', 'e2e', '--', '--reporter=json'], { cwd: client, env: { ...process.env, BLOB_E2E_API_TARGET: 'http://127.0.0.1:18081', BLOB_E2E_CHANNEL: 'chrome' }, timeout: 300000, maxBuffer: 4 * 1024 * 1024 });
  const report = JSON.parse(result.stdout);
  assert.equal(report.stats.unexpected, 0);
  assert.equal(report.stats.flaky, 0);
  assert.equal(report.stats.skipped, 0);
  assert.ok(report.stats.expected >= 5, 'All M1 and M2 core browser flows must run');
  for (const milestone of ['m1-journal.spec.ts', 'm2-knowledge.spec.ts', 'm2-management.spec.ts']) {
    assert.ok(report.suites.some(suite => suite.file.endsWith(milestone)), `Missing ${milestone}`);
  }
  console.log(`PASS Redis DOWN: ${report.stats.expected} real-browser M1/M2 tests covering knowledge, promotion, relations, search, tags and images`);
  await execute('brew', ['services', 'start', 'redis']);
  await waitRedis('UP');
  assert.equal((await call(`/journals/${id}`)).contentMd, 'after Redis outage');
  console.log('PASS Redis restarted; cached record reflects the update made during outage');
} finally {
  // Restore even if assertions or the browser run fail. Never flush user Redis data.
  if (serviceTouched) await execute('brew', ['services', 'start', 'redis']);
  if (id) await call(`/journals/${id}`, 'DELETE');
}
