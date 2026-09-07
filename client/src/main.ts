import 'element-plus/dist/index.css';
import '@/styles/global.css';

import ElementPlus from 'element-plus';
import { createApp } from 'vue';

import App from '@/App.vue';
import { setAccessToken } from '@/api/http';
import { router } from '@/router';
import { pinia } from '@/stores';

const desktopToken = window.blobDesktop?.accessToken;
if (desktopToken) {
  setAccessToken(desktopToken);
}

createApp(App).use(pinia).use(router).use(ElementPlus).mount('#app');
