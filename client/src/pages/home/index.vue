<template>
  <section class="home-page">
    <header class="home-page__header">
      <div>
        <p class="home-page__eyebrow">PERSONAL KNOWLEDGE JOURNAL</p>
        <h1>把每天学到的东西，慢慢连成自己的知识地图。</h1>
        <p class="home-page__description">
          先从一篇日志开始。后续里程碑会加入 Markdown、图片、标签检索与成长统计。
        </p>
      </div>
      <el-button type="primary" disabled>写一篇日志</el-button>
    </header>

    <div class="home-page__grid">
      <el-card shadow="never" class="home-page__card">
        <template #header>今天</template>
        <p>记录尚未开始</p>
        <span>编辑器将在下一步接入。</span>
      </el-card>
      <el-card shadow="never" class="home-page__card">
        <template #header>当前里程碑</template>
        <p>本地服务与桌面壳</p>
        <span>优先建立稳定的数据和运行基础。</span>
      </el-card>
      <el-card shadow="never" class="home-page__card">
        <template #header>连接状态</template>
        <p>{{ connectionLabel }}</p>
        <span>可在设置中查看 MySQL 与 Redis 的独立状态。</span>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import { useApplicationStore } from '@/stores';

const applicationStore = useApplicationStore();
const connectionLabel = computed<string>(() => {
  const labels = {
    checking: '检查中',
    offline: '后端未连接',
    online: '后端已连接',
  } as const;
  return labels[applicationStore.backendStatus];
});
</script>

<style scoped>
.home-page {
  max-width: 1120px;
  margin: 0 auto;
}

.home-page__header {
  display: flex;
  min-height: 230px;
  padding: 42px;
  border: 1px solid #dce5de;
  border-radius: 24px;
  background: linear-gradient(130deg, #f9fbf7 0%, #e9f1e8 100%);
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
}

.home-page__eyebrow {
  margin: 0 0 18px;
  color: #4d7f60;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.15em;
}

.home-page__header h1 {
  max-width: 720px;
  margin: 0;
  color: #203127;
  font-family: Georgia, 'Songti SC', serif;
  font-size: clamp(30px, 4vw, 48px);
  font-weight: 500;
  line-height: 1.25;
}

.home-page__description {
  max-width: 660px;
  margin: 22px 0 0;
  color: #657168;
  line-height: 1.8;
}

.home-page__grid {
  display: grid;
  margin-top: 24px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.home-page__card {
  border-color: #e1e6df;
  border-radius: 16px;
}

.home-page__card p {
  margin: 0 0 8px;
  color: #28372e;
  font-size: 18px;
  font-weight: 650;
}

.home-page__card span {
  color: #7a857d;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .home-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
