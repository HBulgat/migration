<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteGrayRule, getGrayRuleList, updateGrayRuleEnable } from '@/api/grayRule'
import { getGrayRuleTypeLabel } from '@/constants'
import { useMigrationTaskStore } from '@/store'
import { formatDateTime } from '@/utils/format'
import EditDialog from '@/views/GrayRule/EditDialog.vue'
import type { GrayRule } from '@/types'

const taskStore = useMigrationTaskStore()

const loading = ref(false)
const tableData = ref<GrayRule[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const filterForm = reactive({
  migration_key: '',
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingRule = ref<GrayRule | null>(null)
const switchLoading = ref<Record<string, boolean>>({})

async function loadTableData(): Promise<void> {
  if (!filterForm.migration_key) {
    tableData.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const pageResult = await getGrayRuleList({
      migration_key: filterForm.migration_key,
      page: current.value,
      pageSize: pageSize.value,
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

function openCreateDialog(): void {
  dialogMode.value = 'create'
  editingRule.value = null
  dialogVisible.value = true
}

function openEditDialog(rule: GrayRule): void {
  dialogMode.value = 'edit'
  editingRule.value = rule
  dialogVisible.value = true
}

async function handleDelete(rule: GrayRule): Promise<void> {
  await ElMessageBox.confirm(`确认删除规则 ${rule.rule_id} 吗？`, '删除确认', {
    type: 'warning',
  })
  await deleteGrayRule({
    migration_key: rule.migration_key,
    rule_id: rule.rule_id,
  })
  ElMessage.success('删除成功')
  await loadTableData()
}

async function handleEnableChange(rule: GrayRule, enable: boolean): Promise<void> {
  switchLoading.value = {
    ...switchLoading.value,
    [rule.rule_id]: true,
  }

  try {
    await updateGrayRuleEnable({
      migration_key: rule.migration_key,
      rule_id: rule.rule_id,
      enable,
    })
    rule.enable = enable
    ElMessage.success('状态更新成功')
  } catch {
    rule.enable = !enable
  } finally {
    switchLoading.value = {
      ...switchLoading.value,
      [rule.rule_id]: false,
    }
  }
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
  const firstOption = taskOptions[0]
  if (firstOption) {
    filterForm.migration_key = firstOption.value
    await loadTableData()
  }
})
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <h2 class="page-title">灰度规则</h2>
      <el-button type="primary" :disabled="!filterForm.migration_key" @click="openCreateDialog">
        + 新建规则
      </el-button>
    </div>

    <el-form :inline="true" class="search-form">
      <el-form-item label="迁移任务">
        <el-select
          v-model="filterForm.migration_key"
          placeholder="请选择迁移任务"
          filterable
          style="width: 320px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in taskStore.taskOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="!filterForm.migration_key" description="请先选择迁移任务" />

    <template v-else>
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="rule_id" label="规则ID" min-width="160" />
        <el-table-column label="规则类型" width="140">
          <template #default="scope">
            {{ getGrayRuleTypeLabel(scope.row.rule_type) }}
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="120">
          <template #default="scope">
            <el-switch
              v-model="scope.row.enable"
              :loading="switchLoading[scope.row.rule_id]"
              @change="handleEnableChange(scope.row, $event as boolean)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="scope">
            <el-space>
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
    </template>

    <EditDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :rule="editingRule"
      :task-options="taskStore.taskOptions"
      :default-migration-key="filterForm.migration_key"
      @success="loadTableData"
    />
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
