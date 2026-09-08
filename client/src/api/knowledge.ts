import { http } from './http';
import type { PageResponse } from '@/types/journal.types';
import type { CategoryInput, KnowledgeCategory, KnowledgeDetail, KnowledgeInput, KnowledgeQuery, KnowledgeRelation, KnowledgeSummary, RelationInput } from '@/types/knowledge.types';

const KNOWLEDGE_PATH = '/knowledge';
const CATEGORY_PATH = `${KNOWLEDGE_PATH}/categories`;
const RELATION_PATH = `${KNOWLEDGE_PATH}/relations`;
export const knowledgeApi = {
  list: (query: KnowledgeQuery): Promise<PageResponse<KnowledgeSummary>> => http.get(KNOWLEDGE_PATH, { params: query }),
  get: (id: number): Promise<KnowledgeDetail> => http.get(`${KNOWLEDGE_PATH}/${id}`),
  create: (input: KnowledgeInput): Promise<{ id: number }> => http.post(KNOWLEDGE_PATH, input),
  update: (id: number, input: KnowledgeInput): Promise<void> => http.put(`${KNOWLEDGE_PATH}/${id}`, input),
  remove: (id: number): Promise<void> => http.delete(`${KNOWLEDGE_PATH}/${id}`),
  promote: (journalId: number, input: KnowledgeInput): Promise<{ id: number }> => http.post(`/journals/${journalId}/promote-to-knowledge`, input),
  relations: (id: number, page: number, pageSize = 20): Promise<PageResponse<KnowledgeRelation>> => http.get(`${KNOWLEDGE_PATH}/${id}/relations`, { params: { page, pageSize } }),
  relate: (input: RelationInput): Promise<{ id: number }> => http.post(RELATION_PATH, input),
  unrelate: (id: number): Promise<void> => http.delete(`${RELATION_PATH}/${id}`),
};
export const categoryApi = {
  list: (): Promise<KnowledgeCategory[]> => http.get(CATEGORY_PATH),
  create: (input: CategoryInput): Promise<{ id: number }> => http.post(CATEGORY_PATH, input),
  update: (id: number, input: CategoryInput): Promise<void> => http.put(`${CATEGORY_PATH}/${id}`, input),
  remove: (id: number): Promise<void> => http.delete(`${CATEGORY_PATH}/${id}`),
};
