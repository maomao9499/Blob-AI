<template>
  <section class="page editor-page">
    <header class="page-heading"><div><p class="eyebrow">MAKE IT YOUR OWN</p><h1>{{ isEditing ? '编辑知识' : '新建知识' }}</h1></div><router-link to="/knowledge">返回知识库</router-link></header>
    <p v-if="isLoading" role="status">正在加载…</p>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="error" class="error" role="alert">{{ error }} <button v-if="!isReady" @click="load">重试</button></p>
    <form v-if="isReady" class="panel knowledge-form" @submit.prevent="handleSave">
      <p v-if="sourceTitle" class="muted">来源：<router-link v-if="sourceId" :to="`/journals/${sourceId}`">{{ sourceTitle }}</router-link><span v-else>{{ sourceTitle }}（来源日志已删除）</span></p>
      <label>标题<input v-model="form.title" name="title" maxlength="200" required /></label>
      <label>摘要（选填）<textarea v-model="form.summary" aria-label="摘要" name="summary" maxlength="1000" rows="3" placeholder="用自己的话概括这条知识" /></label>
      <label>分类<select v-model="form.categoryId" name="categoryId"><option :value="null">未分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
      <fieldset><legend>标签</legend><p v-if="!tags.length" class="muted">还没有标签，可在标签管理中创建。</p><label v-for="tag in tags" :key="tag.id" class="checkbox"><input v-model="form.tagIds" type="checkbox" :value="tag.id" />{{ tag.name }}</label></fieldset>
      <markdown-editor v-model="form.contentMd" @uploading="isUploading = $event" />
      <footer class="knowledge-form__footer"><span class="muted">{{ isDirty ? '有未保存的修改' : '修改后记得保存' }}</span><button class="primary" :disabled="isSaving || isUploading">{{ isSaving ? '正在保存…' : '保存知识' }}</button></footer>
    </form>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { categoryApi, knowledgeApi } from '@/api/knowledge';
import { journalApi } from '@/api/journal';
import { tagApi } from '@/api/tag';
import type { Tag } from '@/types/journal.types';
import type { KnowledgeInput, KnowledgeCategory } from '@/types/knowledge.types';

