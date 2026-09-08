import type { Ref, ShallowRef } from 'vue';
import type { PageResponse, Tag } from './journal.types';
import type { KnowledgeCategory } from './knowledge.types';
import type { SearchSource } from './search.types';
export interface PaginationQuery { page: number; pageSize: number }
export interface ContentFilters { keyword: string; tagId: number | ''; category: number | '' | 'uncategorized'; sourceType: SearchSource }
export interface ContentFilterProps { filters: ContentFilters; tags: Tag[]; categories?: KnowledgeCategory[]; isSearch?: boolean }
export interface PagedRequestOptions<T, Q extends PaginationQuery> {
  query: Ref<Q>; request: (query: Q) => Promise<PageResponse<T>>; correctPage: (page: number) => Promise<unknown>;
}
export interface PagedRequestState<T> { items: ShallowRef<T[]>; total: Ref<number>; isLoading: Ref<boolean>; error: Ref<string>; load: () => Promise<void> }
