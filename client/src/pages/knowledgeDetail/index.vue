<template>
  <section class="page">
    <header class="page-heading"><router-link to="/knowledge">← 返回知识库</router-link><div v-if="entry" class="actions"><router-link class="button" :to="`/knowledge/${entry.id}/edit`">编辑知识</router-link><button class="danger-link" :disabled="isDeleting" @click="handleDelete">删除知识</button></div></header>
    <p v-if="isLoading" role="status">正在加载知识…</p><p v-if="error" class="error" role="alert">{{ error }} <button @click="load">重试</button></p>
    <article v-if="entry" class="panel knowledge-detail">
      <p class="muted">{{ entry.category?.name || '未分类' }} · 更新于 {{ entry.updatedAt }}</p><h1>{{ entry.title }}</h1>
      <p v-if="entry.summary" class="knowledge-detail__summary">{{ entry.summary }}</p>
      <p v-if="entry.sourceJournalTitle" class="muted">来源：<router-link v-if="entry.sourceJournalId" :to="`/journals/${entry.sourceJournalId}`">{{ entry.sourceJournalTitle }}</router-link><span v-else>{{ entry.sourceJournalTitle }}（来源日志已删除）</span></p>
      <div class="tags"><router-link v-for="tag in entry.tags" :key="tag.id" class="tag" :to="{ path: '/knowledge', query: { tagId: tag.id } }"># {{ tag.name }}</router-link></div>
      <md-preview :model-value="entry.contentMd" :sanitize="sanitizeMarkdown" no-highlight no-mermaid no-katex />
    </article>
    <related-knowledge v-if="entry" :key="entry.id" :knowledge-id="entry.id" />
  </section>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';
import RelatedKnowledge from './components/RelatedKnowledge.vue';
import { knowledgeApi } from '@/api/knowledge';
import { sanitizeMarkdown } from '@/utils/markdownSecurity';
import type { KnowledgeDetail } from '@/types/knowledge.types';
const route = useRoute(); const router = useRouter();
const entry = ref<KnowledgeDetail | null>(null); const isLoading = ref(false); const isDeleting = ref(false); const error = ref('');
let sequence = 0;
const load = async (): Promise<void> => {
  const current = ++sequence; isLoading.value = true; error.value = ''; entry.value = null;
  try {
    const id = Number(route.params.id);
    if (!Number.isSafeInteger(id) || id <= 0) { throw new Error('知识编号无效'); }
    const data = await knowledgeApi.get(id); if (current === sequence) { entry.value = data; }
  } catch (reason: unknown) { if (current === sequence) { error.value = reason instanceof Error ? reason.message : '加载失败'; } }
  finally { if (current === sequence) { isLoading.value = false; } }
};
const handleDelete = async (): Promise<void> => {
  if (!entry.value || isDeleting.value || !window.confirm('确定删除这条知识？删除后无法恢复。')) { return; }
  const id = entry.value.id; const current = sequence; isDeleting.value = true; error.value = '';
  try { await knowledgeApi.remove(id); if (current === sequence) { await router.push('/knowledge'); } }
  catch (reason: unknown) { if (current === sequence) { error.value = reason instanceof Error ? reason.message : '删除失败'; } }
  finally { isDeleting.value = false; }
};
watch(() => route.params.id, load, { immediate: true });
onBeforeUnmount(() => { ++sequence; });
</script>
<style scoped>
.knowledge-detail { padding: 36px; }
.knowledge-detail h1 { font-family: Georgia, 'Songti SC', serif; font-size: 36px; line-height: 1.4; overflow-wrap: anywhere; }
.knowledge-detail__summary { color: #69766d; white-space: pre-wrap; overflow-wrap: anywhere; }
.knowledge-detail :deep(.md-editor-preview) { overflow-wrap: anywhere; }
.knowledge-detail :deep(img) { max-width: 100%; }
</style>
