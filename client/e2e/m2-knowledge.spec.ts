import { test, expect, type APIRequestContext } from '@playwright/test';

const image = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jD1sAAAAASUVORK5CYII=', 'base64');

async function create(request: APIRequestContext, path: string, data: object): Promise<number> {
  const response = await request.post(`/api/v1${path}`, { data });
  expect(response.ok(), `${path}: ${response.status()}`).toBe(true);
  return (await response.json()).data.id;
}

test('M2 manual promotion, independent knowledge, shared tags, relations and search lifecycle', async ({ page, request }) => {
  const stamp = Date.now().toString();
  const title = `M2 整理知识 ${stamp}`;
  const sourceTitle = `M2 来源日志 ${stamp}`;
  let journalId: number | undefined;
  let tagId: number | undefined;
  let categoryId: number | undefined;
  const knowledgeIds: number[] = [];
  try {
    tagId = await create(request, '/tags', { name: `M2共享-${stamp}`, color: '#446688' });
    categoryId = await create(request, '/knowledge/categories', { name: `M2分类-${stamp}`, description: '单层分类' });
    journalId = await create(request, '/journals', { title: sourceTitle, contentMd: '# 原始日志\n日志原文保持不变', entryType: 'LEARNING', entryDate: '2026-09-08', tagIds: [tagId] });
    await page.goto(`/#/journals/${journalId}`);
    await page.getByRole('link', { name: '整理为知识', exact: true }).click();
    await expect(page.getByRole('textbox', { name: '标题', exact: true })).toHaveValue(sourceTitle);
    const before = (await (await request.get('/api/v1/knowledge', { params: { keyword: sourceTitle } })).json()).data.total;
    expect(before).toBe(0);
    await page.getByRole('textbox', { name: '标题', exact: true }).fill(title);
    await page.getByLabel('摘要', { exact: true }).fill('手动整理摘要');
    await page.getByRole('combobox', { name: '分类', exact: true }).selectOption(String(categoryId));
    const uploading = page.waitForResponse(res => res.url().endsWith('/api/v1/media/images') && res.request().method() === 'POST');
    await page.getByLabel('上传图片', { exact: true }).setInputFiles({ name: 'm2-pixel.png', mimeType: 'image/png', buffer: image });
    const imageUrl = (await (await uploading).json()).data.url;
    const saving = page.waitForResponse(res => res.url().endsWith(`/journals/${journalId}/promote-to-knowledge`) && res.request().method() === 'POST');
    await page.getByRole('button', { name: '保存知识', exact: true }).click();
    const id = (await (await saving).json()).data.id;
    knowledgeIds.push(id);
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await expect.poll(() => page.locator('.knowledge-detail img').first().evaluate(el => (el as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    const detail = (await (await request.get(`/api/v1/knowledge/${id}`)).json()).data;
    expect(detail).toMatchObject({ sourceJournalId: journalId, sourceJournalTitle: sourceTitle, summary: '手动整理摘要', category: { id: categoryId } });
    expect(detail.contentMd).toContain(imageUrl);
    expect(detail.tags.map((tag: { id: number }) => tag.id)).toEqual([tagId]);
    expect((await (await request.get(`/api/v1/journals/${journalId}`)).json()).data.contentMd).toBe('# 原始日志\n日志原文保持不变');

    await page.getByRole('link', { name: '编辑知识', exact: true }).click();
    await page.getByLabel('摘要', { exact: true }).fill('已更新的手写摘要');
    await page.getByRole('button', { name: '保存知识', exact: true }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    const relatedId = await create(request, `/journals/${journalId}/promote-to-knowledge`, { title: `${title} 第二篇`, contentMd: '第二篇独立知识', summary: null, categoryId: null, tagIds: [] });
    knowledgeIds.push(relatedId);
    const relationId = await create(request, '/knowledge/relations', { sourceKnowledgeId: id, targetKnowledgeId: relatedId, relationType: 'RELATED' });
    expect((await request.post('/api/v1/knowledge/relations', { data: { sourceKnowledgeId: relatedId, targetKnowledgeId: id, relationType: 'RELATED' } })).status()).toBe(409);
    expect((await request.post('/api/v1/knowledge/relations', { data: { sourceKnowledgeId: id, targetKnowledgeId: id, relationType: 'RELATED' } })).status()).toBe(400);
    await page.reload();
    await expect(page.getByRole('link', { name: `${title} 第二篇`, exact: true })).toBeVisible();
    await page.getByRole('link', { name: `${title} 第二篇`, exact: true }).click();
    await expect(page.getByRole('link', { name: title, exact: true })).toBeVisible();
    expect((await request.delete(`/api/v1/knowledge/categories/${categoryId}`)).status()).toBe(409);
    expect((await request.delete(`/api/v1/tags/${tagId}`)).status()).toBe(409);
    await request.put(`/api/v1/tags/${tagId}`, { data: { name: `M2已重命名-${stamp}`, color: '#334455' } });
    expect((await (await request.get(`/api/v1/journals/${journalId}`)).json()).data.tags[0].name).toBe(`M2已重命名-${stamp}`);
    expect((await (await request.get(`/api/v1/knowledge/${id}`)).json()).data.tags[0].name).toBe(`M2已重命名-${stamp}`);
    await page.goto(`/#/search?keyword=${encodeURIComponent(stamp)}`);
    await expect(page.getByRole('link', { name: title, exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: sourceTitle, exact: true })).toBeVisible();
    await page.reload();
    await expect(page.getByLabel('关键词', { exact: true })).toHaveValue(stamp);
    await page.getByRole('link', { name: title, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/knowledge/${id}$`));
    await request.delete(`/api/v1/journals/${journalId}`);
    journalId = undefined;
    await page.reload();
    await expect(page.getByText('来源日志已删除', { exact: false })).toBeVisible();
    await expect.poll(() => page.locator('.knowledge-detail img').first().evaluate(el => (el as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    expect((await request.delete(`/api/v1/tags/${tagId}`)).status()).toBe(409);
    await page.screenshot({ path: 'test-results/m2-knowledge-detail.png', fullPage: true });
    expect((await request.delete(`/api/v1/knowledge/relations/${relationId}`)).ok()).toBe(true);
    expect((await (await request.get(`/api/v1/knowledge/${relatedId}/relations`)).json()).data.total).toBe(0);
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: '删除知识', exact: true }).click();
    await expect(page).toHaveURL(/#\/knowledge(?:\?.*)?$/);
    expect((await request.get(`/api/v1/knowledge/${id}`)).status()).toBe(404);
  } finally {
    for (const id of knowledgeIds) await request.delete(`/api/v1/knowledge/${id}`);
    if (journalId) await request.delete(`/api/v1/journals/${journalId}`);
    if (categoryId) await request.delete(`/api/v1/knowledge/categories/${categoryId}`);
    if (tagId) await request.delete(`/api/v1/tags/${tagId}`);
  }
});

test('M2 cancelled promotion remains a draft and creates no record', async ({ page, request }) => {
  const title = `M2取消草稿-${Date.now()}`;
  const id = await create(request, '/journals', { title, contentMd: '未保存知识草稿', entryType: 'LIFE', entryDate: '2026-09-08', tagIds: [] });
  try {
    await page.goto(`/#/knowledge/new?sourceJournalId=${id}`);
    await expect(page.getByRole('textbox', { name: '标题', exact: true })).toHaveValue(title);
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByRole('link', { name: '知识库', exact: true }).click();
    await expect(page).toHaveURL(/knowledge\/new/);
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('link', { name: '知识库', exact: true }).click();
    await expect(page).toHaveURL(/#\/knowledge$/);
    expect((await (await request.get('/api/v1/knowledge', { params: { keyword: title } })).json()).data.total).toBe(0);
  } finally { await request.delete(`/api/v1/journals/${id}`); }
});
