<script setup lang="ts">
import { computed } from 'vue'
import { getMigrationTaskStatusMeta } from '@/constants'

const props = defineProps<{
  status: number
}>()

const meta = computed(() => getMigrationTaskStatusMeta(props.status))
</script>

<template>
  <el-tag class="status-tag" :class="meta.tagClass" effect="light">{{ meta.label }}</el-tag>
</template>

<style scoped>
.status-tag {
  border: 1px solid transparent;
  font-weight: 500;
}

.status-old {
  color: #606266;
  background: #f4f4f5;
  border-color: #dcdfe6;
}

.status-validation-gray {
  color: #409eff;
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.status-validation-all {
  color: #00a6c7;
  background: #e6f8fc;
  border-color: #9ee4f2;
}

.status-go-live-gray {
  color: #e6a23c;
  background: #fdf6ec;
  border-color: #f3d19e;
}

.status-go-live-all {
  color: #67c23a;
  background: #f0f9eb;
  border-color: #c2e7b0;
}

.status-decommissioning-gray {
  color: #8e44ad;
  background: #f5eef8;
  border-color: #d7bde2;
}

.status-decommissioning-all {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fbc4c4;
}
</style>
