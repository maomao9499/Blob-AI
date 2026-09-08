import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import KnowledgeList from '@/pages/knowledge/index.vue';
import Search from '@/pages/search/index.vue';
import { categoryApi, knowledgeApi } from '@/api/knowledge';
import { searchApi } from '@/api/search';
import { tagApi } from '@/api/tag';
import type { KnowledgeSummary } from '@/types/knowledge.types';
import type { PageResponse } from '@/types/journal.types';
let wrapper: VueWrapper;
const entry = (id: number, title: string): KnowledgeSummary => ({ id, title, summary: null, excerpt: '正文摘要', category: null, tags: [], createdAt: '', updatedAt: '' });
const page = (items: KnowledgeSummary[], current = 1, total = items.length): PageResponse<KnowledgeSummary> => ({ items, page: current, pageSize: 20, total });
beforeEach(() => { vi.spyOn(tagApi, 'list').mockResolvedValue([]); vi.spyOn(categoryApi, 'list').mockResolvedValue([]); });
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks(); });
const open = async (path: string) => {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/knowledge', component: KnowledgeList }, { path: '/search', component: Search },
    { path: '/knowledge/:id', component: { template: '<p>知识</p>' } },
    { path: '/journals/:id', component: { template: '<p>日志</p>' } },
    { path: '/knowledge/:id/edit', component: { template: '<p>编辑</p>' } },
  ] });
  await router.push(path); wrapper = mount({ template: '<router-view />' }, { global: { plugins: [router] } });
  await router.isReady(); await flushPromises(); return router;
};
it('restores filters and ignores older knowledge responses', async () => {
  let finish!: (result: PageResponse<KnowledgeSummary>) => void;
  const list = vi.spyOn(knowledgeApi, 'list').mockImplementationOnce(() => new Promise(resolve => { finish = resolve; })).mockResolvedValueOnce(page([entry(2, '最新内容')]));
  const router = await open('/knowledge?keyword=旧条件&categoryId=3&tagId=4&page=2');
  expect(list).toHaveBeenCalledWith(expect.objectContaining({ keyword: '旧条件', categoryId: 3, tagId: 4, page: 2 }));
  await router.push('/knowledge?keyword=新条件'); await flushPromises();
  finish(page([entry(1, '过时内容')], 2, 40)); await flushPromises();
  expect(wrapper.text()).toContain('最新内容'); expect(wrapper.text()).not.toContain('过时内容');
  expect((wrapper.get('[name="keyword"]').element as HTMLInputElement).value).toBe('新条件');
});
it('returns to a valid page after deleting the last item and retains filters', async () => {
  vi.spyOn(knowledgeApi, 'list').mockResolvedValueOnce(page([entry(21, '末页内容')], 2, 21)).mockResolvedValueOnce(page([], 2, 20)).mockResolvedValueOnce(page([entry(20, '第一页内容')], 1, 20));
  vi.spyOn(knowledgeApi, 'remove').mockResolvedValue(undefined); vi.spyOn(window, 'confirm').mockReturnValue(true);
  const router = await open('/knowledge?keyword=内容&page=2');
  await wrapper.get('button.danger-link').trigger('click'); await flushPromises();
  expect(router.currentRoute.value.query).toMatchObject({ keyword: '内容', page: '1' });
  expect(wrapper.text()).toContain('第一页内容');
});
it('keeps the two source types separate even when their IDs match and retries errors', async () => {
  const list = vi.spyOn(searchApi, 'list').mockRejectedValueOnce(new Error('搜索失败')).mockResolvedValueOnce({ items: [
    { sourceType: 'JOURNAL', sourceId: 1, title: '日志结果', excerpt: '', tags: [], updatedAt: '' },
    { sourceType: 'KNOWLEDGE', sourceId: 1, title: '知识结果', excerpt: '', tags: [], updatedAt: '' },
  ], total: 2, page: 1, pageSize: 20 });
  await open('/search?keyword=中文&sourceType=ALL&tagId=8');
  expect(wrapper.get('[role="alert"]').text()).toContain('搜索失败');
  await wrapper.get('[role="alert"] button').trigger('click'); await flushPromises();
  expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: '中文', tagId: 8, sourceType: 'ALL' }));
  expect(wrapper.get('a[href="/journals/1"]').text()).toBe('日志结果');
  expect(wrapper.get('a[href="/knowledge/1"]').text()).toBe('知识结果');
});
it.each(['/knowledge', '/search'])('clears unsubmitted filters when resetting the bare %s route', async (path) => {
  vi.spyOn(knowledgeApi, 'list').mockResolvedValue(page([]));
  vi.spyOn(searchApi, 'list').mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 });
  const router = await open(path);
  await wrapper.get('[name="keyword"]').setValue('尚未提交的筛选');
  const select = path === '/knowledge' ? '[name="category"]' : '[name="sourceType"]';
  await wrapper.get(select).setValue(path === '/knowledge' ? 'uncategorized' : 'KNOWLEDGE');
  const reset = wrapper.findAll('button').find(item => item.text() === '重置');
  if (!reset) { throw new Error('Missing reset button'); }
  await reset.trigger('click'); await flushPromises();
  expect(router.currentRoute.value.fullPath).toBe(path);
  expect((wrapper.get('[name="keyword"]').element as HTMLInputElement).value).toBe('');
  expect((wrapper.get(select).element as HTMLSelectElement).value).toBe(path === '/knowledge' ? '' : 'ALL');
});
