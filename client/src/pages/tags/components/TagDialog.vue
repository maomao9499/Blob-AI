<template>
  <div class="modal-backdrop"><section class="panel modal" role="dialog" aria-modal="true" aria-labelledby="tag-dialog-title">
    <h2 id="tag-dialog-title">{{ tag ? '编辑标签' : '新建标签' }}</h2>
    <form @submit.prevent="handleSave"><label>标签名称<input v-model="name" name="tagName" maxlength="50" required autofocus /></label><label>标签颜色<input v-model="color" type="color" name="tagColor" /></label><p v-if="error" class="error" role="alert">{{ error }}</p><div class="actions"><button class="primary" :disabled="isSaving">保存标签</button><button type="button" :disabled="isSaving" @click="emit('close')">取消</button></div></form>
  </section></div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { tagApi } from '@/api/tag';
import type { Tag } from '@/types/journal.types';
const props = defineProps<{ tag: Tag | null }>();
const emit = defineEmits<{ close: []; saved: [] }>();
const name = ref(props.tag?.name ?? '');
const color = ref(props.tag?.color ?? '#356c4d');
const error = ref('');
const isSaving = ref(false);
const handleSave = async (): Promise<void> => {
  if (isSaving.value) { return; }
  if (!name.value.trim()) { error.value = '请输入标签名称'; return; }
  isSaving.value = true; error.value = '';
  try {
    const input = { name: name.value.trim(), color: color.value };
    if (props.tag) { await tagApi.update(props.tag.id, input); } else { await tagApi.create(input); }
    emit('saved');
  } catch (reason: unknown) { error.value = reason instanceof Error ? reason.message : '保存失败'; } finally { isSaving.value = false; }
};
</script>
<style scoped>
form { display: grid; gap: 20px; }
</style>
