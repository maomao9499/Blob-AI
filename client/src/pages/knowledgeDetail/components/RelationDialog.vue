<template>
  <div class="modal-backdrop"><section class="panel modal relation-modal" role="dialog" aria-modal="true" aria-labelledby="relation-title">
    <header class="page-heading"><h2 id="relation-title">关联知识</h2><button :disabled="isSaving" @click="$emit('close')">取消</button></header>
    <form class="relation-search" @submit.prevent="handleSearch"><label>关联关键词<input v-model="keywordDraft" name="relationKeyword" /></label><button class="primary">搜索关联</button></form>
    <p v-if="error || actionError" class="error" role="alert">{{ error || actionError }} <button v-if="error" @click="load">重试</button></p>
    <p v-if="isLoading" role="status">正在加载候选知识…</p>
    <p v-else-if="!candidates.length" class="muted">本页没有其他知识，可调整关键词或翻页。</p>
    <ul v-else class="candidates"><li v-for="item in candidates" :key="item.id"><span>{{ item.title }}</span><button :disabled="isSaving" @click="handleRelate(item.id)">关联</button></li></ul>
    <content-pagination :page="page" :page-size="PAGE_SIZE" :total="total" :is-loading="isLoading || isSaving" @change="page = $event" />
  </section></div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue';
import ContentPagination from '@/components/ContentPagination.vue';
import { usePagedRequest } from '@/composables/common/usePagedRequest';
import { knowledgeApi } from '@/api/knowledge';
import { PAGE_SIZE } from '@/utils/contentQuery';
import type { KnowledgeQuery } from '@/types/knowledge.types';
const props = defineProps<{ knowledgeId: number }>();
const emit = defineEmits<{ saved: []; close: [] }>();
const keywordDraft = ref(''); const keyword = ref(''); const page = ref(1);
const isSaving = ref(false); const actionError = ref('');
const query = computed<KnowledgeQuery>(() => ({ keyword: keyword.value || undefined, page: page.value, pageSize: PAGE_SIZE }));
const { items, total, error, isLoading, load } = usePagedRequest({ query, request: knowledgeApi.list, correctPage: async (value) => { page.value = value; } });
const candidates = computed(() => items.value.filter((item) => item.id !== props.knowledgeId));
const handleSearch = async (): Promise<void> => {
  const next = keywordDraft.value.trim();
  if (keyword.value === next && page.value === 1) { await load(); return; }
  keyword.value = next; page.value = 1;
};
const handleRelate = async (targetId: number): Promise<void> => {
  if (isSaving.value || targetId === props.knowledgeId) { return; }
  isSaving.value = true; actionError.value = '';
  try { await knowledgeApi.relate({ sourceKnowledgeId: props.knowledgeId, targetKnowledgeId: targetId, relationType: 'RELATED' }); emit('saved'); }
  catch (reason: unknown) { actionError.value = reason instanceof Error ? reason.message : '关联失败'; }
  finally { isSaving.value = false; }
};
</script>
<style scoped>
.relation-modal { max-height: 85vh; overflow-y: auto; }
.relation-search { display: flex; gap: 16px; align-items: end; }
.relation-search label { flex: 1; }
.candidates { list-style: none; padding: 0; }
.candidates li { display: flex; gap: 20px; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #e8ece6; overflow-wrap: anywhere; }
</style>
