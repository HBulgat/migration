<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getDiffTypeLabel, getDiffTypeTagType } from '@/constants'
import { getDiffRecordDetail } from '@/api/diffRecord'
import DiffViewer from '@/components/DiffViewer.vue'
import { formatCost, formatDateTime } from '@/utils/format'
import type { DiffRecord } from '@/types'

const props = defineProps<{
  visible: boolean
  recordId: number | null
}>()

const emit = defineEmits<{
  (event: 'update:visible', visible: boolean): void
}>()

const detail = ref<DiffRecord | null>(null)
const loading = ref(false)
const activePath = ref('')

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const diffCount = computed(() => detail.value?.diff_results?.length ?? 0)

async function loadDetail(id: number): Promise<void> {
  loading.value = true
  activePath.value = ''
  try {
    detail.value = await getDiffRecordDetail(id)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.recordId] as const,
  async ([visible, recordId]) => {
    if (!visible || recordId === null) {
      return
    }
    await loadDetail(recordId)
  },
  { immediate: true },
)
</script>

<template>
  <el-drawer v-model="drawerVisible" title="Diff详情" size="78%" destroy-on-close>
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="detail">
      <el-card shadow="never" class="detail-card">
        <template #header>
          <span>基础信息</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="migration_key">
            <span class="mono-text">{{ detail.migration_key }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="trace_id">
            <span class="mono-text">{{ detail.trace_id || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="有无差异">
            <el-tag :type="detail.has_diff ? 'danger' : 'success'" effect="light">
              {{ detail.has_diff ? '有差异' : '无差异' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="差异数">{{ diffCount }}</el-descriptions-item>
          <el-descriptions-item label="旧接口耗时">{{ formatCost(detail.old_cost_time_ms) }}</el-descriptions-item>
          <el-descriptions-item label="新接口耗时">{{ formatCost(detail.new_cost_time_ms) }}</el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ formatCost(detail.total_cost_time_ms) }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(detail.create_time) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="detail-card">
        <template #header>
          <span>差异详情</span>
        </template>

        <el-table :data="detail.diff_results" border max-height="260">
          <el-table-column prop="field_path" label="字段路径" min-width="200" />
          <el-table-column label="差异类型" width="120">
            <template #default="scope">
              <el-tag :type="getDiffTypeTagType(scope.row.diff_type)" effect="light">
                {{ getDiffTypeLabel(scope.row.diff_type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="old_value" label="旧值" min-width="160" show-overflow-tooltip />
          <el-table-column prop="new_value" label="新值" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="activePath = scope.row.field_path">定位</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="detail-card">
        <template #header>
          <span>响应对比</span>
        </template>
        <DiffViewer
          :old-response="detail.old_response"
          :new-response="detail.new_response"
          :diff-results="detail.diff_results"
          :active-path="activePath"
        />
      </el-card>
    </template>
  </el-drawer>
</template>

<style scoped>
.detail-card {
  margin-bottom: 12px;
}
</style>
