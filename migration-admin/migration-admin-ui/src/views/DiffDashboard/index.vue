<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { getDiffStatistics } from '@/api/diffRecord'
import { useMigrationTaskStore } from '@/store'
import { MIGRATION_STATUS_OPTIONS } from '@/constants'
import type { DiffStatistics, TaskOption } from '@/types'

const UI_TEXT = {
  pageTitle: 'Diff大盘',
  taskPlaceholder: '请选择迁移任务',
  statusPlaceholder: '迁移状态（为空表示全部）',
  startDatePlaceholder: '开始日期',
  endDatePlaceholder: '结束日期',
  rangeSeparator: '至',
  filterButton: '筛选',
  refreshButton: '刷新',
  totalLabel: 'Diff请求总数',
  diffLabel: 'Diff不一致数',
  consistencyLabel: 'Diff一致率',
  statisticsChartTitle: 'Diff统计图',
  costChartTitle: '响应时间对比',
  countAxis: '次数',
  consistencyAxis: '一致率',
  oldCostLegend: '旧接口耗时',
  newCostLegend: '新接口耗时',
  trendCompare: '较前一周期',
} as const

const taskStore = useMigrationTaskStore()

const filterForm = reactive({
  migration_key: '',
  migration_status: undefined as number | undefined,
  date_range: [] as string[],
  granularity: 'HOUR' as 'MINUTE' | 'HOUR' | 'DAY',
})

const GRANULARITY_OPTIONS = [
  { label: '分钟', value: 'MINUTE' },
  { label: '小时', value: 'HOUR' },
  { label: '天', value: 'DAY' },
]

const loading = ref(false)
const statistics = ref<DiffStatistics | null>(null)

const diffStatisticsChartRef = ref<HTMLDivElement>()
const costChartRef = ref<HTMLDivElement>()

let diffStatisticsChart: echarts.ECharts | null = null
let costChart: echarts.ECharts | null = null

// 聚合数据计算
const summaryData = computed(() => {
  if (!statistics.value || statistics.value.points.length === 0) {
    return { total: 0, diff: 0, rate: 0 }
  }
  const total = statistics.value.points.reduce((acc, p) => acc + p.total_count, 0)
  const diff = statistics.value.points.reduce((acc, p) => acc + p.diff_count, 0)
  const rate = total === 0 ? 0 : (total - diff) / total
  return { total, diff, rate }
})

const consistencyRateDisplay = computed(() => {
  return `${(summaryData.value.rate * 100).toFixed(2)}%`
})

// 迁移任务选项直接展示所有任务，不与状态联动
const taskOptions = computed<TaskOption[]>(() => taskStore.taskOptions)

// 趋势计算（基于最后两个采样点）
const trendData = computed(() => {
  const pts = statistics.value?.points || []
  if (pts.length < 2) {
    return { total: 0, diff: 0, rate: 0 }
  }
  const last = pts[pts.length - 1]!
  const prev = pts[pts.length - 2]!
  return {
    total: last.total_count - prev.total_count,
    diff: last.diff_count - prev.diff_count,
    rate: last.diff_rate - prev.diff_rate,
  }
})

function formatTrend(delta: number, format: 'count' | 'percent'): string {
  const arrow = delta >= 0 ? '↑' : '↓'
  const absolute = Math.abs(delta)
  if (format === 'percent') {
    return `${arrow}${(absolute * 100).toFixed(2)}% ${UI_TEXT.trendCompare}`
  }
  return `${arrow}${Math.round(absolute)} ${UI_TEXT.trendCompare}`
}

function getTrendClass(delta: number): string {
  return delta > 0 ? 'trend-up' : delta < 0 ? 'trend-down' : 'trend-flat'
}

async function handleMigrationTaskStatusChange(): Promise<void> {
  await loadDashboard()
}

