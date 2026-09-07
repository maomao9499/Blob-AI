import { defineStore } from 'pinia';
import { ref } from 'vue';

import { systemApi } from '@/api/system';
import type { BackendConnectionStatus } from '@/stores/types';

export const useApplicationStore = defineStore('application', () => {
  const backendStatus = ref<BackendConnectionStatus>('checking');

  const checkBackend = async (): Promise<void> => {
    backendStatus.value = 'checking';
    try {
      await systemApi.ping();
      backendStatus.value = 'online';
    } catch (_error: unknown) {
      backendStatus.value = 'offline';
    }
  };

  return { backendStatus, checkBackend };
});
