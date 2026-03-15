<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { HAS_DIFF_OPTIONS, MIGRATION_STATUS_OPTIONS } from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { getDiffRecordList } from '@/api/diffRecord'
import { useMigrationTaskStore } from '@/store'
import { formatDateTime } from '@/utils/format'
import DetailDrawer from '@/views/DiffRecord/DetailDrawer.vue'
import type { DiffRecord } from '@/types'

const route = useRoute()
const taskStore = useMigrationTaskStore()

const loading = ref(false)
const tableData = ref<DiffRecord[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const UI_TEXT = {
  taskPlaceholder: '请选择迁移任务',
  statusPlaceholder: '迁移状态（为空表示全部）',
  diffPlaceholder: '是否有差异',
  startDatePlaceholder: '开始日期',
  endDatePlaceholder: '结束日期',
  rangeSeparator: '至',
  filterButton: '筛选',
  resetButton: '重置',
  traceIdPlaceholder: 'Trace ID',
} as const

const GRANULARITY_OPTIONS = [
  { label: '分钟', value: 'MINUTE' },
  { label: '小时', value: 'HOUR' },
  { label: '天', value: 'DAY' },
]

const filterForm = reactive({
  migration_key: '',
  migration_status: undefined as number | undefined,
  has_diff: undefined as number | undefined,
  trace_id: '',
  date_range: [] as string[],
  granularity: 'HOUR' as 'MINUTE' | 'HOUR' | 'DAY',
})

const detailVisible = ref(false)
const selectedRecordId = ref<number | null>(null)

function buildDateParams(): { start_date?: string; end_date?: string } {
  if (filterForm.date_range.length === 2) {
    return {
      start_date: filterForm.date_range[0],
      end_date: filterForm.date_range[1],
    }
  }
  return {}
}

async function loadTableData(): Promise<void> {
  if (!filterForm.migration_key) {
    tableData.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const pageResult = await getDiffRecordList({
      migration_key: filterForm.migration_key,
      has_diff: filterForm.has_diff,
      migration_status: filterForm.migration_status,
      trace_id: filterForm.trace_id || undefined,
      page: current.value,
      pageSize: pageSize.value,
      ...buildDateParams(),
    })
    tableData.value = pageResult.list
    total.value = pageResult.total
  } finally {
    loading.value = false
  }
}

async function handleSearch(): Promise<void> {
  current.value = 1
  await loadTableData()
}

async function handleReset(): Promise<void> {
  filterForm.migration_status = undefined
  filterForm.has_diff = undefined
  filterForm.trace_id = ''
  filterForm.date_range = []
  filterForm.granularity = 'HOUR'
  current.value = 1
  await loadTableData()
}

function openDetail(record: DiffRecord): void {
  selectedRecordId.value = record.id
  detailVisible.value = true
}

async function handlePageChange(page: number): Promise<void> {
  current.value = page
  await loadTableData()
}

async function handlePageSizeChange(size: number): Promise<void> {
  pageSize.value = size
  current.value = 1
  await loadTableData()
}

onMounted(async () => {
  const taskOptions = await taskStore.fetchTaskOptions()
  const routeMigrationKey = route.query.migration_key
  if (typeof routeMigrationKey === 'string') {
    filterForm.migration_key = routeMigrationKey
  } else {
    const firstOption = taskOptions[0]
    if (firstOption) {
      filterForm.migration_key = firstOption.value
    }
  }
  await loadTableData()
})
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <h2 class="page-title">Diff记录</h2>
      
      <el-select
        v-model="filterForm.migration_key"
        :placeholder="UI_TEXT.taskPlaceholder"
        filterable
        style="width: 220px"
        @change="handleSearch"
      >
        <el-option
          v-for="item in taskStore.taskOptions"
          :key="item.value"
          :label="item.value"
          :value="item.value"
        />
      </el-select>

      <el-select
        v-model="filterForm.migration_status"
        clearable
        filterable
        :placeholder="UI_TEXT.statusPlaceholder"
        style="width: 160px"
        @change="handleSearch"
      >
        <el-option
          v-for="status in MIGRATION_STATUS_OPTIONS"
          :key="status.value"
          :label="status.label"
          :value="status.value"
        />
      </el-select>

      <el-input
        v-model="filterForm.trace_id"
        clearable
        :placeholder="UI_TEXT.traceIdPlaceholder"
        style="width: 180px"
        @keyup.enter="handleSearch"
      />

      <el-select
        v-model="filterForm.has_diff"
        clearable
        filterable
        :placeholder="UI_TEXT.diffPlaceholder"
        style="width: 130px"
        @change="handleSearch"
      >
        <el-option
          v-for="item in HAS_DIFF_OPTIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-date-picker
        v-model="filterForm.date_range"
        :type="filterForm.granularity === 'DAY' ? 'daterange' : 'datetimerange'"
        value-format="YYYY-MM-DD HH:mm:ss"
        :start-placeholder="UI_TEXT.startDatePlaceholder"
        :end-placeholder="UI_TEXT.endDatePlaceholder"
        :range-separator="UI_TEXT.rangeSeparator"
        style="width: 300px"
      />

      <el-radio-group v-model="filterForm.granularity" style="margin-left: 12px">
        <el-radio-button v-for="opt in GRANULARITY_OPTIONS" :key="opt.value" :label="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>

      <el-button type="primary" :icon="Refresh" @click="handleSearch" style="margin-left: 12px">
        {{ UI_TEXT.filterButton }}
      </el-button>
      <el-button @click="handleReset">{{ UI_TEXT.resetButton }}</el-button>
    </div>

    <el-empty v-if="!filterForm.migration_key" description="请先选择迁移任务" />

    <template v-else>
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="migration_key" label="migration_key" min-width="300">
          <template #default="scope">
            <span class="mono-text">{{ scope.row.migration_key }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="trace_id" label="Trace ID" width="180">
          <template #default="scope">
            <span class="mono-text">{{ scope.row.trace_id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="迁移状态" width="120">
          <template #default="scope">
            <StatusTag :status="scope.row.migration_status" />
          </template>
        </el-table-column>
        <el-table-column label="有无差异" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.has_diff ? 'danger' : 'success'" effect="light">
              {{ scope.row.has_diff ? '有差异' : '无差异' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异数" width="80">
          <template #default="scope">{{ scope.row.diff_results?.length ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="create_time" label="时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="current"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </template>

    <DetailDrawer v-model:visible="detailVisible" :record-id="selectedRecordId" />
  </el-card>
</template>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
