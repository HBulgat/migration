<script setup lang="ts">
import { computed } from 'vue'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'
import { parseJsonSafely } from '@/utils/format'

type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue }

const props = withDefaults(
  defineProps<{
    data: unknown
    deep?: number
  }>(),
  {
    deep: 3,
  },
)

const normalizedData = computed(() => parseJsonSafely(props.data))
const prettyData = computed(() => normalizedData.value as JsonValue)
</script>

<template>
  <div class="json-viewer">
    <el-empty
      v-if="normalizedData === null || normalizedData === undefined || normalizedData === ''"
      description="暂无数据"
      :image-size="72"
    />
    <vue-json-pretty
      v-else
      :data="prettyData"
      :deep="deep"
      :show-line="true"
      :show-line-number="true"
      :show-length="true"
    />
  </div>
</template>

<style scoped>
.json-viewer {
  width: 100%;
  min-height: 180px;
  max-height: 420px;
  overflow: auto;
  background: #fff;
}
</style>
