<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createMigrationTask, updateMigrationTask } from '@/api/migrationTask'
import { MIGRATION_STATUS_OPTIONS } from '@/constants'
import type { MigrationTask } from '@/types'

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode: 'create' | 'edit'
    task?: MigrationTask | null
  }>(),
  {
    task: null,
  },
)

const emit = defineEmits<{
  (event: 'update:visible', visible: boolean): void
  (event: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formModel = reactive({
  migration_key: '',
  status: 1,
  description: '',
})

const dialogTitle = computed(() => (props.mode === 'create' ? '新建迁移任务' : '编辑迁移任务'))

const rules: FormRules = {
  migration_key: [
    { required: true, message: '请输入 migration_key', trigger: 'blur' },
    { min: 3, max: 128, message: '长度需在 3 到 128 之间', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择迁移状态', trigger: 'change' }],
}

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

function resetForm(): void {
  formModel.migration_key = ''
  formModel.status = 1
  formModel.description = ''
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return
    }
    if (props.mode === 'edit' && props.task) {
      formModel.migration_key = props.task.migration_key
      formModel.status = props.task.status
      formModel.description = props.task.description || ''
      return
    }
    resetForm()
  },
)

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    const description = formModel.description.trim() || undefined
    if (props.mode === 'create') {
      await createMigrationTask({
        migration_key: formModel.migration_key.trim(),
        status: formModel.status,
        description,
      })
      ElMessage.success('迁移任务创建成功')
    } else {
      await updateMigrationTask({
        migration_key: formModel.migration_key,
        status: formModel.status,
        description,
      })
      ElMessage.success('迁移任务更新成功')
    }

    emit('success')
    emit('update:visible', false)
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px">
      <el-form-item label="migration_key" prop="migration_key">
        <el-input
          v-model="formModel.migration_key"
          :disabled="mode === 'edit'"
          placeholder="建议格式: 服务名-接口名，如 user-getUser-api"
          maxlength="128"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="formModel.status" placeholder="请选择状态" style="width: 100%">
          <el-option
            v-for="item in MIGRATION_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formModel.description"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          placeholder="请输入任务描述"
          maxlength="255"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-space>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </el-space>
    </template>
  </el-dialog>
</template>
