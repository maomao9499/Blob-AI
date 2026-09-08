<template>
  <section class="page editor-page"><header class="page-heading"><div><p class="eyebrow">A LITTLE EVERY DAY</p><h1>{{ isEditing ? '编辑日志' : '写日志' }}</h1></div><router-link to="/">返回时间线</router-link></header>
    <p v-if="isLoading" role="status">正在加载…</p><p v-if="notice" role="status">{{ notice }}</p><p v-if="error" class="error" role="alert">{{ error }}</p>
    <form v-if="isReady" class="panel journal-form" @submit.prevent="handleSave">
      <label>标题<input v-model="form.title" name="title" placeholder="今天有什么值得记录？" maxlength="200" required /></label>
      <div class="journal-form__metadata"><label>日期<input v-model="form.entryDate" name="entryDate" type="date" required /></label><label>类型<select v-model="form.entryType" name="entryType"><option value="LEARNING">学习日志</option><option value="LIFE">生活日志</option></select></label></div>
      <fieldset><legend>标签</legend><p v-if="!tags.length" class="muted">还没有标签，可稍后在标签管理中创建。</p><label v-for="tag in tags" :key="tag.id" class="checkbox"><input v-model="form.tagIds" type="checkbox" :value="tag.id" />{{ tag.name }}</label></fieldset>
      <markdown-editor v-model="form.contentMd" @uploading="isUploading = $event" />
      <footer class="journal-form__footer"><span class="muted">{{ isDirty ? '有未保存的修改' : '修改后记得保存' }}</span><button class="primary" :disabled="isSaving || isUploading">{{ isSaving ? '正在保存…' : '保存日志' }}</button></footer>
    </form>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { journalApi } from '@/api/journal';
import { tagApi } from '@/api/tag';
import type { JournalInput, Tag } from '@/types/journal.types';
const route = useRoute(); const router = useRouter();
const today = (): string => { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`; };
const emptyInput = (): JournalInput => ({ title: '', entryType: 'LEARNING', entryDate: today(), contentMd: '', tagIds: [] });
const form = reactive<JournalInput>(emptyInput());
const tags = ref<Tag[]>([]); const error = ref(''); const baseline = ref(JSON.stringify(form));
const isLoading = ref(false); const isSaving = ref(false); const isUploading = ref(false); const isReady = ref(false);
const savedId = ref<number | null>(null); const notice = ref('');
const isEditing = computed(() => Boolean(route.params.id) || savedId.value !== null);
const isDirty = computed(() => isReady.value && baseline.value !== JSON.stringify(form));
let requestSequence = 0;
const load = async (): Promise<void> => {
  const sequence = ++requestSequence; isLoading.value = true; isReady.value = false; error.value = ''; savedId.value = null; notice.value = '';
  try {
    const id = Number(route.params.id);
    if (isEditing.value && (!Number.isSafeInteger(id) || id <= 0)) { throw new Error('日志编号无效'); }
    const [availableTags, entry] = await Promise.all([tagApi.list(), isEditing.value ? journalApi.get(id) : Promise.resolve(null)]);
    if (sequence !== requestSequence) { return; }
    tags.value = availableTags;
    Object.assign(form, entry ? { title: entry.title, entryType: entry.entryType, entryDate: entry.entryDate, contentMd: entry.contentMd, tagIds: entry.tags.map((tag) => tag.id) } : emptyInput());
    baseline.value = JSON.stringify(form); isReady.value = true;
  } catch (reason: unknown) { if (sequence === requestSequence) { error.value = reason instanceof Error ? reason.message : '加载失败'; } } finally { if (sequence === requestSequence) { isLoading.value = false; } }
};
const handleSave = async (): Promise<void> => {
  if (isSaving.value || isUploading.value) { return; }
  if (!form.title.trim() || !form.contentMd.trim() || !form.entryDate) { error.value = '请填写标题、日期和正文'; return; }
  isSaving.value = true; error.value = '';
  const submittedSnapshot = JSON.stringify(form);
  const targetId = savedId.value ?? (route.params.id ? Number(route.params.id) : null);
  try {
    const input: JournalInput = { ...form, title: form.title.trim(), tagIds: [...form.tagIds] };
    const id = targetId ?? (await journalApi.create(input)).id;
    if (targetId !== null) { await journalApi.update(id, input); }
    savedId.value = id;
    baseline.value = submittedSnapshot;
    if (JSON.stringify(form) !== submittedSnapshot) { notice.value = '已保存提交时的内容，后续修改仍未保存。'; return; }
    isSaving.value = false;
    await router.push(`/journals/${id}`);
  } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '保存失败'; } finally { isSaving.value = false; }
};
const confirmLeave = (): boolean => !isSaving.value && (!isDirty.value || window.confirm('日志尚未保存，确定离开并放弃修改？'));
const beforeUnload = (event: BeforeUnloadEvent): void => { if (isDirty.value) { event.preventDefault(); event.returnValue = ''; } };
onBeforeRouteLeave(confirmLeave); onBeforeRouteUpdate(confirmLeave);
window.addEventListener('beforeunload', beforeUnload);
onBeforeUnmount(() => { ++requestSequence; window.removeEventListener('beforeunload', beforeUnload); });
watch(() => route.params.id, load, { immediate: true });
</script>
<style scoped>
.editor-page { max-width: 1300px; }
.journal-form { display: grid; gap: 22px; }
.journal-form__metadata { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.journal-form__footer { display: flex; align-items: center; justify-content: space-between; }
fieldset { border: 1px solid #e1e6df; border-radius: 10px; padding: 14px; }
.checkbox { display: inline-flex; flex-direction: row; align-items: center; margin: 6px 20px 6px 0; gap: 8px; }
.checkbox input { width: auto; }
</style>
