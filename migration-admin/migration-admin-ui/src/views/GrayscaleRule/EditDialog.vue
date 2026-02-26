<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createGrayscaleRule, updateGrayscaleRule, type CreateGrayscaleRulePayload } from '@/api/grayscaleRule'
import { GRAYSCALE_RULE_TYPE_DESC, GRAYSCALE_RULE_TYPE_OPTIONS, getGrayscaleRuleTypeLabel } from '@/constants'
import type { GrayscaleRule, TaskOption } from '@/types'

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode: 'create' | 'edit'
    taskOptions: TaskOption[]
    defaultMigrationKey?: string
    rule?: GrayscaleRule | null
  }>(),
  {
    defaultMigrationKey: '',
    rule: null,
  },
)

const emit = defineEmits<{
  (event: 'update:visible', visible: boolean): void
  (event: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formModel = reactive<CreateGrayscaleRulePayload & { rule_id: string }>({
  migration_key: '',
  rule_type: 'PERCENTAGE',
  rule_value: '',
  enable: true,
  rule_id: '',
})

const dialogTitle = computed(() => (props.mode === 'create' ? '新建灰度规则' : '编辑灰度规则'))

const ruleValueTip = computed(() => {
  return GRAYSCALE_RULE_TYPE_DESC[formModel.rule_type] ?? '请输入规则值'
})

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

function validateRuleValue(_rule: unknown, value: string, callback: (error?: string | Error) => void): void {
  const text = value.trim()
  if (!text) {
    callback(new Error('请输入规则值'))
    return
  }

  if (formModel.rule_type === 'PERCENTAGE') {
    const number = Number(text)
    if (!Number.isFinite(number) || number < 0 || number > 100) {
      callback(new Error('百分比需为 0-100 的数字'))
      return
    }
  }

  if (formModel.rule_type === 'BLACKLIST' || formModel.rule_type === 'WHITELIST') {
    try {
      const parsed = JSON.parse(text)
      if (!Array.isArray(parsed)) {
        callback(new Error('黑白名单规则值必须是 JSON 数组'))
        return
      }
    } catch {
      callback(new Error('请输入合法 JSON 数组，例如 ["1001","1002"]'))
      return
    }
  }

  callback()
}

const rules: FormRules = {
  migration_key: [{ required: true, message: '请选择迁移任务', trigger: 'change' }],
  rule_type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  rule_value: [{ validator: validateRuleValue, trigger: 'blur' }],
}

function resetForm(): void {
  formModel.rule_id = ''
  formModel.migration_key = props.defaultMigrationKey || ''
  formModel.rule_type = 'PERCENTAGE'
  formModel.rule_value = ''
  formModel.enable = true
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return
    }

    if (props.mode === 'edit' && props.rule) {
      formModel.rule_id = props.rule.rule_id
      formModel.migration_key = props.rule.migration_key
      formModel.rule_type = props.rule.rule_type
      formModel.rule_value = props.rule.rule_value
      formModel.enable = props.rule.enable
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
    if (props.mode === 'create') {
      await createGrayscaleRule({
        migration_key: formModel.migration_key,
        rule_type: formModel.rule_type,
        rule_value: formModel.rule_value.trim(),
        enable: formModel.enable,
      })
      ElMessage.success('灰度规则创建成功')
    } else {
      await updateGrayscaleRule({
        migration_key: formModel.migration_key,
        rule_id: formModel.rule_id,
        rule_type: formModel.rule_type,
        rule_value: formModel.rule_value.trim(),
        enable: formModel.enable,
      })
      ElMessage.success('灰度规则更新成功')
    }

    emit('success')
    emit('update:visible', false)
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px">
      <el-form-item label="迁移任务" prop="migration_key">
        <el-select
          v-model="formModel.migration_key"
          placeholder="请选择迁移任务"
          :disabled="mode === 'edit'"
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="item in taskOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="规则类型" prop="rule_type">
        <el-select v-model="formModel.rule_type" style="width: 100%">
          <el-option
            v-for="item in GRAYSCALE_RULE_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
            <span>{{ item.label }}</span>
            <span class="type-subtext">{{ GRAYSCALE_RULE_TYPE_DESC[item.value] }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="规则值" prop="rule_value">
        <el-input
          v-model="formModel.rule_value"
          type="textarea"
          :autosize="{ minRows: 4, maxRows: 8 }"
          :placeholder="ruleValueTip"
        />
      </el-form-item>

      <el-form-item label="启用状态">
        <el-switch v-model="formModel.enable" />
      </el-form-item>

      <el-alert
        :title="`当前类型：${getGrayscaleRuleTypeLabel(formModel.rule_type)}`"
        :description="ruleValueTip"
        type="info"
        show-icon
        :closable="false"
      />
    </el-form>

    <template #footer>
      <el-space>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </el-space>
    </template>
  </el-dialog>
</template>

<style scoped>
.type-subtext {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
