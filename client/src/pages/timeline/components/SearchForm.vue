<template>
  <form class="search-form panel" @submit.prevent="handleSearch">
    <label>关键词<input v-model="form.keyword" name="keyword" placeholder="搜索标题、正文或标签" /></label>
    <label>类型<select v-model="form.entryType" name="entryType"><option value="">全部类型</option><option value="LEARNING">学习日志</option><option value="LIFE">生活日志</option></select></label>
    <label>标签<select v-model="form.tagId" name="tagId"><option value="">全部标签</option><option v-for="tag in tags" :key="tag.id" :value="String(tag.id)">{{ tag.name }}</option></select></label>
    <label>开始日期<input v-model="form.startDate" type="date" name="startDate" /></label>
    <label>结束日期<input v-model="form.endDate" type="date" name="endDate" /></label>
    <div class="actions"><button class="primary" :disabled="isLoading">搜索</button><button type="button" :disabled="isLoading" @click="emit('reset')">清空</button></div>
  </form>
</template>
<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { JournalQuery, Tag } from '@/types/journal.types';
const props = defineProps<{ query: JournalQuery; tags: Tag[]; isLoading: boolean }>();
const emit = defineEmits<{ search: [query: JournalQuery]; reset: [] }>();
const form = reactive({ keyword: '', entryType: '', tagId: '', startDate: '', endDate: '' });
watch(() => props.query, (query) => {
  Object.assign(form, { keyword: query.keyword ?? '', entryType: query.entryType ?? '', tagId: query.tagId ? String(query.tagId) : '', startDate: query.startDate ?? '', endDate: query.endDate ?? '' });
}, { immediate: true });
const handleSearch = (): void => emit('search', {
  keyword: form.keyword.trim() || undefined,
  entryType: form.entryType === 'LEARNING' || form.entryType === 'LIFE' ? form.entryType : undefined,
  tagId: form.tagId ? Number(form.tagId) : undefined,
  startDate: form.startDate || undefined, endDate: form.endDate || undefined,
  page: 1, pageSize: props.query.pageSize,
});
</script>
<style scoped>
.search-form { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 16px; margin-bottom: 24px; }
@media (max-width: 900px) { .search-form { grid-template-columns: 1fr 1fr; } }
</style>
