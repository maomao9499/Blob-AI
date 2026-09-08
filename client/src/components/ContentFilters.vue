<template>
  <form class="panel content-filters" @submit.prevent="$emit('search', { ...draft })">
    <label>关键词<input v-model="draft.keyword" name="keyword" placeholder="搜索标题、正文或标签" /></label>
    <label v-if="isSearch">内容类型<select v-model="draft.sourceType" name="sourceType"><option value="ALL">全部</option><option value="JOURNAL">日志</option><option value="KNOWLEDGE">知识</option></select></label>
    <label v-else>分类<select v-model="draft.category" name="category"><option value="">全部分类</option><option value="uncategorized">未分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
    <label>标签<select v-model="draft.tagId" name="tagId"><option value="">全部标签</option><option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option></select></label>
    <div class="actions"><button class="primary">搜索</button><button type="button" @click="handleReset">重置</button></div>
  </form>
</template>
<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { ContentFilterProps, ContentFilters } from '@/types/contentQuery.types';
const props = defineProps<ContentFilterProps>();
const emit = defineEmits<{ search: [filters: ContentFilters]; reset: [] }>();
const draft = reactive<ContentFilters>({ ...props.filters });
watch(() => props.filters, (filters) => { Object.assign(draft, filters); });
const handleReset = (): void => {
  Object.assign(draft, { keyword: '', tagId: '', category: '', sourceType: 'ALL' } satisfies ContentFilters);
  emit('reset');
};
</script>
<style scoped>
.content-filters { display: grid; grid-template-columns: minmax(180px, 2fr) repeat(2, minmax(120px, 1fr)) auto; align-items: end; gap: 16px; margin-bottom: 24px; }
@media (max-width: 1000px) { .content-filters { grid-template-columns: 1fr 1fr; } }
@media (max-width: 600px) { .content-filters { grid-template-columns: 1fr; } }
</style>
