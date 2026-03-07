<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { createDiffRule, updateDiffRule, type DiffRule, type CreateDiffRuleRequest, type UpdateDiffRuleRequest } from '@/api/diffRule'

const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentMigrationKey = ref('')

const formData = reactive({
  rule_id: '',
  rule_type: 'IGNORE',
  field_path: '',
  rule_value: '',
  enable: true,
})

const rules = {
  rule_type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  field_path: [{ required: true, message: '请输入字段路径', trigger: 'blur' }],
}

const ruleTypeOptions = [
  { label: 'IGNORE (忽略对比)', value: 'IGNORE' },
  { label: 'TOLERANCE (数值容差)', value: 'TOLERANCE' },
  { label: 'SCRIPT (SpEL脚本)', value: 'SCRIPT' },
  { label: 'SORT (数组排序)', value: 'SORT' },
]

function getRuleValueLabel(type: string) {
  switch (type) {
    case 'TOLERANCE': return '容差值'
    case 'SCRIPT': return 'SpEL表达式'
    case 'SORT': return '排序字段'
    default: return '规则值'
  }
}

function getRuleValuePlaceholder(type: string) {
  switch (type) {
    case 'IGNORE': return '可选：备注忽略原因'
    case 'TOLERANCE': return '必填：例如 0.01 (支持的误差范围)'
    case 'SCRIPT': return '必填：例如 #oldValue == #newValue，支持参数: #oldValue, #newValue, #fieldPath'
    case 'SORT': return '必填：数组元素的唯一标识字段名，例如 id'
    default: return '请输入规则值'
  }
}

function handleRuleTypeChange() {
  if (!isEdit.value) {
    formData.rule_value = ''
  }
}

const open = (migrationKey: string, row?: DiffRule) => {
  currentMigrationKey.value = migrationKey
  isEdit.value = !!row
  visible.value = true

  nextTick(() => {
    formRef.value?.resetFields()
    if (row) {
      formData.rule_id = row.rule_id
      formData.rule_type = row.rule_type
      formData.field_path = row.field_path
      formData.rule_value = row.rule_value || ''
      formData.enable = row.enable
    } else {
      formData.rule_id = ''
      formData.rule_type = 'IGNORE'
      formData.field_path = ''
      formData.rule_value = ''
      formData.enable = true
    }
  })
}

const handleClose = () => {
  visible.value = false
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (formData.rule_type === 'TOLERANCE') {
            if (!formData.rule_value || isNaN(Number(formData.rule_value))) {
                 ElMessage.error('容差规则的值必须为合法的数字')
                 submitting.value = false;
                 return;
            }
        } else if (formData.rule_type === 'SCRIPT' || formData.rule_type === 'SORT') {
            if (!formData.rule_value) {
                ElMessage.error(`${formData.rule_type} 规则必须提供规则值`)
                submitting.value = false;
                return;
            }
        }

        if (isEdit.value) {
          const req: UpdateDiffRuleRequest = {
            migration_key: currentMigrationKey.value,
            rule_id: formData.rule_id,
            rule_type: formData.rule_type,
            field_path: formData.field_path,
            rule_value: formData.rule_value,
            enable: formData.enable,
          }
          await updateDiffRule(req)
        } else {
          const req: CreateDiffRuleRequest = {
            migration_key: currentMigrationKey.value,
            rule_type: formData.rule_type,
            field_path: formData.field_path,
            rule_value: formData.rule_value,
            enable: formData.enable,
          }
          await createDiffRule(req)
        }

        ElMessage.success(`${isEdit.value ? '编辑' : '新建'}成功`)
        emit('success')
        handleClose()
      } catch (e: any) {
        // http.ts 拦截器在非 silent 场景下已统一处理错误提示，这里保留兜底逻辑
        if (e.message !== '请求失败，请稍后重试' && e.message !== '网络请求异常') {
          // api/http 里的 ElMessage 已覆盖大部分场景，这里保留兜底处理
        }
      } finally {
        submitting.value = false
      }
    }
  })
}

defineExpose({
  open,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑Diff规则' : '新建Diff规则'"
    width="600px"
    :before-close="handleClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      class="edit-form"
    >
      <el-form-item label="规则类型" prop="rule_type">
        <el-select 
            v-model="formData.rule_type" 
            placeholder="请选择规则类型" 
            style="width: 100%"
            filterable
            @change="handleRuleTypeChange"
        >
          <el-option
            v-for="item in ruleTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="字段路径" prop="field_path">
        <el-input 
            v-model="formData.field_path" 
            placeholder="JSONPath 格式，例如: $.data.userList[*].score" 
        />
        <div class="tip">使用 JsonPath 语法定位要配置的 JSON 字段。</div>
      </el-form-item>

      <el-form-item :label="getRuleValueLabel(formData.rule_type)" prop="rule_value" :required="formData.rule_type !== 'IGNORE'">
        <el-input 
            v-model="formData.rule_value" 
            type="textarea"
            :rows="parseFloat(formData.rule_type === 'SCRIPT' ? '4' : '2')"
            :placeholder="getRuleValuePlaceholder(formData.rule_type)" 
        />
      </el-form-item>

      <el-form-item label="是否启用" prop="enable">
        <el-switch v-model="formData.enable" />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.edit-form {
  padding: 10px 20px 0 0;
}
.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
