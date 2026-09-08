export interface Tag { id: number; name: string; color: string | null }
export type EntryType = 'LEARNING' | 'LIFE';
export interface JournalSummary {
  id: number; title: string; excerpt: string; entryType: EntryType; entryDate: string;
  aiSummary: string | null; tags: Tag[]; createdAt: string; updatedAt: string;
}
export interface JournalDetail extends JournalSummary { contentMd: string }
export interface JournalInput {
  title: string; contentMd: string; entryType: EntryType; entryDate: string; tagIds: number[];
}
export interface JournalQuery {
  keyword?: string; entryType?: EntryType; tagId?: number; startDate?: string; endDate?: string;
  page: number; pageSize: number;
}
export interface PageResponse<T> { items: T[]; total: number; page: number; pageSize: number }
export interface TagInput { name: string; color: string | null }
export interface ImageUploadResponse { id: number; url: string; markdown: string }
