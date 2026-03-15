<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDiffRuleList, deleteDiffRule, updateDiffRuleEnable, type DiffRule } from '@/api/diffRule'
import { getMigrationTaskList } from '@/api/migrationTask'
import { getMigrationTaskStatusMeta } from '@/constants'
import type { MigrationTask } from '@/types'
import EditDialog from './EditDialog.vue'

const tableData = ref<DiffRule[]>([])
const loading = ref(false)
const tasks = ref<MigrationTask[]>([])

const queryParams = reactive({
  migrationKey: '',
  page: 1,
  pageSize: 10,
})

const total = ref(0)
const editDialogRef = ref<InstanceType<typeof EditDialog> | null>(null)

function isRowDisabled(row: DiffRule): boolean {
  return !row.enable
}

function tableRowClassName({ row }: { row: DiffRule }) {
  return isRowDisabled(row) ? 'disabled-row' : ''
}

async function loadTasks() {
  try {
    const res = await getMigrationTaskList({ page: 1, pageSize: 100 })
    tasks.value = res.list
    if (tasks.value.length > 0 && !queryParams.migrationKey) {
      queryParams.migrationKey = tasks.value[0]!.migration_key
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载迁移任务列表异常')
  }
}

async function fetchData(resetPage = false) {
  if (resetPage) {
    queryParams.page = 1
  }
  if (!queryParams.migrationKey) {
    tableData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getDiffRuleList({
      migration_key: queryParams.migrationKey,
      page: queryParams.page,
      page_size: queryParams.pageSize,
    })
    tableData.value = res.list
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载规则异常')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  if (!queryParams.migrationKey) {
    ElMessage.warning('请先选择迁移任务')
    return
  }
  editDialogRef.value?.open(queryParams.migrationKey)
}

function handleEdit(row: DiffRule) {
  editDialogRef.value?.open(row.migration_key, row)
}

function handleDelete(row: DiffRule) {
  ElMessageBox.confirm('确定要删除该Diff规则吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await deleteDiffRule({
        migration_key: row.migration_key,
        rule_id: row.rule_id,
      })
      ElMessage.success('删除成功')
      fetchData()
    } catch (e: any) {
      ElMessage.error(e.message || '删除异常')
    }
  }).catch(() => {})
}

async function handleEnableChange(row: DiffRule) {
  try {
    await updateDiffRuleEnable({
      migration_key: row.migration_key,
      rule_id: row.rule_id,
      enable: row.enable,
    })
    ElMessage.success(`${row.enable ? '启用' : '禁用'}成功`)
  } catch (e: any) {
    row.enable = !row.enable // fallback
    ElMessage.error(e.message || '操作异常')
  }
}

function handleSizeChange(val: number) {
  queryParams.pageSize = val
  fetchData(true)
}

function handleCurrentChange(val: number) {
  queryParams.page = val
  fetchData()
}

onMounted(async () => {
  await loadTasks()
  fetchData()
})
</script>

<template>
  <div class="diff-rule-container">
    <div class="toolbar">
      <el-form :inline="true" class="search-form">
        <el-form-item label="所属任务：">
          <el-select
            v-model="queryParams.migrationKey"
            placeholder="请选择"
            style="width: 240px"
            clearable
            filterable
            @change="() => fetchData(true)"
          >
            <el-option
              v-for="task in tasks"
              :key="task.migration_key"
              :label="`${task.migration_key}（${getMigrationTaskStatusMeta(task.status).label}）`"
              :value="task.migration_key"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => fetchData(true)">刷新</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="handleAdd" :disabled="!queryParams.migrationKey">
        新建配置
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="tableData"
      border
      row-key="rule_id"
      :row-class-name="tableRowClassName"
    >
      <el-table-column prop="rule_type" label="规则类型" width="120">
        <template #default="{ row }">
          <el-tag
            :type="
              row.rule_type === 'IGNORE' ? 'info' :
              row.rule_type === 'TOLERANCE' ? 'warning' :
              row.rule_type === 'SORT' ? 'success' : 'primary'
            "
          >
            {{ row.rule_type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="field_path" label="字段路径" min-width="180" show-overflow-tooltip>
        <template #default="{ row }"><code class="fpath">{{ row.field_path }}</code></template>
      </el-table-column>
      <el-table-column prop="rule_value" label="规则值" min-width="350" show-overflow-tooltip />
      <el-table-column prop="weight" label="权重" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.weight || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enable" label="启用状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enable" @change="() => handleEnableChange(row)" />
        </template>
      </el-table-column>

      <el-table-column prop="update_time" label="更新时间" width="180" />
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 弹窗 -->
    <EditDialog ref="editDialogRef" @success="fetchData" />
  </div>
</template>

<style scoped>
.diff-rule-container {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgb(0 21 41 / 8%);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  gap: 16px;
}

:deep(.disabled-row) {
  opacity: 0.6;
  background-color: #f9f9f9;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.fpath {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  color: #909399;
}
</style>
