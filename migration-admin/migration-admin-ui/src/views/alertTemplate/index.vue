<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button
        class="filter-item"
        type="primary"
        icon="Plus"
        @click="handleCreate"
      >
        新增模板
      </el-button>
    </div>

    <!-- 列表数据 -->
    <el-table
      v-loading="loading"
      :data="list"
      border
      fit
      highlight-current-row
      style="width: 100%"
    >
      <el-table-column label="模板Key (唯一标识)" prop="template_key" min-width="150" show-overflow-tooltip />
      <el-table-column label="模板名称" prop="name" min-width="150" />
      <el-table-column label="通知渠道" prop="channel" width="120">
        <template #default="{ row }">
          <el-tag :type="row.channel === 'FEISHU' ? 'success' : 'info'">
            {{ row.channel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容预览" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ JSON.stringify(row.template) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="create_time" width="160" />
      <el-table-column label="最近更新" prop="update_time" width="160" />

      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link icon="Edit" @click="handleEdit(row)">
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑/创建弹窗 -->
    <edit-dialog
      ref="editDialogRef"
      @success="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { AlertTemplate } from '@/types'
import { alertTemplateApi } from '@/api/alertTemplate'
import EditDialog from './components/EditDialog.vue'

const list = ref<AlertTemplate[]>([])
const loading = ref(false)
const editDialogRef = ref<InstanceType<typeof EditDialog>>()

const getList = async () => {
  try {
    loading.value = true
    const res = await alertTemplateApi.list()
    // The http client unwraps ApiResult.data, so res is the PageResult itself.
    list.value = res.list || (res as any).records || []
  } catch (error) {
    console.error('Failed to get template list:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  editDialogRef.value?.open()
}

// Removed unused validateJson from index.vue

const handleEdit = (row: AlertTemplate) => {
  editDialogRef.value?.open(row)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.filter-container {
  margin-bottom: 20px;
}
</style>
