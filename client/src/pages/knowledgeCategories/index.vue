<template>
  <section class="page">
    <header class="page-heading"><div><p class="eyebrow">GIVE YOUR KNOWLEDGE A HOME</p><h1>分类管理</h1><router-link to="/knowledge">返回知识库</router-link></div><button class="primary" @click="openEditor(null)">新建分类</button></header>
    <p v-if="error" role="alert" class="error">{{ error }} <button @click="load">重试</button></p>
    <p v-if="isLoading" role="status">正在加载分类…</p>
    <div v-else class="panel"><p v-if="!categories.length && !error" class="muted">还没有分类，创建第一个吧。</p>
      <table v-if="categories.length"><thead><tr><th>分类名称</th><th>描述</th><th>操作</th></tr></thead><tbody><tr v-for="category in categories" :key="category.id"><td>{{ category.name }}</td><td>{{ category.description || '—' }}</td><td><div class="actions"><button @click="openEditor(category)">编辑</button><button class="danger-link" :disabled="isDeleting" @click="handleDelete(category.id)">删除</button><router-link :to="{ path: '/knowledge', query: { categoryId: category.id } }">查看知识</router-link></div></td></tr></tbody></table>
    </div>
    <category-dialog v-if="isEditorOpen" :category="selected" @close="isEditorOpen = false" @saved="handleSaved" />
  </section>
</template>
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import CategoryDialog from './components/CategoryDialog.vue';
import { categoryApi } from '@/api/knowledge';
import type { KnowledgeCategory } from '@/types/knowledge.types';
const categories = ref<KnowledgeCategory[]>([]); const selected = ref<KnowledgeCategory | null>(null);
const isEditorOpen = ref(false); const isLoading = ref(false); const isDeleting = ref(false); const error = ref('');
let sequence = 0;
const load = async (): Promise<void> => {
  const current = ++sequence; isLoading.value = true; error.value = '';
  try { const data = await categoryApi.list(); if (current === sequence) { categories.value = data; } }
  catch (reason: unknown) { if (current === sequence) { error.value = reason instanceof Error ? reason.message : '分类加载失败'; } }
  finally { if (current === sequence) { isLoading.value = false; } }
};
const openEditor = (category: KnowledgeCategory | null): void => { selected.value = category; isEditorOpen.value = true; };
const handleSaved = async (): Promise<void> => { isEditorOpen.value = false; await load(); };
const handleDelete = async (id: number): Promise<void> => {
  if (isDeleting.value || !window.confirm('确定删除这个分类？被知识使用的分类需要先迁移条目。')) { return; }
  isDeleting.value = true; error.value = '';
  try { await categoryApi.remove(id); await load(); }
  catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '删除失败'; }
  finally { isDeleting.value = false; }
};
onMounted(load); onBeforeUnmount(() => { ++sequence; });
</script>
<style scoped>
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 18px 12px; border-bottom: 1px solid #e8ece6; overflow-wrap: anywhere; }
</style>
