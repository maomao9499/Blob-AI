import { onScopeDispose, ref, shallowRef, watch } from 'vue';
import type { PagedRequestOptions, PagedRequestState, PaginationQuery } from '@/types/contentQuery.types';

export const usePagedRequest = <T, Q extends PaginationQuery>(options: PagedRequestOptions<T, Q>): PagedRequestState<T> => {
  const items = shallowRef<T[]>([]);
  const total = ref(0);
  const isLoading = ref(false);
  const error = ref('');
  let sequence = 0;
  const load = async (): Promise<void> => {
    const current = ++sequence;
    const query = { ...options.query.value };
    isLoading.value = true; error.value = '';
    try {
      const result = await options.request(query);
      if (current !== sequence) { return; }
      const lastPage = Math.max(1, Math.ceil(result.total / query.pageSize));
      if (query.page > lastPage) { await options.correctPage(lastPage); return; }
      items.value = result.items; total.value = result.total;
    } catch (reason: unknown) {
      if (current === sequence) { items.value = []; error.value = reason instanceof Error ? reason.message : '加载失败'; }
    } finally { if (current === sequence) { isLoading.value = false; } }
  };
  watch(options.query, load, { immediate: true });
  onScopeDispose(() => { ++sequence; });
  return { items, total, isLoading, error, load };
};
