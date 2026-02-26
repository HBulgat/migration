<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMigrationTask, getMigrationTaskList, queryMigrationTask } from '@/api/migrationTask'
import { MIGRATION_STATUS_OPTIONS } from '@/constants'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import EditDialog from '@/views/MigrationTask/EditDialog.vue'
import type { MigrationTask } from '@/types'

const loading = ref(false)
const tableData = ref<MigrationTask[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingTask = ref<MigrationTask | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTask = ref<MigrationTask | null>(null)

const tableIndex = computed(() => {
  return (index: number) => (current.value - 1) * pageSize.value + index + 1
})

async function loadTableData(): Promise<void> {
  loading.value = true
  try {
    const pageResult = await getMigrationTaskList({
      page: current.value,
      pageSize: pageSize.value,
      status: searchForm.status,
      keyword: searchForm.keyword.trim() || undefined,
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
  searchForm.keyword = ''
  searchForm.status = undefined
  current.value = 1
  await loadTableData()
}

function openCreateDialog(): void {
  dialogMode.value = 'create'
  editingTask.value = null
  dialogVisible.value = true
}

function openEditDialog(task: MigrationTask): void {
  dialogMode.value = 'edit'
  editingTask.value = task
  dialogVisible.value = true
}

async function openDetail(task: MigrationTask): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailTask.value = await queryMigrationTask({ migration_key: task.migration_key })
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(task: MigrationTask): Promise<void> {
  await ElMessageBox.confirm(`确认删除任务 ${task.migration_key} 吗？`, '删除确认', {
    type: 'warning',
  })
  await deleteMigrationTask({ migration_key: task.migration_key })
  ElMessage.success('删除成功')
  await loadTableData()
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
  await loadTableData()
})
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <h2 class="page-title">迁移任务</h2>
      <el-button type="primary" @click="openCreateDialog">+ 新建任务</el-button>
    </div>

    <el-form :inline="true" class="search-form">
      <el-form-item label="搜索">
        <el-input
          v-model="searchForm.keyword"
          placeholder="请输入 migration_key"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 160px">
          <el-option
            v-for="item in MIGRATION_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" stripe border v-loading="loading">
      <el-table-column label="#" width="60" align="center">
        <template #default="scope">{{ tableIndex(scope.$index) }}</template>
      </el-table-column>
      <el-table-column prop="migration_key" label="migration_key" min-width="220">
        <template #default="scope">
          <span class="mono-text">{{ scope.row.migration_key }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="scope">
          <StatusTag :status="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="create_time" label="创建时间" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.create_time) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button link type="warning" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </el-space>
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

    <EditDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :task="editingTask"
      @success="loadTableData"
    />

    <el-drawer v-model="detailVisible" title="迁移任务详情" size="540px" destroy-on-close>
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <el-descriptions v-else-if="detailTask" :column="1" border>
        <el-descriptions-item label="migration_key">
          <span class="mono-text">{{ detailTask.migration_key }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="detailTask.status" />
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailTask.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detailTask.create_time) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detailTask.update_time) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.search-form {
  margin-bottom: 8px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
