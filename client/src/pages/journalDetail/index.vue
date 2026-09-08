<template>
  <section class="page"><header class="page-heading"><router-link to="/">← 返回时间线</router-link><div v-if="entry" class="actions"><router-link class="button" :to="{ path: '/knowledge/new', query: { sourceJournalId: entry.id } }">整理为知识</router-link><router-link class="button" :to="`/journals/${entry.id}/edit`">编辑日志</router-link><button class="danger-link" :disabled="isDeleting" @click="handleDelete">删除日志</button></div></header><p v-if="isLoading" role="status">正在加载日志…</p><p v-if="error" class="error" role="alert">{{ error }}</p>
    <article v-if="entry" class="panel journal-detail"><p class="muted">{{ entry.entryDate }} · {{ entry.entryType === 'LEARNING' ? '学习日志' : '生活日志' }}</p><h1>{{ entry.title }}</h1><div class="tags"><router-link v-for="tag in entry.tags" :key="tag.id" class="tag" :to="{ path: '/', query: { tagId: tag.id } }"># {{ tag.name }}</router-link></div><md-preview :model-value="entry.contentMd" :sanitize="sanitizeMarkdown" no-highlight no-mermaid no-katex /></article>
  </section>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';
import { journalApi } from '@/api/journal';
import { sanitizeMarkdown } from '@/utils/markdownSecurity';
import type { JournalDetail } from '@/types/journal.types';
const route = useRoute(); const router = useRouter();
const entry = ref<JournalDetail | null>(null); const isLoading = ref(false); const isDeleting = ref(false); const error = ref('');
let sequence = 0;
const load = async (): Promise<void> => {
  const current = ++sequence; isLoading.value = true; error.value = ''; entry.value = null;
  try { const data = await journalApi.get(Number(route.params.id)); if (current === sequence) { entry.value = data; } } catch (reason: unknown) { if (current === sequence) { error.value = reason instanceof Error ? reason.message : '加载失败'; } } finally { if (current === sequence) { isLoading.value = false; } }
};
const handleDelete = async (): Promise<void> => {
  if (!entry.value || isDeleting.value || !window.confirm('确定删除这篇日志？删除后无法恢复。')) { return; }
  isDeleting.value = true;
  try { await journalApi.remove(entry.value.id); await router.push('/'); } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '删除失败'; } finally { isDeleting.value = false; }
};
watch(() => route.params.id, load, { immediate: true });
</script>
<style scoped>
.journal-detail { padding: 36px; }
.journal-detail h1 { font-family: Georgia, 'Songti SC', serif; font-size: 36px; line-height: 1.4; overflow-wrap: anywhere; }
.journal-detail :deep(.md-editor-preview) { overflow-wrap: anywhere; }
.journal-detail :deep(img) { max-width: 100%; }
</style>
