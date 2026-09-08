import type { Tag } from './journal.types';

export interface KnowledgeCategory { id: number; name: string; description: string | null; createdAt: string; updatedAt: string }
export interface CategoryInput { name: string; description: string | null }
export interface KnowledgeInput { title: string; contentMd: string; summary: string | null; categoryId: number | null; tagIds: number[] }
export interface KnowledgeSummary {
  id: number; title: string; summary: string | null; excerpt: string;
  category: { id: number; name: string } | null; tags: Tag[]; createdAt: string; updatedAt: string;
}
export interface KnowledgeDetail extends KnowledgeSummary {
  contentMd: string; sourceJournalId: number | null; sourceJournalTitle: string | null;
}
export interface KnowledgeQuery {
  keyword?: string; categoryId?: number; uncategorized?: boolean; tagId?: number; page: number; pageSize: number;
}
export interface KnowledgeRelation { id: number; knowledgeId: number; title: string }
export interface RelationInput { sourceKnowledgeId: number; targetKnowledgeId: number; relationType: 'RELATED' }
