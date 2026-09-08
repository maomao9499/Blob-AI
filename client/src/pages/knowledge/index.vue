<template>
  <section class="page">
    <header class="page-heading"><div><p class="eyebrow">KNOWLEDGE THAT STAYS</p><h1>知识库</h1><p class="muted">整理值得长期复用的思考。</p></div><div class="actions"><router-link to="/knowledge/categories">分类管理</router-link><router-link class="button primary" to="/knowledge/new">新建知识</router-link></div></header>
    <content-filters :filters="filters" :tags="tags" :categories="categories" @search="handleSearch" @reset="handleReset" />
    <p v-if="metadataError" role="alert" class="error">{{ metadataError }} <button @click="loadMetadata">重试筛选项</button></p>
    <p v-if="error || actionError" role="alert" class="error">{{ error || actionError }} <button @click="load">重试</button></p>
    <p v-if="isLoading" role="status">正在加载知识…</p>
    <div v-else-if="!items.length && !error" class="panel empty"><h2>这里还没有知识</h2><p class="muted">调整筛选条件，或手动整理一条新知识。</p></div>
    <div v-else class="knowledge-list" :aria-busy="isLoading">
      <article v-for="entry in items" :key="entry.id" class="panel knowledge-card">
        <p class="muted">{{ entry.category?.name || '未分类' }} · 更新于 {{ entry.updatedAt }}</p>
        <h2><router-link :to="`/knowledge/${entry.id}`">{{ entry.title }}</router-link></h2>
        <p class="knowledge-card__excerpt">{{ entry.summary || entry.excerpt }}</p>
        <footer class="knowledge-card__footer"><div class="tags"><button v-for="tag in entry.tags" :key="tag.id" class="tag" @click="handleSearch({ ...filters, tagId: tag.id })"># {{ tag.name }}</button></div><div class="actions"><router-link :to="`/knowledge/${entry.id}/edit`">编辑</router-link><button class="danger-link" :disabled="deletingId !== null" @click="handleDelete(entry.id)">删除</button></div></footer>
      </article>
    </div>
    <content-pagination :page="query.page" :page-size="query.pageSize" :total="total" :is-loading="isLoading" @change="changePage" />
  </section>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ContentFilters from '@/components/ContentFilters.vue';
import ContentPagination from '@/components/ContentPagination.vue';
import { usePagedRequest } from '@/composables/common/usePagedRequest';
import { categoryApi, knowledgeApi } from '@/api/knowledge';
import { tagApi } from '@/api/tag';
import { PAGE_SIZE, positiveId, queryText, routeQuery } from '@/utils/contentQuery';
import type { KnowledgeCategory, KnowledgeQuery } from '@/types/knowledge.types';
import type { Tag } from '@/types/journal.types';
import type { ContentFilters as FilterInput } from '@/types/contentQuery.types';
const route = useRoute(); const router = useRouter();
const tags = ref<Tag[]>([]); const categories = ref<KnowledgeCategory[]>([]);
const metadataError = ref(''); const actionError = ref(''); const deletingId = ref<number | null>(null);
const query = computed<KnowledgeQuery>(() => {
  const categoryId = positiveId(queryText(route.query, 'categoryId'));
  return { keyword: queryText(route.query, 'keyword') || undefined, categoryId,
    uncategorized: !categoryId && queryText(route.query, 'uncategorized') === 'true' ? true : undefined,
    tagId: positiveId(queryText(route.query, 'tagId')), page: positiveId(queryText(route.query, 'page')) || 1, pageSize: PAGE_SIZE };
});
const filters = computed<FilterInput>(() => ({ keyword: query.value.keyword || '', category: query.value.categoryId ?? (query.value.uncategorized ? 'uncategorized' : ''), tagId: query.value.tagId ?? '', sourceType: 'ALL' }));
const changePage = async (page: number): Promise<void> => { await router.push({ path: '/knowledge', query: routeQuery({ ...query.value, page }) }); };
const { items, total, isLoading, error, load } = usePagedRequest({ query, request: knowledgeApi.list, correctPage: async (page) => { await router.replace({ path: '/knowledge', query: routeQuery({ ...query.value, page }) }); } });
const handleSearch = async (input: FilterInput): Promise<void> => {
  const next: KnowledgeQuery = { keyword: input.keyword.trim() || undefined, categoryId: typeof input.category === 'number' ? input.category : undefined, uncategorized: input.category === 'uncategorized' ? true : undefined, tagId: input.tagId || undefined, page: 1, pageSize: PAGE_SIZE };
  const target = { path: '/knowledge', query: routeQuery(next) };
  if (router.resolve(target).fullPath === route.fullPath) { await load(); return; }
  await router.push(target);
};
const handleReset = async (): Promise<void> => { if (route.fullPath === '/knowledge') { await load(); } else { await router.push('/knowledge'); } };
const handleDelete = async (id: number): Promise<void> => {
  if (deletingId.value !== null || !window.confirm('确定删除这条知识？删除后无法恢复。')) { return; }
  deletingId.value = id; actionError.value = '';
  try { await knowledgeApi.remove(id); await load(); } catch (reason: unknown) { actionError.value = reason instanceof Error ? reason.message : '删除失败'; } finally { deletingId.value = null; }
};
const loadMetadata = async (): Promise<void> => {
  metadataError.value = '';
  try { [tags.value, categories.value] = await Promise.all([tagApi.list(), categoryApi.list()]); }
  catch (reason: unknown) { metadataError.value = reason instanceof Error ? reason.message : '筛选项加载失败'; }
};
onMounted(loadMetadata);
</script>
<style scoped>
.knowledge-list { display: grid; gap: 18px; }
.knowledge-card h2 { font-size: 23px; overflow-wrap: anywhere; }
.knowledge-card h2 a { color: #263e2f; text-decoration: none; }
.knowledge-card__excerpt { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; color: #69766d; }
.knowledge-card__footer { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 24px; }
.empty { text-align: center; padding: 64px; }
</style>
