import { http } from './http';
import type { Tag, TagInput } from '@/types/journal.types';
export const tagApi = {
  list: (): Promise<Tag[]> => http.get('/tags'),
  create: (input: TagInput): Promise<{ id: number }> => http.post('/tags', input),
  update: (id: number, input: TagInput): Promise<void> => http.put(`/tags/${id}`, input),
  remove: (id: number): Promise<void> => http.delete(`/tags/${id}`),
};
