<template>
  <div class="markdown-editor">
    <div class="editor-heading"><span class="muted">Markdown · 支持粘贴图片，或选择上传</span><label class="button">{{ isUploading ? '上传中…' : '上传图片' }}<input class="upload-input" type="file" accept="image/png,image/jpeg,image/gif,image/webp" multiple :disabled="isUploading" aria-label="上传图片" @change="handleFiles" /></label></div>
    <p v-if="uploadError" class="error" role="alert">{{ uploadError }}</p>
    <md-editor :model-value="modelValue" :sanitize="sanitizeMarkdown" :toolbars="toolbars" no-mermaid no-katex no-highlight :preview="true" @update:model-value="emit('update:modelValue', $event)" @on-upload-img="handleUpload" />
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { MdEditor, type ToolbarNames } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import { mediaApi } from '@/api/media';
import { sanitizeMarkdown } from '@/utils/markdownSecurity';
const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: string]; uploading: [value: boolean] }>();
const isUploading = ref(false); const uploadError = ref('');
// 首版不加载外部 CDN 的公式、图表和格式化扩展。
const toolbars: ToolbarNames[] = ['bold', 'italic', 'strikeThrough', '-', 'title', 'quote', 'unorderedList', 'orderedList', 'task', 'codeRow', 'code', 'link', 'table', '-', 'revoke', 'next', '=', 'preview', 'previewOnly'];
const isSupportedImage = async (file: File): Promise<boolean> => {
  const b = new Uint8Array(await file.slice(0, 12).arrayBuffer());
  const png = b.length >= 8 && [137,80,78,71,13,10,26,10].every((v,i) => b[i] === v);
  const jpg = b[0] === 255 && b[1] === 216 && b[2] === 255;
  const text = new TextDecoder('ascii').decode(b);
  return png || jpg || text.startsWith('GIF87a') || text.startsWith('GIF89a') || (text.startsWith('RIFF') && text.slice(8,12) === 'WEBP');
};
const handleUpload = async (files: File[], callback: (urls: string[]) => void): Promise<void> => {
  if (isUploading.value) { return; }
  isUploading.value = true; emit('uploading', true); uploadError.value = '';
  const urls: string[] = []; const errors: string[] = [];
  try {
    for (const file of files) {
      try {
        if (file.size > 10 * 1024 * 1024 || !await isSupportedImage(file)) { throw new Error('请选择不超过 10MB 的 PNG、JPEG、GIF 或 WebP 图片'); }
        const uploaded = await mediaApi.uploadImage(file);
        if (!/^\/api\/v1\/media\/images\/[a-zA-Z0-9._-]+$/.test(uploaded.url)) { throw new Error('图片地址无效'); }
        urls.push(uploaded.url);
      } catch (reason: unknown) { errors.push(`${file.name}：${reason instanceof Error ? reason.message : '上传失败'}`); }
    }
    if (urls.length) { callback(urls); }
    uploadError.value = errors.join('；');
  } finally { isUploading.value = false; emit('uploading', false); }
};
const handleFiles = async (event: Event): Promise<void> => {
  const input = event.target;
  if (!(input instanceof HTMLInputElement) || !input.files) { return; }
  await handleUpload(Array.from(input.files), (urls) => emit('update:modelValue', props.modelValue + '\n\n' + urls.map((url) => `![](${url})`).join('\n\n')));
  input.value = '';
};
</script>
<style scoped>
.editor-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 12px; }
.upload-input { width: 1px; height: 1px; position: absolute; opacity: 0; }
.markdown-editor :deep(.md-editor) { height: 540px; border-radius: 12px; }
</style>
