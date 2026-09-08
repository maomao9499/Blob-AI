import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, expect, it, vi } from 'vitest';
import Categories from '@/pages/knowledgeCategories/index.vue';
import KnowledgeDetail from '@/pages/knowledgeDetail/index.vue';
import { categoryApi, knowledgeApi } from '@/api/knowledge';
import type { KnowledgeDetail as Detail, KnowledgeSummary } from '@/types/knowledge.types';
vi.mock('md-editor-v3', () => ({ MdPreview: { props: ['modelValue'], template: '<div class="preview">{{ modelValue }}</div>' } }));
let wrapper: VueWrapper;
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks(); });
const button = (text: string) => { const found = wrapper.findAll('button').find(item => item.text() === text); if (!found) throw new Error(`Missing button ${text}`); return found; };
const detail = (overrides: Partial<Detail> = {}): Detail => ({ id: 1, title: '当前知识', contentMd: '正文', summary: null, excerpt: '', category: null, tags: [], sourceJournalId: null, sourceJournalTitle: '来源快照', createdAt: '', updatedAt: '', ...overrides });
const open = async (path: string) => {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/knowledge/categories', component: Categories },
    { path: '/knowledge/:id', component: KnowledgeDetail },
    { path: '/knowledge/:id/edit', component: { template: '<p>编辑</p>' } },
    { path: '/knowledge', component: { template: '<p>列表</p>' } },
  ] });
  await router.push(path); wrapper = mount({ template: '<router-view />' }, { global: { plugins: [router] } });
  await router.isReady(); await flushPromises(); return router;
};
it('retains a category conflict for correction and reports in-use deletion errors', async () => {
  const category = { id: 3, name: 'Java', description: null, createdAt: '', updatedAt: '' };
  vi.spyOn(categoryApi, 'list').mockResolvedValue([category]);
  vi.spyOn(categoryApi, 'create').mockRejectedValueOnce(new Error('分类名称已存在')).mockResolvedValueOnce({ id: 4 });
  vi.spyOn(categoryApi, 'remove').mockRejectedValue(new Error('分类仍被知识使用'));
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  await open('/knowledge/categories');
  await button('新建分类').trigger('click');
  await wrapper.get('[name="name"]').setValue('Java');
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(wrapper.get('[role="dialog"]').text()).toContain('分类名称已存在');
  expect((wrapper.get('[name="name"]').element as HTMLInputElement).value).toBe('Java');
  await wrapper.get('[name="name"]').setValue('数据库');
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(wrapper.find('[role="dialog"]').exists()).toBe(false);
  await button('删除').trigger('click'); await flushPromises();
  expect(wrapper.text()).toContain('分类仍被知识使用'); expect(wrapper.text()).toContain('Java');
});
it('shows deleted source snapshot and paginated related selector excluding itself with conflict retry', async () => {
  vi.spyOn(knowledgeApi, 'get').mockResolvedValue(detail());
  vi.spyOn(knowledgeApi, 'relations').mockResolvedValue({ items: [{ id: 91, knowledgeId: 2, title: '已有关联' }], total: 1, page: 1, pageSize: 20 });
  const list = vi.spyOn(knowledgeApi, 'list').mockResolvedValueOnce({ items: [detail(), detail({ id: 2, title: '候选知识' })] as KnowledgeSummary[], total: 22, page: 1, pageSize: 20 }).mockResolvedValue({ items: [detail({ id: 3, title: '下一页候选' })], total: 22, page: 2, pageSize: 20 });
  const relate = vi.spyOn(knowledgeApi, 'relate').mockRejectedValueOnce(new Error('关联已存在')).mockResolvedValueOnce({ id: 92 });
  await open('/knowledge/1');
  expect(wrapper.text()).toContain('来源快照'); expect(wrapper.text()).toContain('来源日志已删除');
  expect(wrapper.find('a[href="/journals/1"]').exists()).toBe(false);
  expect(wrapper.get('a[href="/knowledge/2"]').text()).toBe('已有关联');
  await button('关联知识').trigger('click'); await flushPromises();
  expect(wrapper.get('[role="dialog"]').text()).not.toContain('当前知识');
  await wrapper.get('[role="dialog"]').findAll('button').find(item => item.text() === '下一页')!.trigger('click'); await flushPromises();
  expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }));
  await button('关联').trigger('click'); await flushPromises();
  expect(wrapper.get('[role="dialog"]').text()).toContain('关联已存在');
  await button('关联').trigger('click'); await flushPromises();
  expect(relate).toHaveBeenLastCalledWith({ sourceKnowledgeId: 1, targetKnowledgeId: 3, relationType: 'RELATED' });
  expect(wrapper.find('[role="dialog"]').exists()).toBe(false);
});
it('protects dirty category input when route leave or refresh is cancelled and removes unload listener on close', async () => {
  vi.spyOn(categoryApi, 'list').mockResolvedValue([]);
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  const router = await open('/knowledge/categories');
  await button('新建分类').trigger('click');
  await wrapper.get('[name="name"]').setValue('未保存分类');
  await router.push('/knowledge'); await flushPromises();
  expect(router.currentRoute.value.path).toBe('/knowledge/categories');
  expect(confirm).toHaveBeenCalled();
  expect((wrapper.get('[name="name"]').element as HTMLInputElement).value).toBe('未保存分类');
  await router.push('/knowledge/categories?from=changed'); await flushPromises();
  expect(router.currentRoute.value.fullPath).toBe('/knowledge/categories');
  const refreshing = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(refreshing);
  expect(refreshing.defaultPrevented).toBe(true);
  confirm.mockReturnValue(true);
  await button('取消').trigger('click'); await flushPromises();
  const afterClosing = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(afterClosing);
  expect(afterClosing.defaultPrevented).toBe(false);
});
it('denies route leave and refresh while a category save is pending then retains failed input', async () => {
  vi.spyOn(categoryApi, 'list').mockResolvedValue([]);
  let rejectSave!: (reason: Error) => void;
  vi.spyOn(categoryApi, 'create').mockImplementation(() => new Promise((_resolve, reject) => { rejectSave = reject; }));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
  const router = await open('/knowledge/categories');
  await button('新建分类').trigger('click');
  await wrapper.get('[name="name"]').setValue('正在保存分类');
  await wrapper.get('form').trigger('submit'); await flushPromises();
  await router.push('/knowledge'); await flushPromises();
  expect(router.currentRoute.value.path).toBe('/knowledge/categories');
  expect(confirm).not.toHaveBeenCalled();
  const refreshing = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(refreshing); expect(refreshing.defaultPrevented).toBe(true);
  rejectSave(new Error('分类保存失败')); await flushPromises();
  expect(wrapper.get('[role="dialog"]').text()).toContain('分类保存失败');
  expect((wrapper.get('[name="name"]').element as HTMLInputElement).value).toBe('正在保存分类');
  await router.push('/knowledge'); await flushPromises();
  expect(confirm).toHaveBeenCalled(); expect(router.currentRoute.value.path).toBe('/knowledge');
});
