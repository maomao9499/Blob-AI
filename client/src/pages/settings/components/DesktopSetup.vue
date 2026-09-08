<template>
  <form v-if="desktop" class="panel desktop-setup" @submit.prevent="handleSave">
    <h2>本地服务配置</h2><p class="muted">先通过 Homebrew 启动 MySQL 和 Redis。密码保存在 macOS 加密存储中；留空保留已有密码。</p>
    <label>MySQL 连接地址<input v-model="form.dbUrl" name="dbUrl" required /></label><label>MySQL 用户名<input v-model="form.dbUsername" name="dbUsername" required /></label>
    <label>MySQL 密码<input v-model="form.dbPassword" name="dbPassword" type="password" autocomplete="new-password" :placeholder="settings?.dbPasswordConfigured ? '已配置，留空保留' : '请输入本地数据库密码'" /></label>
    <div class="desktop-setup__row"><label>Redis 主机<input v-model="form.redisHost" name="redisHost" required /></label><label>Redis 端口<input v-model.number="form.redisPort" name="redisPort" type="number" min="1" max="65535" required /></label></div><label>Redis 密码（可选）<input v-model="form.redisPassword" name="redisPassword" type="password" autocomplete="new-password" :placeholder="settings?.redisPasswordConfigured ? '已配置，留空保留' : '无密码可留空'" /></label>
    <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="status" role="status">{{ status }}</p><button class="primary" :disabled="isSaving">{{ isSaving ? '正在启动服务…' : '保存并启动后端' }}</button>
  </form>
  <p v-else class="muted">浏览器开发模式：数据库连接使用后端环境变量；桌面安装版可在此配置。</p>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { configureBackend } from '@/api/http';
import { useApplicationStore } from '@/stores';
import type { DesktopSettingsInput, DesktopSettingsView } from '@/types/desktop.types';
const emit = defineEmits<{ connected: [] }>();
const desktop = window.blobDesktop; const appStore = useApplicationStore();
const settings = ref<DesktopSettingsView | null>(null);
const form = reactive<DesktopSettingsInput>({ dbUrl: 'jdbc:mysql://127.0.0.1:3306/blob_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai', dbUsername: 'root', dbPassword: '', redisHost: '127.0.0.1', redisPort: 6379, redisPassword: '' });
const error = ref(''); const status = ref(''); const isSaving = ref(false);
onMounted(async (): Promise<void> => {
  if (!desktop) { return; }
  try { const bootstrap = await desktop.getBootstrap(); settings.value = bootstrap.settings; if (bootstrap.settings) { const { dbUrl, dbUsername, redisHost, redisPort } = bootstrap.settings; Object.assign(form, { dbUrl, dbUsername, redisHost, redisPort }); } error.value = bootstrap.error ?? ''; } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '配置读取失败'; }
});
const handleSave = async (): Promise<void> => {
  if (!desktop || isSaving.value) { return; }
  isSaving.value = true; error.value = ''; status.value = '';
  try {
    settings.value = await desktop.saveSettings({ ...form });
    form.dbPassword = ''; form.redisPassword = '';
    const bootstrap = await desktop.startBackend();
    if (!bootstrap.apiBaseUrl || !bootstrap.sessionToken) { throw new Error(bootstrap.error ?? '后端启动失败，请检查连接配置'); }
    configureBackend(bootstrap.apiBaseUrl, bootstrap.sessionToken);
    await appStore.checkBackend();
    if (appStore.backendStatus !== 'online') { throw new Error('后端暂时无法连接，请重试启动。'); }
    status.value = '本地服务已就绪，可以开始记录。'; emit('connected');
  } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '保存或启动失败'; } finally { isSaving.value = false; }
};
</script>
<style scoped>
.desktop-setup { display: grid; gap: 16px; margin-bottom: 24px; }
.desktop-setup h2, .desktop-setup p { margin: 0; }
.desktop-setup__row { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
</style>
