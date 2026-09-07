<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <router-link class="app-shell__brand" to="/">
        <span class="app-shell__brand-mark">B</span>
        <span>Blob</span>
      </router-link>

      <nav aria-label="主导航" class="app-shell__navigation">
        <router-link
          v-for="item in navigationItems"
          :key="item.label"
          :class="['app-shell__navigation-item', { 'is-disabled': item.isDisabled }]"
          :to="item.path"
          @click="handleNavigation(item, $event)"
        >
          <span>{{ item.label }}</span>
          <small v-if="item.isDisabled">即将开放</small>
        </router-link>
      </nav>

      <div class="app-shell__sidebar-footer">
        <span :class="['app-shell__status-dot', statusClass]" />
        <span>{{ statusText }}</span>
      </div>
    </aside>

    <main class="app-shell__content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';

import { useApplicationStore } from '@/stores';

interface NavigationItem {
  readonly label: string;
  readonly path: string;
  readonly isDisabled: boolean;
}

const navigationItems: readonly NavigationItem[] = [
  { label: '日志', path: '/', isDisabled: false },
  { label: '标签', path: '/', isDisabled: true },
  { label: '成长', path: '/', isDisabled: true },
  { label: '设置', path: '/settings', isDisabled: false },
];

const applicationStore = useApplicationStore();
const statusText = computed<string>(() => {
  const statusLabels = {
    checking: '正在连接本地服务',
    offline: '本地服务未连接',
    online: '本地服务已连接',
  } as const;
  return statusLabels[applicationStore.backendStatus];
});
const statusClass = computed<string>(() => `is-${applicationStore.backendStatus}`);

const handleNavigation = (item: NavigationItem, event: MouseEvent): void => {
  if (item.isDisabled) {
    event.preventDefault();
  }
};

onMounted(async (): Promise<void> => {
  await applicationStore.checkBackend();
});
</script>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  min-height: 100vh;
  background: #f4f6f3;
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  padding: 28px 18px 20px;
  border-right: 1px solid #e1e6df;
  background: #fbfcfa;
  flex-direction: column;
}

.app-shell__brand {
  display: flex;
  margin: 0 10px 34px;
  color: #24352a;
  font-size: 22px;
  font-weight: 700;
  text-decoration: none;
  align-items: center;
  gap: 12px;
}

.app-shell__brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: #2f6548;
  color: #fff;
  font-family: Georgia, serif;
  place-items: center;
}

.app-shell__navigation {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.app-shell__navigation-item {
  display: flex;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  color: #516057;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
  transition: background 160ms ease, color 160ms ease;
}

.app-shell__navigation-item.router-link-active:not(.is-disabled) {
  background: #e4eee7;
  color: #24563c;
  font-weight: 650;
}

.app-shell__navigation-item.is-disabled {
  color: #a1aaa4;
  cursor: not-allowed;
}

.app-shell__navigation-item small {
  font-size: 10px;
}

.app-shell__sidebar-footer {
  display: flex;
  margin-top: auto;
  padding: 12px 10px;
  color: #758078;
  font-size: 12px;
  align-items: center;
  gap: 8px;
}

.app-shell__status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d49b42;
}

.app-shell__status-dot.is-online {
  background: #3c9c65;
}

.app-shell__status-dot.is-offline {
  background: #cf5c54;
}

.app-shell__content {
  min-width: 0;
  padding: 36px 44px;
}

@media (max-width: 760px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    position: static;
    height: auto;
  }
}
</style>
