import { test, expect, type Page } from '@playwright/test';

const createKnowledge = async (page: Page, title: string, categoryName: string | null, recordId: (id: number) => void): Promise<number> => {
  await page.goto('/#/knowledge');
  await page.getByRole('link', { name: '新建知识', exact: true }).click();
  await page.getByRole('textbox', { name: '标题', exact: true }).fill(title);
  await page.getByLabel('摘要', { exact: true }).fill(`手动摘要：${title}`);
  await page.getByRole('combobox', { name: '分类', exact: true }).selectOption({ label: categoryName ?? '未分类' });
  await page.locator('.cm-content').click();
  await page.keyboard.insertText(`# ${title}\n\n独立创建的知识正文。`);
  const created = page.waitForResponse(response => response.url().endsWith('/api/v1/knowledge') && response.request().method() === 'POST');
  await page.getByRole('button', { name: '保存知识', exact: true }).click();
  const response = await created;
  expect(response.ok()).toBe(true);
  const id: number = (await response.json()).data.id;
  recordId(id);
  await expect(page).toHaveURL(new RegExp(`/knowledge/${id}$`));
  await expect(page.locator('.knowledge-detail > h1')).toHaveText(title);
  return id;
};

test('M2 independent knowledge, category conflicts and restored filters, related picker lifecycle', async ({ page, request }) => {
  test.setTimeout(120_000);
  const stamp = `${Date.now()}-${test.info().workerIndex}`;
  const categoryName = `M2管理分类-${stamp}`;
  const renamedCategory = `M2已改名-${stamp}`;
  const firstTitle = `M2分类知识-${stamp}`;
  const secondTitle = `M2未分类知识-${stamp}`;
  const knowledgeIds: number[] = [];
  let categoryId: number | undefined;
  try {
    await page.goto('/#/knowledge');
    await page.getByRole('link', { name: '分类管理', exact: true }).click();
    await page.getByRole('button', { name: '新建分类', exact: true }).click();
    await page.getByLabel('分类名称', { exact: true }).fill(categoryName);
    await page.getByLabel('分类描述', { exact: true }).fill('知识分类管理端到端用例');
    const creatingCategory = page.waitForResponse(response => response.url().endsWith('/api/v1/knowledge/categories') && response.request().method() === 'POST');
    await page.getByRole('button', { name: '保存分类', exact: true }).click();
    const categoryResponse = await creatingCategory;
    expect(categoryResponse.ok()).toBe(true);
    categoryId = (await categoryResponse.json()).data.id;
    await expect(page.getByRole('cell', { name: categoryName, exact: true })).toBeVisible();

    const firstId = await createKnowledge(page, firstTitle, categoryName, id => knowledgeIds.push(id));
    const first = (await (await request.get(`/api/v1/knowledge/${firstId}`)).json()).data;
    expect(first).toMatchObject({ title: firstTitle, category: { id: categoryId, name: categoryName }, sourceJournalId: null, sourceJournalTitle: null });
    expect(first.contentMd).toContain('独立创建的知识正文');
    const secondId = await createKnowledge(page, secondTitle, null, id => knowledgeIds.push(id));
    expect((await (await request.get(`/api/v1/knowledge/${secondId}`)).json()).data.category).toBeNull();

    await page.goto('/#/knowledge/categories');
    const categoryRow = page.getByRole('row').filter({ has: page.getByRole('cell', { name: categoryName, exact: true }) });
    await categoryRow.getByRole('button', { name: '编辑', exact: true }).click();
    await page.getByLabel('分类名称', { exact: true }).fill(renamedCategory);
    await page.getByLabel('分类描述', { exact: true }).fill('分类名称和描述均已更新');
    const updatingCategory = page.waitForResponse(response => response.url().endsWith(`/api/v1/knowledge/categories/${categoryId}`) && response.request().method() === 'PUT');
    await page.getByRole('button', { name: '保存分类', exact: true }).click();
    expect((await updatingCategory).ok()).toBe(true);
    const renamedRow = page.getByRole('row').filter({ has: page.getByRole('cell', { name: renamedCategory, exact: true }) });
    await expect(renamedRow).toContainText('分类名称和描述均已更新');
    expect((await (await request.get(`/api/v1/knowledge/${firstId}`)).json()).data.category.name).toBe(renamedCategory);
    const conflictingDelete = page.waitForResponse(response => response.url().endsWith(`/api/v1/knowledge/categories/${categoryId}`) && response.request().method() === 'DELETE');
    page.once('dialog', dialog => dialog.accept());
    await renamedRow.getByRole('button', { name: '删除', exact: true }).click();
    const conflict = await conflictingDelete;
    expect(conflict.status()).toBe(409);
    await expect(page.getByRole('alert')).toContainText((await conflict.json()).message);
    await expect(renamedRow).toBeVisible();

    await page.getByRole('link', { name: '返回知识库', exact: true }).click();
    await page.getByLabel('关键词', { exact: true }).fill(stamp);
    await page.getByRole('combobox', { name: '分类', exact: true }).selectOption(String(categoryId));
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    await expect(page.getByRole('link', { name: firstTitle, exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: secondTitle, exact: true })).toHaveCount(0);
    await page.reload();
    await expect(page.getByLabel('关键词', { exact: true })).toHaveValue(stamp);
    await expect(page.getByRole('combobox', { name: '分类', exact: true })).toHaveValue(String(categoryId));
    await expect(page.getByRole('link', { name: firstTitle, exact: true })).toBeVisible();
    const categoryQuery = new URLSearchParams((await page.evaluate(() => location.hash)).split('?')[1]);
    expect(categoryQuery.get('categoryId')).toBe(String(categoryId));
    expect(categoryQuery.has('uncategorized')).toBe(false);

    await page.getByRole('combobox', { name: '分类', exact: true }).selectOption('uncategorized');
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    await expect(page.getByRole('link', { name: secondTitle, exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: firstTitle, exact: true })).toHaveCount(0);
    await page.reload();
    await expect(page.getByLabel('关键词', { exact: true })).toHaveValue(stamp);
    await expect(page.getByRole('combobox', { name: '分类', exact: true })).toHaveValue('uncategorized');
    await expect(page.getByRole('link', { name: secondTitle, exact: true })).toBeVisible();
    const uncategorizedQuery = new URLSearchParams((await page.evaluate(() => location.hash)).split('?')[1]);
    expect(uncategorizedQuery.get('uncategorized')).toBe('true');
    expect(uncategorizedQuery.has('categoryId')).toBe(false);

    await page.goto(`/#/knowledge/${firstId}`);
    await page.getByRole('button', { name: '关联知识', exact: true }).click();
    const picker = page.getByRole('dialog', { name: '关联知识', exact: true });
    await picker.getByLabel('关联关键词', { exact: true }).fill(secondTitle);
    const findingRelated = page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/knowledge' && new URL(response.url()).searchParams.get('keyword') === secondTitle);
    await picker.getByRole('button', { name: '搜索关联', exact: true }).click();
    expect((await findingRelated).ok()).toBe(true);
    const candidate = picker.getByRole('listitem').filter({ hasText: secondTitle });
    await expect(candidate).toBeVisible();
    await expect(picker.getByText(firstTitle, { exact: true })).toHaveCount(0);
    const creatingRelation = page.waitForResponse(response => response.url().endsWith('/api/v1/knowledge/relations') && response.request().method() === 'POST');
    await candidate.getByRole('button', { name: '关联', exact: true }).click();
    const relationResponse = await creatingRelation;
    expect(relationResponse.ok()).toBe(true);
    const relationId: number = (await relationResponse.json()).data.id;
    await expect(picker).toHaveCount(0);
    await page.getByRole('link', { name: secondTitle, exact: true }).click();
    await expect(page.locator('.knowledge-detail > h1')).toHaveText(secondTitle);
    await expect(page.getByRole('link', { name: firstTitle, exact: true })).toBeVisible();
    const removingRelation = page.waitForResponse(response => response.url().endsWith(`/api/v1/knowledge/relations/${relationId}`) && response.request().method() === 'DELETE');
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: '解除关联', exact: true }).click();
    expect((await removingRelation).ok()).toBe(true);
    await expect(page.getByRole('link', { name: firstTitle, exact: true })).toHaveCount(0);
    expect((await (await request.get(`/api/v1/knowledge/${firstId}/relations`)).json()).data.total).toBe(0);
    expect((await request.get(`/api/v1/knowledge/${firstId}`)).ok()).toBe(true);
    expect((await request.get(`/api/v1/knowledge/${secondId}`)).ok()).toBe(true);
    await page.screenshot({ path: 'test-results/m2-management-detail.png', fullPage: true });

    await page.goto(`/#/knowledge/${firstId}/edit`);
    await page.getByRole('combobox', { name: '分类', exact: true }).selectOption({ label: '未分类' });
    await page.getByRole('button', { name: '保存知识', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/knowledge/${firstId}$`));
    await page.goto('/#/knowledge/categories');
    const deletingCategory = page.waitForResponse(response => response.url().endsWith(`/api/v1/knowledge/categories/${categoryId}`) && response.request().method() === 'DELETE');
    page.once('dialog', dialog => dialog.accept());
    await renamedRow.getByRole('button', { name: '删除', exact: true }).click();
    expect((await deletingCategory).ok()).toBe(true);
    await expect(renamedRow).toHaveCount(0);
    categoryId = undefined;
  } finally {
    for (const id of knowledgeIds) { await request.delete(`/api/v1/knowledge/${id}`); }
    if (categoryId !== undefined) { await request.delete(`/api/v1/knowledge/categories/${categoryId}`); }
  }
});
