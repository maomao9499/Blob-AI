import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import KnowledgeEdit from '@/pages/knowledgeEdit/index.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { knowledgeApi, categoryApi } from '@/api/knowledge';
import { journalApi } from '@/api/journal';
import { tagApi } from '@/api/tag';

vi.mock('md-editor-v3', () => ({ MdEditor: { props: ['modelValue'], emits: ['update:modelValue'], template: '<textarea aria-label="正文" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' } }));
let wrapper: VueWrapper;
beforeEach(() => {
  vi.spyOn(tagApi, 'list').mockResolvedValue([]);
  vi.spyOn(categoryApi, 'list').mockResolvedValue([]);
});
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks(); });
const open = async (path = '/knowledge/new') => {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/knowledge/new', component: KnowledgeEdit },
    { path: '/knowledge/:id/edit', component: KnowledgeEdit },
    { path: '/knowledge/:id', component: { template: '<p>知识详情</p>' } },
    { path: '/knowledge', component: { template: '<p>知识列表</p>' } },
  ] });
  await router.push(path);
  wrapper = mount({ template: '<router-view />' }, { global: { plugins: [router] } });
  await router.isReady(); await flushPromises(); return router;
};
const fill = async () => {
  await wrapper.get('[name="title"]').setValue('手动知识');
  await wrapper.get('textarea[aria-label="正文"]').setValue('正文 ![](/api/v1/media/images/example.png)');
};
it('loads promotion as an unsaved draft without creating and confirms leaving even without edits', async () => {
  vi.spyOn(journalApi, 'get').mockResolvedValue({ id: 7, title: '来源标题', contentMd: '原文', tags: [], entryType: 'LEARNING', entryDate: '2026-09-08', excerpt: '', aiSummary: null, createdAt: '', updatedAt: '' });
  const promote = vi.spyOn(knowledgeApi, 'promote');
  const create = vi.spyOn(knowledgeApi, 'create');
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  const router = await open('/knowledge/new?sourceJournalId=7');
  expect((wrapper.get('[name="title"]').element as HTMLInputElement).value).toBe('来源标题');
  expect(promote).not.toHaveBeenCalled(); expect(create).not.toHaveBeenCalled();
  await router.push('/knowledge');
  expect(confirm).toHaveBeenCalled(); expect(router.currentRoute.value.path).toBe('/knowledge/new');
});
it('preserves pending edits and uses PUT after the first create succeeds', async () => {
  let finish!: (result: {id: number}) => void;
  const create = vi.spyOn(knowledgeApi, 'create').mockImplementation(() => new Promise(resolve => { finish = resolve; }));
  const update = vi.spyOn(knowledgeApi, 'update').mockResolvedValue(undefined);
  const router = await open(); await fill();
  await wrapper.get('form').trigger('submit'); await flushPromises();
  await wrapper.get('[name="title"]').setValue('请求期间修改');
  finish({ id: 42 }); await flushPromises();
  expect(router.currentRoute.value.path).toBe('/knowledge/new');
  expect((wrapper.get('[name="title"]').element as HTMLInputElement).value).toBe('请求期间修改');
  expect(wrapper.text()).toContain('未保存');
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(create).toHaveBeenCalledTimes(1);
  expect(update).toHaveBeenCalledWith(42, expect.objectContaining({ title: '请求期间修改', contentMd: '正文 ![](/api/v1/media/images/example.png)' }));
  expect(router.currentRoute.value.path).toBe('/knowledge/42');
});
it('retains failed input for explicit retry and prevents saving during upload', async () => {
  const create = vi.spyOn(knowledgeApi, 'create').mockRejectedValueOnce(new Error('保存冲突')).mockResolvedValueOnce({ id: 9 });
  const router = await open(); await fill();
  wrapper.getComponent(MarkdownEditor).vm.$emit('uploading', true); await flushPromises();
  await wrapper.get('form').trigger('submit'); expect(create).not.toHaveBeenCalled();
  wrapper.getComponent(MarkdownEditor).vm.$emit('uploading', false); await flushPromises();
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(wrapper.get('[role="alert"]').text()).toContain('保存冲突');
  expect((wrapper.get('[name="title"]').element as HTMLInputElement).value).toBe('手动知识');
  expect(create).toHaveBeenCalledTimes(1);
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(router.currentRoute.value.path).toBe('/knowledge/9');
});
it('saves promotion through the source endpoint with copied tags and a normalized manual summary', async () => {
  vi.spyOn(journalApi, 'get').mockResolvedValue({ id: 7, title: '来源标题', contentMd: '来源正文', tags: [{ id: 3, name: 'Java', color: null }], entryType: 'LEARNING', entryDate: '2026-09-08', excerpt: '', aiSummary: null, createdAt: '', updatedAt: '' });
  const create = vi.spyOn(knowledgeApi, 'create');
  const promote = vi.spyOn(knowledgeApi, 'promote').mockResolvedValue({ id: 43 });
  const router = await open('/knowledge/new?sourceJournalId=7');
  await wrapper.get('[name="summary"]').setValue('  手写摘要  ');
  await wrapper.get('form').trigger('submit'); await flushPromises();
  expect(promote).toHaveBeenCalledWith(7, { title: '来源标题', contentMd: '来源正文', summary: '手写摘要', categoryId: null, tagIds: [3] });
  expect(create).not.toHaveBeenCalled();
  expect(router.currentRoute.value.path).toBe('/knowledge/43');
});
