<script setup lang="ts">
import { computed } from 'vue'
import type { DiffItem } from '@/types'
import { parseJsonSafely } from '@/utils/format'
import { VueMonacoDiffEditor } from '@guolao/vue-monaco-editor'

const props = withDefaults(
  defineProps<{
    oldResponse: unknown
    newResponse: unknown
    diffResults?: DiffItem[]
    activePath?: string
  }>(),
  {
    diffResults: () => [],
    activePath: '',
  },
)

const diffPaths = computed(() => props.diffResults.map((item) => item.field_path))

const originalValue = computed(() => {
  const parsed = parseJsonSafely(props.oldResponse)
  return typeof parsed === 'string' ? parsed : JSON.stringify(parsed, null, 2)
})

const modifiedValue = computed(() => {
  const parsed = parseJsonSafely(props.newResponse)
  return typeof parsed === 'string' ? parsed : JSON.stringify(parsed, null, 2)
})

const editorOptions: any = {
  readOnly: true,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  fontSize: 13,
  fontFamily: "'Fira Code', Consolas, Monaco, monospace",
  renderSideBySide: true,
  lineNumbersMinChars: 3,
  glyphMargin: false,
  lineDecorationsWidth: 0,
  scrollbar: {
    vertical: 'visible',
    horizontal: 'visible'
  },
  diffWordWrap: 'on'
}
</script>

<template>
  <div class="diff-viewer">
    <div class="path-list">
      <span class="path-title">差异路径：</span>
      <el-tag
        v-for="path in diffPaths"
        :key="path"
        :type="activePath === path ? 'danger' : 'info'"
        effect="light"
        size="small"
      >
        {{ path }}
      </el-tag>
      <span v-if="diffPaths.length === 0" class="path-empty">暂无差异字段</span>
    </div>

    <div class="viewer-container">
      <div class="viewer-header">
        <div class="header-item">旧接口响应</div>
        <div class="header-item">新接口响应</div>
      </div>
      <div class="editor-wrapper">
        <VueMonacoDiffEditor
          theme="vs-light"
          language="json"
          :original="originalValue"
          :modified="modifiedValue"
          :options="editorOptions"
          class="monaco-diff-container"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.diff-viewer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.path-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.path-title {
  color: #606266;
  font-size: 13px;
}

.path-empty {
  color: #909399;
  font-size: 13px;
}

.viewer-container {
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.viewer-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f8f9fb;
  border-bottom: 1px solid #ebeef5;
}

.header-item {
  padding: 8px 16px;
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.header-item:first-child {
  border-right: 1px solid #ebeef5;
}

.editor-wrapper {
  height: 500px;
  width: 100%;
}

.monaco-diff-container {
  height: 100%;
  width: 100%;
}
</style>
