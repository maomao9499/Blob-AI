import { test, expect } from '@playwright/test';

test('M1 real MySQL journal, tag, Markdown and image lifecycle', async ({ page, request }) => {
  const stamp = Date.now().toString();
  const title = `M1 学习记录 ${stamp}`;
  const tagName = `Redis-${stamp}`;
  const markdown = '# 今日学习\n中文缓存知识\n\n```java\nSystem.out.println("Hello");\n```\n\n<script>window.e2eInjected=true</script>\n<img src=x onerror="window.e2eInjected=true">';
  let journalId: number | undefined; let tagId: number | undefined;
  try {
    await page.goto('/#/tags');
    await page.getByRole('button', { name: '新建标签', exact: true }).click();
    await page.getByLabel('标签名称').fill(tagName);
    const tagCreated = page.waitForResponse((res) => res.url().endsWith('/api/v1/tags') && res.request().method() === 'POST');
    await page.getByRole('button', { name: '保存标签' }).click();
    tagId = (await (await tagCreated).json()).data.id;
    await expect(page.getByRole('cell').filter({ hasText: tagName })).toBeVisible();
    await page.getByRole('link', { name: '写日志', exact: true }).click();
    await page.getByRole('textbox', { name: '标题', exact: true }).fill(title);
    await page.getByLabel('日期', { exact: true }).fill('2026-09-07');
    await page.getByLabel(tagName, { exact: true }).check();
    const editor = page.locator('.cm-content');
    await editor.click(); await page.keyboard.insertText(markdown);
    const imageUploaded = page.waitForResponse((res) => res.url().endsWith('/api/v1/media/images') && res.request().method() === 'POST');
    await page.getByLabel('上传图片', { exact: true }).setInputFiles({ name: 'm1-pixel.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jD1sAAAAASUVORK5CYII=', 'base64') });
    const uploaded = await (await imageUploaded).json();
    expect(uploaded.code).toBe('OK');
    await expect(page.locator(`.md-editor-preview img[src="${uploaded.data.url}"]`)).toBeVisible();
    const journalCreated = page.waitForResponse((res) => res.url().endsWith('/api/v1/journals') && res.request().method() === 'POST');
    await page.getByRole('button', { name: '保存日志', exact: true }).click();
    journalId = (await (await journalCreated).json()).data.id;
    expect((await (await request.get(`/api/v1/journals/${journalId}`)).json()).data.contentMd).toContain(uploaded.data.url);
    await expect(page).toHaveURL(new RegExp(`/journals/${journalId}$`));
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await expect(page.locator('.md-editor-preview pre')).toContainText('System.out.println');
    await expect(page.locator('.md-editor-preview pre code')).toBeVisible();
    await expect(page.locator(`.journal-detail img[src="${uploaded.data.url}"]`)).toBeVisible();
    await expect.poll(() => page.locator(`.journal-detail img[src="${uploaded.data.url}"]`).evaluate((element) => (element as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    expect(await page.evaluate(() => Reflect.get(window, 'e2eInjected'))).toBeUndefined();
    await page.getByRole('link', { name: '← 返回时间线' }).click();
    await page.getByLabel('关键词').fill('中文缓存知识');
    await page.getByRole('combobox', { name: '标签', exact: true }).selectOption(String(tagId));
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await page.reload();
    await expect(page.getByLabel('关键词')).toHaveValue('中文缓存知识');
    await page.getByRole('link', { name: title, exact: true }).click();
    await page.getByRole('link', { name: '编辑日志', exact: true }).click();
    await page.getByRole('combobox', { name: '类型', exact: true }).selectOption('LIFE');
    await page.getByLabel('日期', { exact: true }).fill('2026-09-06');
    await page.getByRole('textbox', { name: '标题', exact: true }).fill(`${title} 已修改`);
    await page.getByRole('button', { name: '保存日志', exact: true }).click();
    await expect(page.getByRole('heading', { name: `${title} 已修改` })).toBeVisible();
    await expect(page.locator('.journal-detail')).toContainText('生活日志');
    await page.screenshot({ path: 'test-results/m1-journal-detail.png', fullPage: true });
    page.once('dialog', (dialog) => dialog.accept());
    await page.getByRole('button', { name: '删除日志', exact: true }).click();
    await expect(page).toHaveURL(/#\/$/);
    const missing = await request.get(`/api/v1/journals/${journalId}`);
    expect(missing.status()).toBe(404);
    journalId = undefined;
  } finally {
    if (journalId) { await request.delete(`/api/v1/journals/${journalId}`); }
    if (tagId) { await request.delete(`/api/v1/tags/${tagId}`); }
  }
});

test('unsaved changes require explicit discard; cancelled navigation preserves edits', async ({ page }) => {
  await page.goto('/#/journals/new');
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('未保存的记录');
  page.once('dialog', (dialog) => dialog.dismiss());
  await page.getByRole('link', { name: '返回时间线', exact: true }).click();
  await expect(page).toHaveURL(/journals\/new$/);
  await expect(page.getByRole('textbox', { name: '标题', exact: true })).toHaveValue('未保存的记录');
  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('link', { name: '返回时间线', exact: true }).click();
  await expect(page).toHaveURL(/#\/$/);
});
