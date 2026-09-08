<template>
  <section class="page"><header class="page-heading"><div><p class="eyebrow">FIND THE CONNECTIONS</p><h1>标签管理</h1><p class="muted">给每一段记录一个容易找回的线索。</p></div><button class="primary" @click="openEditor(null)">新建标签</button></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="isLoading" role="status">正在加载标签…</p>
    <div v-else class="panel"><p v-if="!tags.length" class="muted">还没有标签，创建第一个吧。</p><table v-else><thead><tr><th>标签</th><th>操作</th></tr></thead><tbody><tr v-for="tag in tags" :key="tag.id"><td><span class="tag" :style="{ borderColor: safeColor(tag.color) }">{{ tag.name }}</span></td><td><div class="actions"><button @click="openEditor(tag)">编辑</button><button class="danger-link" :disabled="isDeleting" @click="handleDelete(tag.id)">删除</button><router-link :to="{ path: '/', query: { tagId: tag.id } }">查看日志</router-link></div></td></tr></tbody></table></div>
    <tag-dialog v-if="isEditorOpen" :tag="selected" @close="isEditorOpen = false" @saved="handleSaved" />
    <div v-if="deleteId !== null" class="modal-backdrop"><section class="panel modal" role="dialog" aria-modal="true" aria-labelledby="delete-tag-title"><h2 id="delete-tag-title">删除标签</h2><p>确定删除这个标签？被日志使用的标签需要先解除关联。</p><p v-if="deleteError" class="error" role="alert">{{ deleteError }}</p><div class="actions"><button :disabled="isDeleting" class="danger-link" @click="confirmDelete">确认删除</button><button :disabled="isDeleting" @click="deleteId = null">取消</button></div></section></div>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import TagDialog from './components/TagDialog.vue';
import { tagApi } from '@/api/tag';
import type { Tag } from '@/types/journal.types';
const tags = ref<Tag[]>([]); const selected = ref<Tag | null>(null);
const isEditorOpen = ref(false); const isLoading = ref(false); const isDeleting = ref(false);
const error = ref(''); const deleteError = ref(''); const deleteId = ref<number | null>(null);
const safeColor = (color: string | null): string => color && /^#[0-9a-f]{6}$/i.test(color) ? color : '#356c4d';
const load = async (): Promise<void> => {
  isLoading.value = true; error.value = '';
  try { tags.value = await tagApi.list(); } catch (reason: unknown) { tags.value = []; error.value = reason instanceof Error ? reason.message : '标签加载失败'; } finally { isLoading.value = false; }
};
const openEditor = (tag: Tag | null): void => { selected.value = tag; isEditorOpen.value = true; };
const handleSaved = async (): Promise<void> => { isEditorOpen.value = false; await load(); };
const handleDelete = (id: number): void => { deleteId.value = id; deleteError.value = ''; };
const confirmDelete = async (): Promise<void> => {
  if (isDeleting.value || deleteId.value === null) { return; }
  isDeleting.value = true;
  try { await tagApi.remove(deleteId.value); deleteId.value = null; await load(); } catch (reason: unknown) { deleteError.value = reason instanceof Error ? reason.message : '删除失败'; } finally { isDeleting.value = false; }
};
onMounted(load);
</script>
<style scoped>
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 18px 12px; border-bottom: 1px solid #e8ece6; overflow-wrap: anywhere; }
</style>
