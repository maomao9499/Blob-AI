import { defineStore } from 'pinia';
import { ref } from 'vue';
import { journalApi } from '@/api/journal';
import type { JournalQuery, JournalSummary } from '@/types/journal.types';

export const useJournalStore = defineStore('journal', () => {
  const items = ref<JournalSummary[]>([]);
  const total = ref(0);
  const isLoading = ref(false);
  const error = ref('');
  let sequence = 0;
  const load = async (query: JournalQuery): Promise<void> => {
    const current = ++sequence;
    isLoading.value = true;
    error.value = '';
    try {
      const page = await journalApi.list(query);
      if (current !== sequence) { return; }
      items.value = page.items;
      total.value = page.total;
    } catch (reason: unknown) {
      if (current !== sequence) { return; }
      items.value = [];
      total.value = 0;
      error.value = reason instanceof Error ? reason.message : '日志加载失败';
    } finally {
      if (current === sequence) { isLoading.value = false; }
    }
  };
  return { items, total, isLoading, error, load };
});
