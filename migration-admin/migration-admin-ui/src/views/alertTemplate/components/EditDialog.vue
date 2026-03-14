<template>
  <el-dialog
    :title="isEdit ? '编辑模板' : '新增模板'"
    v-model="visible"
    width="800px"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="模板Key" prop="template_key">
        <el-input 
          v-model="formData.template_key" 
          placeholder="请输入唯一模板Key (如 login_fail_template)" 
          :disabled="isEdit"
        />
        <div class="tip">注：创建后不可修改</div>
      </el-form-item>

      <el-form-item label="模板名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入模板名称" />
      </el-form-item>

      <el-form-item label="通知渠道" prop="channel">
        <el-radio-group v-model="formData.channel">
          <el-radio label="FEISHU">飞书 (Feishu)</el-radio>
          <el-radio label="EMAIL">邮件 (Email)</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="模板内容 (JSON)" prop="templateStr">
        <el-input
          v-model="formData.templateStr"
          type="textarea"
          :rows="12"
          placeholder="请输入 JSON 格式的模板内容"
        />
        <div class="tip">支持 ${variable} 占位符。</div>
      </el-form-item>

    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { AlertTemplate, CreateAlertTemplateRequest, UpdateAlertTemplateRequest } from '@/types'
import { alertTemplateApi } from '@/api/alertTemplate'

const emit = defineEmits(['success'])

const visible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

interface FormData {
  template_key: string
  name: string
  channel: 'FEISHU' | 'EMAIL'
  templateStr: string
}

const formData = reactive<FormData>({
  template_key: '',
  name: '',
  channel: 'FEISHU',
  templateStr: '{\n  "msg_type": "text",\n  "content": {\n    "text": "告警: ${msg}"\n  }\n}'
})

// Custom validator for JSON string
const validateJson = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error('请输入模板内容'))
  }
  try {
    JSON.parse(value)
    callback()
  } catch (e) {
    callback(new Error('请输入合法的 JSON 格式'))
  }
}

const rules = reactive<FormRules>({
  template_key: [{ required: true, message: '请输入模板Key', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择通知渠道', trigger: 'change' }],
  templateStr: [
    { required: true, message: '请输入模板内容', trigger: 'blur' },
    { validator: validateJson, trigger: 'blur' }
  ]
})

const open = (row?: AlertTemplate) => {
  visible.value = true
  isEdit.value = !!row
  
  if (row) {
    formData.template_key = row.template_key
    formData.name = row.name
    formData.channel = row.channel || 'FEISHU'
    formData.templateStr = row.template ? JSON.stringify(row.template, null, 2) : ''
  } else {
    // defaults
    formData.template_key = ''
    formData.name = ''
    formData.channel = 'FEISHU'
    formData.templateStr = '{\n  "msg_type": "text",\n  "content": {\n    "text": "告警: ${msg}"\n  }\n}'
  }
}

const handleClosed = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const payloadTemplate = JSON.parse(formData.templateStr)
        
        if (isEdit.value) {
          const req: UpdateAlertTemplateRequest = {
            template_key: formData.template_key,
            name: formData.name,
            channel: formData.channel,
            template: payloadTemplate
          }
          await alertTemplateApi.update(req)
          ElMessage.success('更新成功')
        } else {
          const req: CreateAlertTemplateRequest = {
            template_key: formData.template_key,
            name: formData.name,
            channel: formData.channel,
            template: payloadTemplate
          }
          await alertTemplateApi.create(req)
          ElMessage.success('创建成功')
        }
        visible.value = false
        emit('success')
      } catch (error: any) {
        ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
      } finally {
        submitLoading.value = false
      }
    }
  })
}

defineExpose({
  open
})
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 5px;
}
</style>
