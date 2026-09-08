import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm, symlink, readlink } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

test('resource assembly rejects missing web, ambiguous jars and wrong architecture, preserves runtime links and hashes', async () => {
  const { prepareResources } = await import('./prepare-resources.mjs');
  const root = await mkdtemp(join(tmpdir(), 'blob-resources-'));
  const client = join(root, 'client');
  const runtime = join(root, 'jdk');
  const options = { clientDir: client, runtimeDir: runtime, inspectRuntime: async () => 'arm64' };
  try {
    await mkdir(client);
    await assert.rejects(prepareResources(options), /web|前端/);
    await mkdir(join(client, 'dist'));
    await writeFile(join(client, 'dist/index.html'), 'web');
    await mkdir(join(root, 'server/target'), { recursive: true });
    await writeFile(join(root, 'server/target/a.jar'), 'jar');
    await writeFile(join(root, 'server/target/b.jar'), 'jar');
    await assert.rejects(prepareResources(options), /JAR/);
    await rm(join(root, 'server/target/b.jar'));
    await assert.rejects(prepareResources(options), /Java|runtime/);
    await mkdir(join(runtime, 'bin'), { recursive: true });
    await writeFile(join(runtime, 'bin/java'), 'executable', { mode: 0o755 });
    await writeFile(join(runtime, 'release'), 'JAVA_VERSION="21.0.10"\nOS_ARCH="aarch64"');
    await symlink('java', join(runtime, 'bin/java-link'));
    await assert.rejects(prepareResources({ ...options, inspectRuntime: async () => 'x86_64' }), /arm64/);
    const manifest = await prepareResources(options);
    assert.equal(await readFile(join(client, 'resources/web/index.html'), 'utf8'), 'web');
    assert.equal(await readlink(join(client, 'resources/runtime/Contents/Home/bin/java-link')), 'java');
    assert.ok(manifest.files.find(file => file.path === 'server/blob-server.jar' && /^[a-f0-9]{64}$/.test(file.sha256)));
    assert.deepEqual(await prepareResources(options), manifest);
  } finally { await rm(root, { recursive: true, force: true }); }
});
