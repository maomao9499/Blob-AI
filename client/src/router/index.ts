import { createRouter, createWebHashHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/tags', component: () => import('@/pages/tags/index.vue') },
    { path: '/journals/new', component: () => import('@/pages/journalEdit/index.vue') },
    { path: '/journals/:id/edit', component: () => import('@/pages/journalEdit/index.vue') },
    { path: '/journals/:id', component: () => import('@/pages/journalDetail/index.vue') },
    {
      path: '/',
      component: () => import('@/pages/timeline/index.vue'),
      name: 'home',
    },
    {
      path: '/settings',
      component: () => import('@/pages/settings/index.vue'),
      name: 'settings',
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});
