import { defineStore } from 'pinia';
import { ref } from 'vue';

// 会话令牌仅存在内存中，不安装持久化插件。
export const useDesktopStore = defineStore('desktop', () => {
  const sessionToken = ref<string | null>(null);
  const apiBaseUrl = ref('/api/v1');
  const configure = (baseUrl: string, token: string | null): void => {
    apiBaseUrl.value = baseUrl;
    sessionToken.value = token;
  };
  return { sessionToken, apiBaseUrl, configure };
});
