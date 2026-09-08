import { http } from './http';
import type { PageResponse } from '@/types/journal.types';
import type { SearchQuery, SearchResult } from '@/types/search.types';
export const searchApi = {
  list: (query: SearchQuery): Promise<PageResponse<SearchResult>> => http.get('/search/all', { params: query }),
};
