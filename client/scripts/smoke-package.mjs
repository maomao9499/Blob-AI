import { _electron as electron, expect } from '@playwright/test';
import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';

const execute = promisify(execFile);
const client = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const password = process.env.BLOB_SMOKE_DB_PASSWORD;
if (!password) throw new Error('Set BLOB_SMOKE_DB_PASSWORD in the calling environment before packaged smoke.');
const testDbUrl = process.env.BLOB_SMOKE_DB_URL ?? 'jdbc:mysql://127.0.0.1:3306/blob_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai';
if (!/^jdbc:mysql:\/\/127\.0\.0\.1:\d+\/blob_test(?:\?|$)/.test(testDbUrl)) throw new Error('Packaged smoke must use loopback blob_test.');
const { BLOB_SMOKE_DB_PASSWORD: _password, ...launchEnvironment } = process.env;
const temporary = await mkdtemp(join(tmpdir(), 'blob-package-smoke-'));
const mount = join(temporary, 'volume');
const installed = join(temporary, 'installed/Blob.app');
const userData = join(temporary, 'user-data');
const screenshotDirectory = join(client, 'out/smoke');
const title = `桌面安装包验证 ${Date.now()}`;
const image = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jD1sAAAAASUVORK5CYII=', 'base64');
let mounted = false;
let application;
let page;
let javaPid;
let journalId;
const knowledgeIds = [];
let relationId;
let imageUrl;
let failed = false;
let stage = 'DMG installation';

async function assertChildExited(pid) {
  if (!pid) return;
  await expect.poll(() => {
    try { process.kill(pid, 0); return false; }
    catch (error) { if (error.code === 'ESRCH') return true; throw error; }
  }, { timeout: 12000, message: 'The tracked Java child must exit when Blob quits' }).toBe(true);
}

async function launch() {
  application = await electron.launch({ executablePath: join(installed, 'Contents/MacOS/Blob'), env: { ...launchEnvironment, BLOB_USER_DATA_DIR: userData }, timeout: 30000 });
  page = await application.firstWindow();
  page.setDefaultTimeout(15000);
  const preferences = await application.evaluate(({ BrowserWindow }) => {
    const values = BrowserWindow.getAllWindows()[0].webContents.getLastWebPreferences();
    return { nodeIntegration: values.nodeIntegration, contextIsolation: values.contextIsolation, sandbox: values.sandbox };
  });
  assert.deepEqual(preferences, { nodeIntegration: false, contextIsolation: true, sandbox: true });
}

async function recordJavaPid() {
  javaPid = await application.evaluate(async () => {
    const { execFileSync } = process.getBuiltinModule('node:child_process');
    const lines = execFileSync('/bin/ps', ['-axo', 'pid=,ppid=,comm='], { encoding: 'utf8' }).trim().split('\n');
    const children = lines.map(line => line.trim().match(/^(\d+)\s+(\d+)\s+(.+)$/)).filter(Boolean);
    const java = children.find(match => Number(match[2]) === process.pid && match[3].endsWith('/java'));
    return java ? Number(java[1]) : null;
  });
  assert.ok(javaPid, 'A tracked packaged Java child must exist');
}

