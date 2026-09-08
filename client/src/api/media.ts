import { http } from './http';
import type { ImageUploadResponse } from '@/types/journal.types';
export const mediaApi = {
  uploadImage: (file: File): Promise<ImageUploadResponse> => {
    const data = new FormData();
    data.append('file', file);
    return http.post('/media/images', data);
  },
};
