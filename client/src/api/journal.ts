import { http } from './http';
import type { JournalQuery, JournalInput, JournalDetail, JournalSummary, PageResponse } from '@/types/journal.types';

export const journalApi = {
  list: (query: JournalQuery): Promise<PageResponse<JournalSummary>> => http.get('/journals', { params: query }),
  get: (id: number): Promise<JournalDetail> => http.get(`/journals/${id}`),
  create: (input: JournalInput): Promise<{ id: number }> => http.post('/journals', input),
  update: (id: number, input: JournalInput): Promise<void> => http.put(`/journals/${id}`, input),
  remove: (id: number): Promise<void> => http.delete(`/journals/${id}`),
  recent: (): Promise<string[]> => http.get('/search/recent'),
};
