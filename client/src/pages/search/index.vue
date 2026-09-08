<template>
  <section class="page">
    <header class="page-heading"><div><p class="eyebrow">FIND YOUR THOUGHTS</p><h1>搜索</h1><p class="muted">一起查找日志与知识中的标题、正文和标签。</p></div></header>
    <content-filters :filters="filters" :tags="tags" is-search @search="handleSearch" @reset="handleReset" />
    <p v-if="tagError" class="error" role="alert">{{ tagError }} <button @click="loadTags">重试标签</button></p>
    <p v-if="error" class="error" role="alert">{{ error }} <button @click="load">重试</button></p>
    <p v-if="isLoading" role="status">正在搜索…</p>
    <p v-else-if="!items.length && !error" class="panel muted">没有找到内容，试试其他关键词或筛选条件。</p>
    <div v-else class="results" :aria-busy="isLoading">
      <article v-for="item in items" :key="`${item.sourceType}-${item.sourceId}`" class="panel result-card">
        <p class="muted">{{ item.sourceType === 'JOURNAL' ? '日志' : '知识' }} · 更新于 {{ item.updatedAt }}</p>
        <h2><router-link :to="detailPath(item)">{{ item.title }}</router-link></h2>
        <p class="result-card__excerpt">{{ item.excerpt }}</p>
        <div class="tags"><button v-for="tag in item.tags" :key="tag.id" class="tag" @click="handleSearch({ ...filters, tagId: tag.id })"># {{ tag.name }}</button></div>
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
import { searchApi } from '@/api/search';
import { tagApi } from '@/api/tag';
import { PAGE_SIZE, positiveId, queryText, routeQuery } from '@/utils/contentQuery';
import type { Tag } from '@/types/journal.types';
import type { SearchQuery, SearchResult } from '@/types/search.types';
import type { ContentFilters as FilterInput } from '@/types/contentQuery.types';
const route = useRoute(); const router = useRouter();
const tags = ref<Tag[]>([]); const tagError = ref('');
const query = computed<SearchQuery>(() => {
  const type = queryText(route.query, 'sourceType');
  return { keyword: queryText(route.query, 'keyword') || undefined, sourceType: type === 'JOURNAL' || type === 'KNOWLEDGE' ? type : 'ALL', tagId: positiveId(queryText(route.query, 'tagId')), page: positiveId(queryText(route.query, 'page')) || 1, pageSize: PAGE_SIZE };
});
const filters = computed<FilterInput>(() => ({ keyword: query.value.keyword || '', tagId: query.value.tagId ?? '', category: '', sourceType: query.value.sourceType }));
const changePage = async (page: number): Promise<void> => { await router.push({ path: '/search', query: routeQuery({ ...query.value, page }) }); };
const { items, total, isLoading, error, load } = usePagedRequest({ query, request: searchApi.list, correctPage: async (page) => { await router.replace({ path: '/search', query: routeQuery({ ...query.value, page }) }); } });
const detailPath = (item: SearchResult): string => `/${item.sourceType === 'JOURNAL' ? 'journals' : 'knowledge'}/${item.sourceId}`;
const handleSearch = async (input: FilterInput): Promise<void> => {
  const next: SearchQuery = { keyword: input.keyword.trim() || undefined, tagId: input.tagId || undefined, sourceType: input.sourceType, page: 1, pageSize: PAGE_SIZE };
  const target = { path: '/search', query: routeQuery(next) };
  if (router.resolve(target).fullPath === route.fullPath) { await load(); return; }
  await router.push(target);
};
const handleReset = async (): Promise<void> => { if (route.fullPath === '/search') { await load(); } else { await router.push('/search'); } };
const loadTags = async (): Promise<void> => {
  tagError.value = '';
  try { tags.value = await tagApi.list(); } catch (reason: unknown) { tagError.value = reason instanceof Error ? reason.message : '标签加载失败'; }
};
onMounted(loadTags);
</script>
<style scoped>
.results { display: grid; gap: 18px; }
.result-card h2 { font-size: 23px; overflow-wrap: anywhere; }
.result-card h2 a { color: #263e2f; text-decoration: none; }
.result-card__excerpt { line-height: 1.8; color: #69766d; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
