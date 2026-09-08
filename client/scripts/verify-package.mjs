import { access, readdir, readFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createManifest, inspectRuntime } from './prepare-resources.mjs';

const client = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const execute = promisify(execFile);
const appPath = join(client, 'out/Blob-darwin-arm64/Blob.app');
const resources = join(appPath, 'Contents/Resources');
const java = join(resources, 'runtime/Contents/Home/bin/java');

async function filesUnder(directory) {
  const files = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await filesUnder(path));
    else files.push(path);
  }
  return files;
}

try {
  for (const file of ['web/index.html', 'server/blob-server.jar', 'app.asar', 'manifest.json']) await access(join(resources, file));
  await access(java, constants.X_OK);
  if (await inspectRuntime(java) !== 'arm64') throw new Error('安装包 Java 架构不是 arm64');
  const { stderr, stdout } = await execute(java, ['-version'], { timeout: 10000 });
  if (!/version "21[.\-"]/.test(stderr + stdout)) throw new Error('安装包 Java 版本不是 21');
  const expected = JSON.parse(await readFile(join(resources, 'manifest.json'), 'utf8'));
  const actual = await createManifest(resources);
  const actualByPath = new Map(actual.files.map(file => [file.path, file]));
  for (const file of expected.files) if (JSON.stringify(actualByPath.get(file.path)) !== JSON.stringify(file)) throw new Error(`安装包资源校验失败: ${file.path}`);
  const { listPackage } = await import('@electron/asar');
  const archive = listPackage(join(resources, 'app.asar'));
  for (const file of ['/dist-electron/main.cjs', '/dist-electron/preload.cjs']) if (!archive.includes(file)) throw new Error(`app.asar 缺少 ${file}`);
  const artifacts = await filesUnder(join(client, 'out/make'));
  if (!artifacts.some(file => file.endsWith('.dmg')) || !artifacts.some(file => file.endsWith('.zip'))) throw new Error('缺少 DMG 或 ZIP 安装产物');
  console.log(`安装包校验通过：${appPath}\nJava 21 arm64、前端、JAR、preload、资源 SHA-256、DMG 和 ZIP 均已验证。`);
} catch (error) { console.error(error.message); process.exitCode = 1; }