const route = useRoute();
const router = useRouter();
const emptyInput = (): KnowledgeInput => ({ title: '', contentMd: '', summary: '', categoryId: null, tagIds: [] });
const form = reactive<KnowledgeInput>(emptyInput());
const tags = ref<Tag[]>([]);
const categories = ref<KnowledgeCategory[]>([]);
const baseline = ref(JSON.stringify(form));
const error = ref('');
const notice = ref('');
const isLoading = ref(false);
const isSaving = ref(false);
const isUploading = ref(false);
const isReady = ref(false);
const savedId = ref<number | null>(null);
const sourceId = ref<number | null>(null);
const sourceTitle = ref<string | null>(null);
const isPromotionDraft = ref(false);
const isEditing = computed(() => Boolean(route.params.id) || savedId.value !== null);
const isDirty = computed(() => isReady.value && (isPromotionDraft.value || baseline.value !== JSON.stringify(form)));
let requestSequence = 0;
const requireId = (value: unknown): number => {
  const id = typeof value === 'string' ? Number(value) : NaN;
  if (!Number.isSafeInteger(id) || id <= 0) { throw new Error('内容编号无效'); }
  return id;
};
const load = async (): Promise<void> => {
  const sequence = ++requestSequence;
  isLoading.value = true; isReady.value = false; error.value = ''; notice.value = ''; savedId.value = null;
  sourceId.value = null; sourceTitle.value = null; isPromotionDraft.value = false;
  try {
    const id = route.params.id ? requireId(route.params.id) : null;
    const journalId = !id && route.query.sourceJournalId !== undefined ? requireId(route.query.sourceJournalId) : null;
    const [availableTags, availableCategories, entry, journal] = await Promise.all([
      tagApi.list(), categoryApi.list(), id ? knowledgeApi.get(id) : null, journalId ? journalApi.get(journalId) : null,
    ]);
    if (sequence !== requestSequence) { return; }
    tags.value = availableTags; categories.value = availableCategories;
    Object.assign(form, entry ? { title: entry.title, contentMd: entry.contentMd, summary: entry.summary ?? '', categoryId: entry.category?.id ?? null, tagIds: entry.tags.map((tag) => tag.id) } : emptyInput());
    sourceId.value = entry?.sourceJournalId ?? journal?.id ?? null;
    sourceTitle.value = entry?.sourceJournalTitle ?? journal?.title ?? null;
    if (journal) { Object.assign(form, { title: journal.title, contentMd: journal.contentMd, tagIds: journal.tags.map((tag) => tag.id) }); }
    baseline.value = JSON.stringify(form); isPromotionDraft.value = Boolean(journal); isReady.value = true;
  } catch (reason: unknown) {
    if (sequence === requestSequence) { error.value = reason instanceof Error ? reason.message : '加载失败'; }
  } finally { if (sequence === requestSequence) { isLoading.value = false; } }
};
const handleSave = async (): Promise<void> => {
  if (!isReady.value || isSaving.value || isUploading.value) { return; }
  if (!form.title.trim() || !form.contentMd.trim()) { error.value = '请填写标题和正文'; return; }
  isSaving.value = true; error.value = ''; notice.value = '';
  const submittedSnapshot = JSON.stringify(form);
  const sequence = requestSequence;
  const targetId = savedId.value ?? (route.params.id ? Number(route.params.id) : null);
  const input: KnowledgeInput = { ...form, title: form.title.trim(), summary: form.summary?.trim() || null, tagIds: [...form.tagIds] };
  try {
    let id = targetId;
    if (id !== null) { await knowledgeApi.update(id, input); }
    else { id = (await (sourceId.value ? knowledgeApi.promote(sourceId.value, input) : knowledgeApi.create(input))).id; }
    if (sequence !== requestSequence) { return; }
    savedId.value = id; baseline.value = submittedSnapshot; isPromotionDraft.value = false;
    if (JSON.stringify(form) !== submittedSnapshot || isUploading.value) { notice.value = '已保存提交时的内容，后续修改仍未保存。'; return; }
    isSaving.value = false;
    await router.push(`/knowledge/${id}`);
  } catch (reason: unknown) { if (sequence === requestSequence) { error.value = reason instanceof Error ? reason.message : '保存失败'; } }
  finally { if (sequence === requestSequence) { isSaving.value = false; } }
};
const confirmLeave = (): boolean => !isSaving.value && (!(isDirty.value || isUploading.value) || window.confirm('知识尚未保存，确定离开并放弃修改？'));
const beforeUnload = (event: BeforeUnloadEvent): void => { if (isDirty.value || isSaving.value || isUploading.value) { event.preventDefault(); event.returnValue = ''; } };
onBeforeRouteLeave(confirmLeave);
onBeforeRouteUpdate(confirmLeave);
window.addEventListener('beforeunload', beforeUnload);
onBeforeUnmount(() => { ++requestSequence; window.removeEventListener('beforeunload', beforeUnload); });
watch(() => [route.params.id, route.query.sourceJournalId], load, { immediate: true });
</script>
<style scoped>
textarea { width: 100%; padding: 10px 12px; border: 1px solid #d7dfd6; border-radius: 8px; color: #243b2c; resize: vertical; }
.editor-page { max-width: 1300px; }
.knowledge-form { display: grid; gap: 22px; }
.knowledge-form__footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
fieldset { border: 1px solid #e1e6df; border-radius: 10px; padding: 14px; }
.checkbox { display: inline-flex; flex-direction: row; align-items: center; margin: 6px 20px 6px 0; gap: 8px; }
.checkbox input { width: auto; }
</style>
