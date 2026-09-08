import { createRouter, createWebHashHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/knowledge', component: () => import('@/pages/knowledge/index.vue') },
    { path: '/knowledge/categories', component: () => import('@/pages/knowledgeCategories/index.vue') },
    { path: '/knowledge/new', component: () => import('@/pages/knowledgeEdit/index.vue') },
    { path: '/knowledge/:id/edit', component: () => import('@/pages/knowledgeEdit/index.vue') },
    { path: '/knowledge/:id', component: () => import('@/pages/knowledgeDetail/index.vue') },
    { path: '/search', component: () => import('@/pages/search/index.vue') },
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
