import 'element-plus/dist/index.css';
import '@/styles/global.css';

import { ElButton, ElCard, ElTag } from 'element-plus';
import { createApp } from 'vue';

import App from '@/App.vue';
import { configureBackend } from '@/api/http';
import { router } from '@/router';
import { pinia } from '@/stores';

const start = async (): Promise<void> => {
  const desktop = window.blobDesktop;
  if (desktop) {
    try {
      const bootstrap = await desktop.getBootstrap();
      if (bootstrap.apiBaseUrl) { configureBackend(bootstrap.apiBaseUrl, bootstrap.sessionToken); }
      if (bootstrap.setupRequired) { await router.replace('/settings'); }
    } catch { await router.replace('/settings'); }
  }
  createApp(App).use(pinia).use(router).component('ElButton', ElButton).component('ElCard', ElCard).component('ElTag', ElTag).mount('#app');
};
void start();
