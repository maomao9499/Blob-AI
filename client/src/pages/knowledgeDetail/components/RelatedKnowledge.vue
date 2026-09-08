<template>
  <section class="panel related-knowledge" aria-label="相关知识">
    <header class="page-heading"><h2>相关知识</h2><button class="primary" @click="isDialogOpen = true">关联知识</button></header>
    <p v-if="error || actionError" class="error" role="alert">{{ error || actionError }} <button @click="load">重试</button></p>
    <p v-if="isLoading" role="status">正在加载关联…</p>
    <p v-else-if="!items.length && !error" class="muted">还没有相关知识。</p>
    <ul v-else class="relations"><li v-for="item in items" :key="item.id"><router-link :to="`/knowledge/${item.knowledgeId}`">{{ item.title }}</router-link><button class="danger-link" :disabled="isRemoving" @click="handleRemove(item.id)">解除关联</button></li></ul>
    <content-pagination :page="page" :page-size="PAGE_SIZE" :total="total" :is-loading="isLoading" @change="page = $event" />
    <relation-dialog v-if="isDialogOpen" :knowledge-id="knowledgeId" @close="isDialogOpen = false" @saved="handleSaved" />
  </section>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue';
import ContentPagination from '@/components/ContentPagination.vue';
import RelationDialog from './RelationDialog.vue';
import { usePagedRequest } from '@/composables/common/usePagedRequest';
import { knowledgeApi } from '@/api/knowledge';
import { PAGE_SIZE } from '@/utils/contentQuery';
const props = defineProps<{ knowledgeId: number }>();
const page = ref(1); const isDialogOpen = ref(false); const isRemoving = ref(false); const actionError = ref('');
const query = computed(() => ({ id: props.knowledgeId, page: page.value, pageSize: PAGE_SIZE }));
const { items, total, error, isLoading, load } = usePagedRequest({ query, request: (input) => knowledgeApi.relations(input.id, input.page, input.pageSize), correctPage: async (value) => { page.value = value; } });
const handleSaved = async (): Promise<void> => { isDialogOpen.value = false; await load(); };
const handleRemove = async (id: number): Promise<void> => {
  if (isRemoving.value || !window.confirm('确定解除这条关联？两条知识都会保留。')) { return; }
  isRemoving.value = true; actionError.value = '';
  try { await knowledgeApi.unrelate(id); await load(); }
  catch (reason: unknown) { actionError.value = reason instanceof Error ? reason.message : '解除关联失败'; }
  finally { isRemoving.value = false; }
};
</script>
<style scoped>
.related-knowledge { margin-top: 24px; }
.relations { list-style: none; padding: 0; }
.relations li { display: flex; justify-content: space-between; gap: 16px; padding: 12px 0; overflow-wrap: anywhere; }
</style>
