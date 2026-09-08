<template>
  <footer class="pagination"><span class="muted">共 {{ total }} 条 · 第 {{ page }} 页</span><div class="actions"><button :disabled="page <= 1 || isLoading" @click="$emit('change', page - 1)">上一页</button><button :disabled="page * pageSize >= total || isLoading" @click="$emit('change', page + 1)">下一页</button></div></footer>
</template>
<script setup lang="ts">
defineProps<{ page: number; pageSize: number; total: number; isLoading: boolean }>();
defineEmits<{ change: [page: number] }>();
</script>
<style scoped>
.pagination { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 24px; flex-wrap: wrap; }
</style>
