import type { Tag } from './journal.types';
export type SearchSource = 'ALL' | 'JOURNAL' | 'KNOWLEDGE';
export interface SearchQuery { keyword?: string; sourceType: SearchSource; tagId?: number; page: number; pageSize: number }
export interface SearchResult { sourceType: Exclude<SearchSource, 'ALL'>; sourceId: number; title: string; excerpt: string; tags: Tag[]; updatedAt: string }
