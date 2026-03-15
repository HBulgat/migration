<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlertRuleList, deleteAlertRule, updateAlertRuleEnable, type AlertRule } from '@/api/alertRule'
import { getMigrationTaskList } from '@/api/migrationTask'
import { getMigrationTaskStatusMeta } from '@/constants'
import type { MigrationTask } from '@/types'
import EditDialog from './EditDialog.vue'

const tableData = ref<AlertRule[]>([])
const loading = ref(false)
const tasks = ref<MigrationTask[]>([])

const queryParams = reactive({
  migrationKey: '',
  channel: '',
})

const editDialogRef = ref<InstanceType<typeof EditDialog> | null>(null)



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

async function fetchData() {
  if (!queryParams.migrationKey) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    const res = await getAlertRuleList({
      migration_key: queryParams.migrationKey,
      channel: queryParams.channel || undefined,
    })
    tableData.value = res
  } catch (error: any) {
    ElMessage.error(error.message || '加载告警规则异常')
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

function handleEdit(row: AlertRule) {
  editDialogRef.value?.open(row.migration_key, row)
}

function handleDelete(row: AlertRule) {
  ElMessageBox.confirm('确定要删除该告警规则吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await deleteAlertRule({
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

async function handleEnableChange(row: AlertRule) {
  try {
    await updateAlertRuleEnable({
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

onMounted(async () => {
  await loadTasks()
  fetchData()
})
</script>

<template>
  <div class="alert-rule-container">
    <div class="toolbar">
      <el-form :inline="true" class="search-form">
        <el-form-item label="所属任务：">
          <el-select
            v-model="queryParams.migrationKey"
            placeholder="请选择"
            style="width: 240px"
            clearable
            filterable
            @change="() => fetchData()"
          >
            <el-option
              v-for="task in tasks"
              :key="task.migration_key"
              :label="`${task.migration_key}（${getMigrationTaskStatusMeta(task.status).label}）`"
              :value="task.migration_key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道：">
          <el-select
            v-model="queryParams.channel"
            placeholder="全部渠道"
            style="width: 150px"
            clearable
            @change="fetchData"
          >
            <el-option label="飞书" value="FEISHU" />
            <el-option label="邮件" value="EMAIL" disabled />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => fetchData()">刷新</el-button>
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
    >
      <el-table-column prop="name" label="规则名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="channel" label="通知渠道" width="120">
        <template #default="{ row }">
          <el-tag :type="row.channel === 'FEISHU' ? 'primary' : 'success'">
            {{ row.channel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="template_key" label="消息模板" min-width="150">
        <template #default="{ row }"><code>{{ row.template_key }}</code></template>
      </el-table-column>
      <el-table-column prop="receivers" label="接收人" min-width="200">
        <template #default="{ row }">
          <div class="receivers-wrap">
            <template v-for="(receiver, idx) in row.receivers" :key="idx">
              <el-tooltip v-if="idx < 2" :content="receiver" placement="top" effect="dark">
                <el-tag size="small" class="receiver-tag" type="info">
                  {{ receiver }}
                </el-tag>
              </el-tooltip>
            </template>
            <el-tooltip v-if="row.receivers && row.receivers.length > 2" effect="dark" placement="top">
              <template #content>
                <div v-for="r in row.receivers" :key="r">{{ r }}</div>
              </template>
              <el-tag size="small" type="info" class="receiver-tag">+{{ row.receivers.length - 2 }}</el-tag>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="enable" label="启用状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enable" @change="() => handleEnableChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <EditDialog ref="editDialogRef" @success="fetchData" />
  </div>
</template>

<style scoped>
.alert-rule-container {
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



.receivers-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.receiver-tag {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.receiver-tag .el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
}
</style>
