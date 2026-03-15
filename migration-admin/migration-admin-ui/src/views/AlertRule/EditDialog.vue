<script setup lang="ts">
import { ref, reactive, nextTick, computed } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { createAlertRule, updateAlertRule, type AlertRule, type CreateAlertRuleRequest, type UpdateAlertRuleRequest } from '@/api/alertRule'
import { alertTemplateApi } from '@/api/alertTemplate'
import type { AlertTemplate } from '@/types'
import VueMonacoEditor from '@guolao/vue-monaco-editor'

const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentMigrationKey = ref('')
const realTemplates = ref<AlertTemplate[]>([])
const loadingTemplates = ref(false)

const formData = reactive({
  rule_id: '',
  name: '',
  channel: 'FEISHU',
  template_key: '',
  receivers: '',
  enable: true,
})

const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择通知渠道', trigger: 'change' }],
  template_key: [{ required: true, message: '请选择或输入消息模板', trigger: 'change' }],
  receivers: [{ required: true, message: '请填写至少一个接收人', trigger: 'blur' }],
}

const channelOptions = [
  { label: '飞书 (FEISHU)', value: 'FEISHU' },
  { label: '邮件 (EMAIL)', value: 'EMAIL', disabled: true },
]

const fetchTemplates = async (channel?: string) => {
  loadingTemplates.value = true
  try {
    const res = await alertTemplateApi.list(channel)
    realTemplates.value = res.list
  } catch (e) {
    console.error('Failed to fetch templates:', e)
  } finally {
    loadingTemplates.value = false
  }
}

const templateOptions = computed(() => {
  return realTemplates.value.map(t => ({
    label: `${t.name} (${t.template_key})`,
    value: t.template_key
  }))
})

const receiverLabel = computed(() => {
  return formData.channel === 'FEISHU' ? 'Webhook URL' : '接收邮箱'
})

const editorOptions: any = {
  theme: 'vs-light',
  language: 'text', // URL/Email is plain text
  minimap: { enabled: false },
  lineNumbersMinChars: 3,
  glyphMargin: false,
  lineDecorationsWidth: 0,
  scrollBeyondLastLine: false,
  wordWrap: 'on',
  fontSize: 13,
  fontFamily: "'JetBrains Mono', Consolas, Monaco, monospace",
  renderLineHighlight: 'all',
  scrollbar: {
    vertical: 'visible',
    horizontal: 'visible',
  },
  tabSize: 2,
  automaticLayout: true,
}

const open = async (migrationKey: string, row?: AlertRule) => {
  currentMigrationKey.value = migrationKey
  isEdit.value = !!row
  visible.value = true

  // 初始加载当前渠道的模板
  await fetchTemplates(row?.channel || 'FEISHU')

  nextTick(() => {
    formRef.value?.resetFields()
    if (row) {
      formData.rule_id = row.rule_id
      formData.name = row.name
      formData.channel = row.channel
      formData.template_key = row.template_key
      formData.receivers = row.receivers.join('\n')
      formData.enable = row.enable
    } else {
      formData.rule_id = ''
      formData.name = ''
      formData.channel = 'FEISHU'
      formData.template_key = ''
      formData.receivers = ''
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
        const receivers = formData.receivers.split(/[\n,]/).map(s => s.trim()).filter(s => !!s)
        if (isEdit.value) {
          const req: UpdateAlertRuleRequest = {
            migration_key: currentMigrationKey.value,
            rule_id: formData.rule_id,
            name: formData.name,
            channel: formData.channel,
            template_key: formData.template_key,
            receivers: receivers,
            enable: formData.enable,
          }
          await updateAlertRule(req)
        } else {
          const req: CreateAlertRuleRequest = {
            migration_key: currentMigrationKey.value,
            name: formData.name,
            channel: formData.channel,
            template_key: formData.template_key,
            receivers: receivers,
            enable: formData.enable,
          }
          await createAlertRule(req)
        }

        ElMessage.success(`${isEdit.value ? '编辑' : '新建'}成功`)
        emit('success')
        handleClose()
      } catch (e: any) {
        // http.ts 拦截器在非 silent 场景下已统一处理错误提示
      } finally {
        submitting.value = false
      }
    }
  })
}

defineExpose({
  open,
})

// 监听渠道变化，获取对应渠道的真实模板
import { watch } from 'vue'
watch(() => formData.channel, (newChannel) => {
  fetchTemplates(newChannel as string)
  // 切换渠道时，如果当前模板不在新列表中，可以考虑重置或保留
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑告警规则' : '新建告警规则'"
    width="600px"
    :before-close="handleClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="edit-form"
    >
      <el-form-item label="所属任务">
        <el-input :model-value="currentMigrationKey" disabled />
      </el-form-item>

      <el-form-item label="规则名称" prop="name">
        <el-input v-model="formData.name" placeholder="例如: 核心业务差异告警" maxlength="100" />
      </el-form-item>

      <el-form-item label="通知渠道" prop="channel">
        <el-select 
            v-model="formData.channel" 
            placeholder="请选择" 
            style="width: 100%"
        >
          <el-option
            v-for="item in channelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
            :disabled="item.disabled"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="消息模板" prop="template_key">
        <el-select 
            v-model="formData.template_key" 
            placeholder="请选择模板Key" 
            style="width: 100%"
            filterable
            allow-create
            default-first-option
        >
          <el-option
            v-for="item in templateOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="receiverLabel" prop="receivers">
        <div class="editor-wrapper">
          <vue-monaco-editor
            v-model:value="formData.receivers"
            theme="vs-light"
            :options="editorOptions"
            class="monaco-container"
          />
        </div>
        <div class="tip" v-if="formData.channel === 'FEISHU'">可以输入多个Webhook URL，每一行代表一个地址。</div>
        <div class="tip" v-else>可以输入多个邮箱地址，每一行代表一个地址。</div>
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
.editor-wrapper {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.2s;
  resize: vertical;
  height: 200px; /* Use fixed height for resizability and child filling */
  min-height: 120px;
}
.editor-wrapper:hover {
  border-color: #c0c4cc;
}
.monaco-container {
  height: 100%;
  width: 100%;
}
.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
