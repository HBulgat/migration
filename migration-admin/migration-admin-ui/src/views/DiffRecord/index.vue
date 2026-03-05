<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { HAS_DIFF_OPTIONS } from '@/constants'
import { getDiffRecordList } from '@/api/diffRecord'
import { useMigrationTaskStore } from '@/store'
import { formatCost, formatDateTime } from '@/utils/format'
import DetailDrawer from '@/views/DiffRecord/DetailDrawer.vue'
import type { DiffRecord } from '@/types'

const route = useRoute()
const taskStore = useMigrationTaskStore()

const loading = ref(false)
const tableData = ref<DiffRecord[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const filterForm = reactive({
  migration_key: '',
  has_diff: undefined as number | undefined,
  date_range: [] as string[],
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
  filterForm.has_diff = undefined
  filterForm.date_range = []
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
    </div>

    <el-form :inline="true" class="search-form">
      <el-form-item label="迁移任务">
        <el-select
          v-model="filterForm.migration_key"
          placeholder="请选择迁移任务"
          filterable
          style="width: 320px"
        >
          <el-option
            v-for="item in taskStore.taskOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="有无差异">
        <el-select v-model="filterForm.has_diff" placeholder="全部" clearable filterable style="width: 140px">
          <el-option
            v-for="item in HAS_DIFF_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="时间范围">
        <el-date-picker
          v-model="filterForm.date_range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="!filterForm.migration_key" description="请先选择迁移任务" />

    <template v-else>
      <el-table :data="tableData" stripe border v-loading="loading" @row-click="openDetail">
        <el-table-column prop="migration_key" label="migration_key" min-width="180">
          <template #default="scope">
            <span class="mono-text">{{ scope.row.migration_key }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有无差异" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.has_diff ? 'danger' : 'success'" effect="light">
              {{ scope.row.has_diff ? '有差异' : '无差异' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="old_cost_time_ms" label="旧接口耗时" width="120">
          <template #default="scope">{{ formatCost(scope.row.old_cost_time_ms) }}</template>
        </el-table-column>
        <el-table-column prop="new_cost_time_ms" label="新接口耗时" width="120">
          <template #default="scope">{{ formatCost(scope.row.new_cost_time_ms) }}</template>
        </el-table-column>
        <el-table-column label="差异数" width="90">
          <template #default="scope">{{ scope.row.diff_results?.length ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="create_time" label="时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click.stop="openDetail(scope.row)">查看</el-button>
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
.search-form {
  margin-bottom: 10px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
