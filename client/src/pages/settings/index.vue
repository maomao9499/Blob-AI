<template>
  <section class="settings-page">
    <header class="settings-page__header">
      <div>
        <p class="settings-page__eyebrow">LOCAL ENVIRONMENT</p>
        <h1>设置与依赖</h1>
        <p>分别检查本地 MySQL 与 Redis，不让缓存故障影响日志数据。</p>
      </div>
      <el-button :loading="isLoading" @click="handleRefresh">重新检查</el-button>
    </header>

    <desktop-setup @connected="handleRefresh" />
    <el-card v-if="errorMessage" shadow="never" class="settings-page__error">
      {{ errorMessage }}
    </el-card>

    <div class="settings-page__dependencies">
      <el-card
        v-for="dependency in dependencyCards"
        :key="dependency.key"
        shadow="never"
        class="settings-page__dependency-card"
      >
        <div class="settings-page__dependency-heading">
          <h2>{{ dependency.label }}</h2>
          <el-tag :type="dependency.tagType">{{ dependency.statusLabel }}</el-tag>
        </div>
        <p>{{ dependency.message }}</p>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TagProps } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import DesktopSetup from './components/DesktopSetup.vue';

import { systemApi } from '@/api/system';
import type { DependencyStatus, DependencyStatuses } from '@/types/api.types';

interface DependencyCard {
  readonly key: keyof DependencyStatuses;
  readonly label: string;
  readonly message: string;
  readonly statusLabel: string;
  readonly tagType: TagProps['type'];
}

const EMPTY_STATUS: DependencyStatus = {
  message: '尚未检查',
  status: 'NOT_CONFIGURED',
};
const DEPENDENCY_LABELS: Readonly<Record<keyof DependencyStatuses, string>> = {
  mysql: 'MySQL',
  redis: 'Redis',
};

const dependencyStatuses = ref<DependencyStatuses>({
  mysql: EMPTY_STATUS,
  redis: EMPTY_STATUS,
});
const errorMessage = ref('');
const isLoading = ref(false);

const getStatusPresentation = (
  status: DependencyStatus['status'],
): Pick<DependencyCard, 'statusLabel' | 'tagType'> => {
  const presentationMap = {
    DOWN: { statusLabel: '不可用', tagType: 'danger' },
    NOT_CONFIGURED: { statusLabel: '未配置', tagType: 'info' },
    UP: { statusLabel: '正常', tagType: 'success' },
  } as const;
  return presentationMap[status];
};

const dependencyCards = computed<readonly DependencyCard[]>(() =>
  (Object.keys(DEPENDENCY_LABELS) as Array<keyof DependencyStatuses>).map((key) => ({
    key,
    label: DEPENDENCY_LABELS[key],
    message: dependencyStatuses.value[key].message,
    ...getStatusPresentation(dependencyStatuses.value[key].status),
  })),
);

const loadDependencies = async (): Promise<void> => {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    dependencyStatuses.value = await systemApi.getDependencies();
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '依赖状态获取失败';
  } finally {
    isLoading.value = false;
  }
};

const handleRefresh = async (): Promise<void> => {
  await loadDependencies();
};

onMounted(async (): Promise<void> => {
  await loadDependencies();
});
</script>

<style scoped>
.settings-page {
  max-width: 920px;
  margin: 0 auto;
}

.settings-page__header {
  display: flex;
  margin-bottom: 28px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.settings-page__eyebrow {
  margin: 0 0 8px;
  color: #4d7f60;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.settings-page__header h1 {
  margin: 0;
  color: #23342a;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 34px;
  font-weight: 500;
}

.settings-page__header p:not(.settings-page__eyebrow) {
  margin: 10px 0 0;
  color: #778078;
}

.settings-page__error {
  margin-bottom: 18px;
  border-color: #efc6c2;
  color: #a53d36;
}

.settings-page__dependencies {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.settings-page__dependency-card {
  border-color: #e1e6df;
  border-radius: 16px;
}

.settings-page__dependency-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.settings-page__dependency-heading h2 {
  margin: 0;
  color: #27372d;
  font-size: 18px;
}

.settings-page__dependency-card p {
  min-height: 24px;
  margin: 18px 0 0;
  color: #6f7a72;
}

@media (max-width: 720px) {
  .settings-page__dependencies {
    grid-template-columns: 1fr;
  }
}
</style>
