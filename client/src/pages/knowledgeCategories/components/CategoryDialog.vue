<template>
  <div class="modal-backdrop"><section class="panel modal" role="dialog" aria-modal="true" aria-labelledby="category-title">
    <h2 id="category-title">{{ category ? '编辑分类' : '新建分类' }}</h2>
    <form class="category-form" @submit.prevent="handleSave">
      <fieldset :disabled="isSaving"><label>分类名称<input v-model="form.name" name="name" maxlength="50" required /></label><label>分类描述<textarea v-model="form.description" name="description" maxlength="500" rows="3" /></label></fieldset>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <div class="actions"><button class="primary" :disabled="isSaving">{{ isSaving ? '正在保存…' : '保存分类' }}</button><button type="button" :disabled="isSaving" @click="handleClose">取消</button></div>
    </form>
  </section></div>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router';
import { categoryApi } from '@/api/knowledge';
import type { CategoryInput, KnowledgeCategory } from '@/types/knowledge.types';
const props = defineProps<{ category: KnowledgeCategory | null }>();
const emit = defineEmits<{ saved: []; close: [] }>();
const form = reactive<CategoryInput>({ name: props.category?.name ?? '', description: props.category?.description ?? '' });
const baseline = JSON.stringify(form);
const isSaving = ref(false); const error = ref('');
const isDirty = computed(() => baseline !== JSON.stringify(form));
const handleSave = async (): Promise<void> => {
  if (isSaving.value) { return; }
  if (!form.name.trim()) { error.value = '请填写分类名称'; return; }
  isSaving.value = true; error.value = '';
  try {
    const input: CategoryInput = { name: form.name.trim(), description: form.description?.trim() || null };
    if (props.category) { await categoryApi.update(props.category.id, input); } else { await categoryApi.create(input); }
    emit('saved');
  } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '保存失败'; }
  finally { isSaving.value = false; }
};
const confirmLeave = (): boolean => !isSaving.value && (!isDirty.value || window.confirm('分类尚未保存，确定放弃修改？'));
const handleClose = (): void => { if (confirmLeave()) { emit('close'); } };
const beforeUnload = (event: BeforeUnloadEvent): void => {
  if (isDirty.value || isSaving.value) { event.preventDefault(); event.returnValue = ''; }
};
onBeforeRouteLeave(confirmLeave);
onBeforeRouteUpdate(confirmLeave);
window.addEventListener('beforeunload', beforeUnload);
onBeforeUnmount(() => { window.removeEventListener('beforeunload', beforeUnload); });
</script>
<style scoped>
textarea { width: 100%; padding: 10px 12px; border: 1px solid #d7dfd6; border-radius: 8px; color: #243b2c; resize: vertical; }
.category-form, fieldset { display: grid; gap: 18px; }
fieldset { padding: 0; border: 0; }
</style>
