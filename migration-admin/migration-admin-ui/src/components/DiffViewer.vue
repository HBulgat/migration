<script setup lang="ts">
import { computed } from 'vue'
import type { DiffItem } from '@/types'
import { parseJsonSafely } from '@/utils/format'

interface HighlightedLine {
  html: string
  isDiff: boolean
  isActive: boolean
}

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
const activePathKeywords = computed(() => extractPathKeywords(props.activePath))
const diffKeywords = computed(() => {
  const merged = new Set<string>()
  diffPaths.value.forEach((path) => {
    extractPathKeywords(path).forEach((keyword) => merged.add(keyword))
  })
  return [...merged]
})

const oldLines = computed(() => buildHighlightedLines(parseJsonSafely(props.oldResponse)))
const newLines = computed(() => buildHighlightedLines(parseJsonSafely(props.newResponse)))

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function colorizeJsonLine(line: string): string {
  const escaped = escapeHtml(line)
  return escaped
    .replace(/"([^"\\]+)"(?=\s*:)/g, '<span class="json-key">"$1"</span>')
    .replace(/(:\s*)"([^"\\]*)"/g, '$1<span class="json-string">"$2"</span>')
    .replace(/(:\s*)(-?\d+(?:\.\d+)?)/g, '$1<span class="json-number">$2</span>')
    .replace(/(:\s*)(true|false)/g, '$1<span class="json-boolean">$2</span>')
    .replace(/(:\s*)(null)/g, '$1<span class="json-null">$2</span>')
}

function extractPathKeywords(path: string): string[] {
  if (!path) {
    return []
  }

  const keywords: string[] = []
  const matcher = /([A-Za-z_][\w]*)|\[(\d+)\]/g
  let current = matcher.exec(path)
  while (current) {
    const token = current[1] || current[2]
    if (token && token !== 'data') {
      keywords.push(token)
    }
    current = matcher.exec(path)
  }
  return keywords
}

function buildHighlightedLines(value: unknown): HighlightedLine[] {
  const jsonText = typeof value === 'string' ? value : JSON.stringify(value ?? {}, null, 2)
  return jsonText.split('\n').map((line) => {
    const lowerLine = line.toLowerCase()
    const isDiff = diffKeywords.value.some((keyword) => lineContainsKeyword(lowerLine, keyword))
    const isActive = activePathKeywords.value.some((keyword) => lineContainsKeyword(lowerLine, keyword))
    return {
      html: colorizeJsonLine(line),
      isDiff,
      isActive,
    }
  })
}

function lineContainsKeyword(line: string, keyword: string): boolean {
  const lowerKeyword = keyword.toLowerCase()
  return line.includes(`"${lowerKeyword}"`) || line.includes(`[${lowerKeyword}]`) || line.includes(lowerKeyword)
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

    <div class="viewer-panels">
      <el-card class="viewer-card" shadow="never">
        <template #header>
          <div class="card-header">旧接口响应</div>
        </template>
        <div class="json-panel">
          <div
            v-for="(line, index) in oldLines"
            :key="`old-${index}`"
            class="json-line"
            :class="{ 'diff-line': line.isDiff, 'active-line': line.isActive }"
            v-html="line.html"
          />
        </div>
      </el-card>

      <el-card class="viewer-card" shadow="never">
        <template #header>
          <div class="card-header">新接口响应</div>
        </template>
        <div class="json-panel">
          <div
            v-for="(line, index) in newLines"
            :key="`new-${index}`"
            class="json-line"
            :class="{ 'diff-line': line.isDiff, 'active-line': line.isActive }"
            v-html="line.html"
          />
        </div>
      </el-card>
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

.viewer-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.viewer-card {
  min-height: 300px;
}

.card-header {
  font-weight: 600;
  color: #303133;
}

.json-panel {
  max-height: 420px;
  overflow: auto;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.json-line {
  white-space: pre;
  padding: 0 12px;
  line-height: 22px;
  color: #303133;
}

.diff-line {
  background: #fde2e2;
}

.active-line {
  background: #fbc4c4;
}

:deep(.json-key) {
  color: #92278f;
}

:deep(.json-string) {
  color: #007f5f;
}

:deep(.json-number) {
  color: #0d47a1;
}

:deep(.json-boolean) {
  color: #d35400;
}

:deep(.json-null) {
  color: #909399;
}

@media (max-width: 1200px) {
  .viewer-panels {
    grid-template-columns: 1fr;
  }
}
</style>