async function currentApi(path, method = 'GET', body) {
  return page.evaluate(async ({ path, method, body }) => {
    const connection = await window.blobDesktop.getBootstrap();
    const response = await fetch(`${connection.apiBaseUrl}${path}`, { method, headers: { 'X-Blob-Desktop-Token': connection.sessionToken, 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
    return { status: response.status, body: response.status === 204 ? null : await response.json() };
  }, { path, method, body });
}

async function quit() {
  if (application) { await application.close(); application = undefined; page = undefined; }
  await assertChildExited(javaPid);
  javaPid = undefined;
}

try {
  await mkdir(mount);
  await mkdir(join(temporary, 'installed'));
  await mkdir(screenshotDirectory, { recursive: true });
  await execute('/usr/bin/hdiutil', ['attach', '-readonly', '-nobrowse', '-mountpoint', mount, join(client, 'out/make/Blob.dmg')], { timeout: 30000 });
  mounted = true;
  await execute('/usr/bin/ditto', [join(mount, 'Blob.app'), installed], { timeout: 60000 });
  await execute('/usr/bin/hdiutil', ['detach', mount], { timeout: 30000 });
  mounted = false;
  console.log('PASS DMG mounted read-only, app copied to isolated install directory, volume detached');

  await launch();
  stage = 'first-run settings';
  await expect(page.getByRole('heading', { name: '本地服务配置' })).toBeVisible();
  assert.match(page.url(), /^blob-app:\/\/app\//);
  await page.getByLabel('MySQL 连接地址', { exact: true }).fill(testDbUrl);
  await page.getByLabel('MySQL 用户名', { exact: true }).fill('root');
  await page.getByLabel('MySQL 密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: '保存并启动后端', exact: true }).click();
  await expect(page.getByText('本地服务已就绪，可以开始记录。', { exact: true })).toBeVisible({ timeout: 40000 });
  await recordJavaPid();
  const config = await readFile(join(userData, 'blob-data/config/settings.json'), 'utf8');
  assert.equal(config.includes(password), false, 'Persisted settings must not include the plaintext password');
  assert.ok(JSON.parse(config).encryptedDbPassword);
  assert.equal(await page.getByLabel('MySQL 密码', { exact: true }).inputValue(), '');
  console.log('PASS actual first-run settings, macOS safeStorage, packaged Java readiness, secure window preferences');

  const boundaries = await page.evaluate(async () => {
    const connection = await window.blobDesktop.getBootstrap();
    const missing = await fetch(`${connection.apiBaseUrl}/system/ping`);
    const wrong = await fetch(`${connection.apiBaseUrl}/system/ping`, { headers: { 'X-Blob-Desktop-Token': 'incorrect-smoke-token' } });
    const arbitrary = await fetch('blob-app://app/api/v1/journals');
    return { missing: missing.status, wrong: wrong.status, arbitrary: arbitrary.status, secretGetter: 'readForBackend' in window.blobDesktop || 'getPassword' in window.blobDesktop };
  });
  assert.deepEqual(boundaries, { missing: 401, wrong: 401, arbitrary: 403, secretGetter: false });
  console.log('PASS missing/wrong token rejected, arbitrary protocol API rejected, no secret getter');

  await page.getByRole('link', { name: '写日志', exact: true }).click();
  await page.getByRole('textbox', { name: '标题', exact: true }).fill(title);
  await page.getByLabel('日期', { exact: true }).fill('2026-09-07');
  await page.locator('.cm-content').click();
  await page.keyboard.insertText('# 安装包验证\n中文 Markdown 与本地图片。\n\n```java\nSystem.out.println("Blob desktop");\n```');
  const uploading = page.waitForResponse(response => response.url().endsWith('/api/v1/media/images') && response.request().method() === 'POST');
  await page.getByLabel('上传图片', { exact: true }).setInputFiles({ name: 'desktop-smoke.png', mimeType: 'image/png', buffer: image });
  const upload = await (await uploading).json();
  assert.equal(upload.code, 'OK');
  imageUrl = upload.data.url;
  const creating = page.waitForResponse(response => response.url().endsWith('/api/v1/journals') && response.request().method() === 'POST');
  await page.getByRole('button', { name: '保存日志', exact: true }).click();
  const created = await (await creating).json();
  assert.equal(created.code, 'OK');
  journalId = created.data.id;
  await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
  await expect(page.locator('.journal-detail pre code')).toBeVisible();
  await expect(page.locator('.journal-detail pre code')).toContainText('Blob desktop');
  await expect.poll(() => page.locator('.journal-detail img').first().evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
  assert.ok((await currentApi(`/journals/${journalId}`)).body.data.contentMd.includes(imageUrl));
  await page.screenshot({ path: join(screenshotDirectory, 'packaged-journal.png'), fullPage: true });
  console.log('PASS journal and PNG created through UI, persisted Markdown uses relative media URL, image decoded');
  stage = 'knowledge promotion and relation';
  await page.getByRole('link', { name: '整理为知识', exact: true }).click();
  await expect(page.getByRole('textbox', { name: '标题', exact: true })).toHaveValue(title);
  await page.getByRole('textbox', { name: '标题', exact: true }).fill(`${title} 知识`);
  const promoting = page.waitForResponse(response => response.url().endsWith(`/journals/${journalId}/promote-to-knowledge`) && response.request().method() === 'POST');
  await page.getByRole('button', { name: '保存知识', exact: true }).click();
  const promoted = await (await promoting).json();
  assert.equal(promoted.code, 'OK');
  knowledgeIds.push(promoted.data.id);
  await expect(page.getByRole('heading', { name: `${title} 知识`, exact: true })).toBeVisible();
  await expect.poll(() => page.locator('.knowledge-detail img').first().evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
  const related = await currentApi('/knowledge', 'POST', { title: `${title} 相关知识`, contentMd: '独立知识正文', summary: '用户手写摘要', categoryId: null, tagIds: [] });
  assert.equal(related.body.code, 'OK');
  knowledgeIds.push(related.body.data.id);
  const relation = await currentApi('/knowledge/relations', 'POST', { sourceKnowledgeId: knowledgeIds[0], targetKnowledgeId: knowledgeIds[1], relationType: 'RELATED' });
  assert.equal(relation.body.code, 'OK');
  relationId = relation.body.data.id;
  const knowledge = (await currentApi(`/knowledge/${knowledgeIds[0]}`)).body.data;
  assert.equal(knowledge.sourceJournalId, journalId);
  assert.equal(knowledge.sourceJournalTitle, title);
  assert.ok(knowledge.contentMd.includes(imageUrl));
  await page.screenshot({ path: join(screenshotDirectory, 'packaged-knowledge.png'), fullPage: true });
  console.log('PASS M2 UI promotion retains source and original image URL; independent knowledge and RELATED created');
  await quit();
  console.log('PASS first quit reaped tracked Java child');

  await launch();
  stage = 'relaunch readiness';
  await expect.poll(async () => page.evaluate(async () => (await window.blobDesktop.getBootstrap()).setupRequired), { timeout: 40000 }).toBe(false);
  await recordJavaPid();
  stage = 'relaunch knowledge and relation';
  await page.evaluate(id => { window.location.hash = `/knowledge/${id}`; }, knowledgeIds[0]);
  await expect(page.getByRole('heading', { name: `${title} 知识`, exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: `${title} 相关知识`, exact: true })).toBeVisible();
  await expect.poll(() => page.locator('.knowledge-detail img').first().evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
  const restored = (await currentApi(`/knowledge/${knowledgeIds[0]}`)).body.data;
  assert.equal(restored.sourceJournalId, journalId);
  assert.equal((await currentApi(`/knowledge/${knowledgeIds[1]}/relations`)).body.data.items[0].knowledgeId, knowledgeIds[0]);
  console.log('PASS M2 relaunch restored knowledge, source snapshot, bidirectional relation and decoded image');
  await page.evaluate(id => { window.location.hash = `/journals/${id}`; }, journalId);
  await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
  stage = 'relaunch image decoding';
  await expect.poll(() => page.locator('.journal-detail img').first().evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: '删除日志', exact: true }).click();
  await expect(page).toHaveURL(/#\/$/);
  assert.equal((await currentApi(`/journals/${journalId}`)).status, 404);
  journalId = undefined;
  await page.evaluate(id => { window.location.hash = `/knowledge/${id}`; }, knowledgeIds[0]);
  await expect(page.getByText('来源日志已删除', { exact: false })).toBeVisible();
  await expect.poll(() => page.locator('.knowledge-detail img').first().evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
  await page.screenshot({ path: join(screenshotDirectory, 'packaged-knowledge-relaunch.png'), fullPage: true });
  assert.equal((await currentApi(`/knowledge/relations/${relationId}`, 'DELETE')).body.code, 'OK');
  relationId = undefined;
  for (const id of knowledgeIds) assert.equal((await currentApi(`/knowledge/${id}`, 'DELETE')).body.code, 'OK');
  knowledgeIds.length = 0;
  await quit();
  console.log('PASS relaunch restored encrypted settings, MySQL journal and local image; UI deletion and second child cleanup verified');
} catch (error) {
  failed = true;
  console.error(`Packaged smoke failed at ${stage}: ${String(error.message).split(password).join('[REDACTED]')}`);
  if (stage === 'relaunch image decoding' && page && !page.isClosed()) {
    const diagnosis = await page.locator('.journal-detail img').first().evaluate(async element => {
      const response = await fetch(element.src);
      return { src: element.getAttribute('src'), complete: element.complete, naturalWidth: element.naturalWidth, status: response.status, contentType: response.headers.get('content-type'), bytes: (await response.arrayBuffer()).byteLength };
    }).catch(() => ({ diagnosticUnavailable: true }));
    console.error(JSON.stringify(diagnosis));
    await page.screenshot({ path: join(screenshotDirectory, 'packaged-relaunch-failure.png'), fullPage: true }).catch(() => undefined);
  }
  process.exitCode = 1;
} finally {
  if (page && !page.isClosed()) {
    if (relationId) await currentApi(`/knowledge/relations/${relationId}`, 'DELETE').catch(() => undefined);
    for (const id of knowledgeIds) await currentApi(`/knowledge/${id}`, 'DELETE').catch(() => undefined);
  }
  if (journalId && page && !page.isClosed()) await currentApi(`/journals/${journalId}`, 'DELETE').catch(() => undefined);
  await quit().catch(() => { console.error('Java cleanup could not be verified'); process.exitCode = 1; });
  if (mounted) {
    await execute('/usr/bin/hdiutil', ['detach', mount], { timeout: 30000 }).then(() => { mounted = false; }).catch(() => { console.error('DMG detach requires follow-up'); process.exitCode = 1; });
  }
  if (!mounted && !failed && !process.exitCode) await rm(temporary, { recursive: true, force: true });
  else console.log(`Retained isolated smoke evidence directory: ${temporary}`);
}