function buildDateRangeParams(): { start_date?: string; end_date?: string } {
  if (filterForm.date_range && filterForm.date_range.length === 2) {
    const [rawStart, rawEnd] = filterForm.date_range
    if (!rawStart || !rawEnd) return {}
    
    let start = rawStart
    let end = rawEnd
    // 补全时间以便后端 LocalDateTime 解析
    if (start.length === 10) start += ' 00:00:00'
    if (end.length === 10) end += ' 23:59:59'
    return {
      start_date: start,
      end_date: end,
    }
  }
  return {}
}

function initCharts(): void {
  if (diffStatisticsChartRef.value && !diffStatisticsChart) {
    diffStatisticsChart = echarts.init(diffStatisticsChartRef.value)
  }
  if (costChartRef.value && !costChart) {
    costChart = echarts.init(costChartRef.value)
  }
}

function buildStatisticsOption(): echarts.EChartsOption {
  const points = statistics.value?.points || []
  const xAxis = points.map((p) => p.time_point)
  
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: [UI_TEXT.totalLabel, UI_TEXT.diffLabel, UI_TEXT.consistencyLabel], top: 0 },
    grid: { left: 40, right: 60, top: 45, bottom: 35 },
    xAxis: { type: 'category', data: xAxis, axisLabel: { color: '#909399' } },
    yAxis: [
      { type: 'value', name: UI_TEXT.countAxis, axisLabel: { color: '#909399' } },
      { type: 'value', name: UI_TEXT.consistencyAxis, min: 0, max: 100, axisLabel: { formatter: '{value}%', color: '#909399' } },
    ],
    series: [
      {
        name: UI_TEXT.totalLabel,
        type: 'bar',
        data: points.map(p => p.total_count),
        barMaxWidth: 26,
        itemStyle: { color: 'rgba(64, 158, 255, 0.42)', borderRadius: [4, 4, 0, 0] },
      },
      {
        name: UI_TEXT.diffLabel,
        type: 'line',
        smooth: true,
        data: points.map(p => p.diff_count),
        lineStyle: { color: '#f56c6c', width: 3 },
      },
      {
        name: UI_TEXT.consistencyLabel,
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: points.map(p => Number(((1 - p.diff_rate) * 100).toFixed(2))),
        lineStyle: { color: '#67c23a', width: 4 },
      },
    ],
  }
}

function buildCostOption(): echarts.EChartsOption {
  const points = statistics.value?.points || []
  const xAxis = points.map((p) => p.time_point)

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [UI_TEXT.oldCostLegend, UI_TEXT.newCostLegend], top: 0 },
    xAxis: { type: 'category', data: xAxis, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', axisLabel: { color: '#909399' } },
    series: [
      {
        name: UI_TEXT.oldCostLegend,
        type: 'line',
        smooth: true,
        data: points.map((p) => p.avg_old_cost_time),
        lineStyle: { color: '#909399' },
      },
      {
        name: UI_TEXT.newCostLegend,
        type: 'line',
        smooth: true,
        data: points.map((p) => p.avg_new_cost_time),
        lineStyle: { color: '#409eff' },
      },
    ],
  }
}

function renderCharts(): void {
  initCharts()
  if (!diffStatisticsChart || !costChart) return
  diffStatisticsChart.setOption(buildStatisticsOption())
  costChart.setOption(buildCostOption())
}

function resizeCharts(): void {
  diffStatisticsChart?.resize()
  costChart?.resize()
}

function disposeCharts(): void {
  diffStatisticsChart?.dispose()
  diffStatisticsChart = null
  costChart?.dispose()
  costChart = null
}

