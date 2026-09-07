import { createRouter, createWebHashHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/pages/home/index.vue'),
      name: 'home',
    },
    {
      path: '/settings',
      component: () => import('@/pages/settings/index.vue'),
      name: 'settings',
    },
  ],
});
