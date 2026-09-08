<template>
  <section class="page">
    <header class="page-heading"><div><p class="eyebrow">YOUR EVERYDAY NOTES</p><h1>日志时间线</h1><p class="muted">把今天的思考留下来，让知识与生活慢慢积累。</p></div><router-link class="button primary" to="/journals/new">写日志</router-link></header>
    <search-form :query="query" :tags="tags" :is-loading="store.isLoading" @search="handleSearch" @reset="handleReset" />
    <div v-if="recent.length" class="recent"><span class="muted">最近搜索</span><button v-for="word in recent" :key="word" @click="handleSearch({ ...query, keyword: word, page: 1 })">{{ word }}</button></div>
    <p v-if="tagError" class="error" role="alert">{{ tagError }}</p>
    <p v-if="actionError || store.error" class="error" role="alert">{{ actionError || store.error }} <button @click="load">重试</button></p>
    <p v-if="store.isLoading" role="status">正在加载日志…</p>
    <div v-else-if="!store.items.length && !store.error" class="panel empty"><h2>这里还没有日志</h2><p class="muted">试试调整筛选条件，或写下你的第一篇记录。</p><router-link to="/journals/new">开始记录</router-link></div>
    <div v-else class="timeline" :aria-busy="store.isLoading">
      <article v-for="entry in store.items" :key="entry.id" class="panel journal-card">
        <div class="journal-card__meta"><time>{{ entry.entryDate }}</time><span>{{ entry.entryType === 'LEARNING' ? '学习日志' : '生活日志' }}</span></div>
        <h2><router-link :to="`/journals/${entry.id}`">{{ entry.title }}</router-link></h2>
        <p class="journal-card__excerpt">{{ entry.excerpt }}</p>
        <div class="journal-card__footer"><div class="tags"><button v-for="tag in entry.tags" :key="tag.id" class="tag" @click="handleSearch({ ...query, tagId: tag.id, page: 1 })"># {{ tag.name }}</button></div><div class="actions"><router-link :to="`/journals/${entry.id}/edit`">编辑</router-link><button class="danger-link" :disabled="deletingId === entry.id" @click="handleDelete(entry.id)">删除</button></div></div>
      </article>
    </div>
    <footer class="pagination"><span class="muted">共 {{ store.total }} 篇 · 第 {{ query.page }} 页</span><div class="actions"><button :disabled="query.page <= 1 || store.isLoading" @click="handleSearch({ ...query, page: query.page - 1 })">上一页</button><button :disabled="query.page * query.pageSize >= store.total || store.isLoading" @click="handleSearch({ ...query, page: query.page + 1 })">下一页</button></div></footer>
  </section>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router';
import SearchForm from './components/SearchForm.vue';
import { useJournalStore } from '@/stores/modules/journal';
import { journalApi } from '@/api/journal';
import { tagApi } from '@/api/tag';
import type { JournalQuery, Tag } from '@/types/journal.types';
const route = useRoute();
const router = useRouter();
const store = useJournalStore();
const tags = ref<Tag[]>([]);
const recent = ref<string[]>([]);
const tagError = ref('');
const actionError = ref('');
const deletingId = ref<number | null>(null);
const query = computed<JournalQuery>(() => {
  const q = route.query;
  const text = (key: string): string | undefined => typeof q[key] === 'string' && q[key] ? q[key] : undefined;
  const type = text('entryType');
  const positive = (value: string | undefined, fallback: number): number => Number.isSafeInteger(Number(value)) && Number(value) > 0 ? Number(value) : fallback;
  return { keyword: text('keyword'), entryType: type === 'LEARNING' || type === 'LIFE' ? type : undefined, tagId: text('tagId') ? positive(text('tagId'), 0) || undefined : undefined, startDate: text('startDate'), endDate: text('endDate'), page: positive(text('page'), 1), pageSize: 20 };
});
const load = async (): Promise<void> => {
  await store.load(query.value);
  try { recent.value = await journalApi.recent(); } catch { recent.value = []; }
};
const handleSearch = async (next: JournalQuery): Promise<void> => {
  const routeQuery: LocationQueryRaw = {};
  for (const [key, value] of Object.entries(next)) { if (value !== undefined && value !== '') { routeQuery[key] = String(value); } }
  await router.push({ path: '/', query: routeQuery });
};
const handleReset = async (): Promise<void> => { await router.push('/'); };
const handleDelete = async (id: number): Promise<void> => {
  if (deletingId.value || !window.confirm('确定删除这篇日志？删除后无法恢复。')) { return; }
  deletingId.value = id; actionError.value = '';
  try { await journalApi.remove(id); await load(); } catch (error: unknown) { actionError.value = error instanceof Error ? error.message : '删除失败'; } finally { deletingId.value = null; }
};
watch(query, load, { immediate: true });
onMounted(async (): Promise<void> => {
  try { tags.value = await tagApi.list(); } catch { tagError.value = '标签暂时加载失败，可稍后刷新页面。'; }
});
</script>
<style scoped>
.timeline { display: grid; gap: 18px; }
.recent { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; align-items: center; }
.journal-card__meta { display: flex; gap: 16px; color: #6f8275; font-size: 13px; }
.journal-card h2 { font-size: 23px; font-weight: 600; margin: 16px 0 10px; overflow-wrap: anywhere; }
.journal-card h2 a { color: #263e2f; text-decoration: none; }
.journal-card__excerpt { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; color: #69766d; }
.journal-card__footer, .pagination { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 24px; }
.empty { text-align: center; padding: 64px; }
</style>