async function loadDashboard(): Promise<void> {
  if (!filterForm.migration_key) {
    statistics.value = null
    await nextTick()
    renderCharts()
    return
  }

  loading.value = true
  try {
    const dateRangeParams = buildDateRangeParams()
    const statisticsResult = await getDiffStatistics({
      migration_key: filterForm.migration_key,
      migration_status: filterForm.migration_status,
      granularity: filterForm.granularity,
      ...dateRangeParams,
    })
    statistics.value = statisticsResult
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function handleGranularityChange(): void {
  // 切换粒度时重置时间范围，避免格式不兼容或跨度过大
  filterForm.date_range = []
  loadDashboard()
}

async function initPage(): Promise<void> {
  const taskOptions = await taskStore.fetchTaskOptions()
  const firstOption = taskOptions[0]
  if (!filterForm.migration_key && firstOption) {
    filterForm.migration_key = firstOption.value
  }
  await loadDashboard()
}

onMounted(async () => {
  await initPage()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<template>
  <el-card class="page-card" shadow="never" v-loading="loading">
    <div class="page-toolbar">
      <h2 class="page-title">{{ UI_TEXT.pageTitle }}</h2>
      <el-select
        v-model="filterForm.migration_key"
        :placeholder="UI_TEXT.taskPlaceholder"
        filterable
        style="width: 220px"
        @change="loadDashboard"
      >
        <el-option
          v-for="item in taskOptions"
          :key="item.value"
          :label="item.value"
          :value="item.value"
        />
      </el-select>

      <el-select
        v-model="filterForm.migration_status"
        clearable
        filterable
        :placeholder="UI_TEXT.statusPlaceholder"
        style="width: 160px"
        @change="handleMigrationTaskStatusChange"
      >
        <el-option
          v-for="status in MIGRATION_STATUS_OPTIONS"
          :key="status.value"
          :label="status.label"
          :value="status.value"
        />
      </el-select>

      <el-date-picker
        v-model="filterForm.date_range"
        :type="filterForm.granularity === 'DAY' ? 'daterange' : 'datetimerange'"
        value-format="YYYY-MM-DD HH:mm:ss"
        :start-placeholder="UI_TEXT.startDatePlaceholder"
        :end-placeholder="UI_TEXT.endDatePlaceholder"
        :range-separator="UI_TEXT.rangeSeparator"
        style="width: 300px"
      />

      <el-radio-group v-model="filterForm.granularity" @change="handleGranularityChange" style="margin-left: 12px">
        <el-radio-button v-for="opt in GRANULARITY_OPTIONS" :key="opt.value" :label="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>

      <el-button type="primary" :icon="Refresh" @click="loadDashboard" style="margin-left: 12px">
        {{ UI_TEXT.filterButton }}
      </el-button>
    </div>

    <el-row :gutter="12" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.totalLabel }}</span>
          <strong class="stat-value">{{ summaryData.total }}</strong>
          <small class="stat-trend" :class="getTrendClass(trendData.total)">
            {{ formatTrend(trendData.total, 'count') }}
          </small>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.diffLabel }}</span>
          <strong class="stat-value danger">{{ summaryData.diff }}</strong>
          <small class="stat-trend" :class="getTrendClass(-trendData.diff)">
            {{ formatTrend(-trendData.diff, 'count') }}
          </small>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.consistencyLabel }}</span>
          <strong class="stat-value success">{{ consistencyRateDisplay }}</strong>
          <small class="stat-trend" :class="getTrendClass(trendData.rate)">
            {{ formatTrend(trendData.rate, 'percent') }}
          </small>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-row">
      <template #header>
        <span>{{ UI_TEXT.statisticsChartTitle }}</span>
      </template>
      <div ref="diffStatisticsChartRef" class="chart-area" />
    </el-card>

    <el-card shadow="never" class="chart-row">
      <template #header>
        <span>{{ UI_TEXT.costChartTitle }}</span>
      </template>
      <div ref="costChartRef" class="chart-area" />
    </el-card>
  </el-card>
</template>

<style scoped>
.stat-row {
  margin-top: 8px;
}

.stat-card {
  min-height: 126px;
}

.stat-label {
  display: block;
  color: #909399;
  font-size: 13px;
}

.stat-value {
  display: block;
  margin: 8px 0;
  font-size: 28px;
  line-height: 1;
  color: #303133;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.success {
  color: #67c23a;
}

.stat-trend {
  display: block;
  font-size: 12px;
  margin-bottom: 2px;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

.trend-flat {
  color: #909399;
}

.chart-row {
  margin-top: 12px;
}

.chart-area {
  width: 100%;
  height: 320px;
}
</style>
