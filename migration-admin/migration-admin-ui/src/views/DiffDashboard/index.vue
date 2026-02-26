<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { Refresh } from '@element-plus/icons-vue'
import { getDiffRecordList, getDiffStatistics } from '@/api/diffRecord'
import { useMigrationTaskStore } from '@/store'
import { MIGRATION_STATUS_OPTIONS } from '@/constants'
import type { DiffRecord, DiffStatistics, TaskOption } from '@/types'

interface DailySummary {
  totalCount: number
  diffCount: number
  consistencyRate: number
}

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
  trendCompare: '较前一日',
} as const

const taskStore = useMigrationTaskStore()

const filterForm = reactive({
  migration_key: '',
  migration_status: undefined as number | undefined,
  date_range: [] as string[],
})

const loading = ref(false)
const statistics = ref<DiffStatistics | null>(null)
const records = ref<DiffRecord[]>([])

const diffStatisticsChartRef = ref<HTMLDivElement>()
const costChartRef = ref<HTMLDivElement>()

let diffStatisticsChart: echarts.ECharts | null = null
let costChart: echarts.ECharts | null = null

const consistencyRateDisplay = computed(() => {
  const diffRate = statistics.value?.diff_rate ?? 0
  const consistencyRate = Math.max(0, Math.min(1, 1 - diffRate))
  return `${(consistencyRate * 100).toFixed(2)}%`
})

const sortedRecords = computed(() =>
  [...records.value].sort((a, b) => dayjs(a.create_time).valueOf() - dayjs(b.create_time).valueOf()),
)

const filteredTaskOptions = computed<TaskOption[]>(() => {
  if (filterForm.migration_status === undefined) {
    return taskStore.taskOptions
  }
  return taskStore.taskOptions.filter((item) => item.status === filterForm.migration_status)
})

const trendContextLabel = computed(() => {
  if (filterForm.date_range.length === 2) {
    return `${dayjs(filterForm.date_range[1]).format('MM-DD')} ${UI_TEXT.trendCompare}`
  }

  const latestRecord = sortedRecords.value.length > 0 ? sortedRecords.value[sortedRecords.value.length - 1] : undefined
  if (latestRecord?.create_time) {
    return `${dayjs(latestRecord.create_time).format('MM-DD')} ${UI_TEXT.trendCompare}`
  }
  return `${dayjs().format('MM-DD')} ${UI_TEXT.trendCompare}`
})

const dailyTrend = computed(() => {
  const sortedList = sortedRecords.value
  const latestRecord = sortedList.length > 0 ? sortedList[sortedList.length - 1] : undefined
  if (!latestRecord?.create_time) {
    return {
      totalCount: 0,
      diffCount: 0,
      consistencyRate: 0,
    }
  }

  const currentDay = dayjs(latestRecord.create_time)
  const previousDay = currentDay.subtract(1, 'day')

  const currentSummary = summarizeByDay(sortedList, currentDay)
  const previousSummary = summarizeByDay(sortedList, previousDay)

  return {
    totalCount: currentSummary.totalCount - previousSummary.totalCount,
    diffCount: currentSummary.diffCount - previousSummary.diffCount,
    consistencyRate: currentSummary.consistencyRate - previousSummary.consistencyRate,
  }
})

function summarizeByDay(recordList: DiffRecord[], day: dayjs.Dayjs): DailySummary {
  const targetDate = day.format('YYYY-MM-DD')
  const dayRecords = recordList.filter((record) => {
    if (!record.create_time) {
      return false
    }
    return dayjs(record.create_time).format('YYYY-MM-DD') === targetDate
  })

  const totalCount = dayRecords.length
  const diffCount = dayRecords.filter((record) => record.has_diff).length
  const consistencyRate = totalCount === 0 ? 0 : (totalCount - diffCount) / totalCount

  return {
    totalCount,
    diffCount,
    consistencyRate,
  }
}

function formatTrend(delta: number, format: 'count' | 'percent', contextLabel: string): string {
  const arrow = delta >= 0 ? '↑' : '↓'
  const absolute = Math.abs(delta)
  if (format === 'percent') {
    return `${arrow}${(absolute * 100).toFixed(2)}% ${contextLabel}`
  }
  return `${arrow}${Math.round(absolute)} ${contextLabel}`
}

function getTrendClass(delta: number): string {
  if (delta > 0) {
    return 'trend-up'
  }
  if (delta < 0) {
    return 'trend-down'
  }
  return 'trend-flat'
}

async function handleMigrationStatusChange(): Promise<void> {
  const matched = filteredTaskOptions.value.some((item) => item.value === filterForm.migration_key)
  if (filterForm.migration_key && !matched) {
    filterForm.migration_key = ''
    statistics.value = null
    records.value = []
    await nextTick()
    renderCharts()
    return
  }

  await loadDashboard()
}

function buildDateRangeParams(): { start_date?: string; end_date?: string } {
  if (filterForm.date_range.length === 2) {
    return {
      start_date: filterForm.date_range[0],
      end_date: filterForm.date_range[1],
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
  const sortedList = sortedRecords.value
  const xAxis = sortedList.map((item) => dayjs(item.create_time).format('MM-DD HH:mm'))

  const totalSeries: number[] = []
  const diffSeries: number[] = []
  const consistencyRateSeries: number[] = []

  let totalCount = 0
  let diffCount = 0

  sortedList.forEach((item) => {
    totalCount += 1
    if (item.has_diff) {
      diffCount += 1
    }

    totalSeries.push(totalCount)
    diffSeries.push(diffCount)
    consistencyRateSeries.push(Number((((totalCount - diffCount) / totalCount) * 100).toFixed(2)))
  })

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
    },
    legend: {
      data: [UI_TEXT.totalLabel, UI_TEXT.diffLabel, UI_TEXT.consistencyLabel],
      top: 0,
    },
    grid: {
      left: 40,
      right: 60,
      top: 45,
      bottom: 35,
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      axisLabel: { color: '#909399' },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
    },
    yAxis: [
      {
        type: 'value',
        name: UI_TEXT.countAxis,
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f2f5' } },
      },
      {
        type: 'value',
        name: UI_TEXT.consistencyAxis,
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}%',
          color: '#909399',
        },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: UI_TEXT.totalLabel,
        type: 'bar',
        data: totalSeries,
        barMaxWidth: 26,
        itemStyle: {
          color: 'rgba(64, 158, 255, 0.42)',
          borderRadius: [4, 4, 0, 0],
        },
      },
      {
        name: UI_TEXT.diffLabel,
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: diffSeries,
        lineStyle: {
          color: '#f56c6c',
          width: 3,
        },
      },
      {
        name: UI_TEXT.consistencyLabel,
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: consistencyRateSeries,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#67c23a',
          width: 4,
        },
        itemStyle: {
          color: '#67c23a',
          borderColor: '#ffffff',
          borderWidth: 2,
        },
      },
    ],
  }
}

function buildCostOption(): echarts.EChartsOption {
  const sortedList = sortedRecords.value
  const xAxis = sortedList.map((item) => dayjs(item.create_time).format('MM-DD HH:mm'))

  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [UI_TEXT.oldCostLegend, UI_TEXT.newCostLegend],
      top: 0,
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      axisLabel: { color: '#909399' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#909399' },
    },
    series: [
      {
        name: UI_TEXT.oldCostLegend,
        type: 'line',
        smooth: true,
        data: sortedList.map((item) => item.old_cost_time_ms ?? 0),
        lineStyle: { color: '#909399' },
      },
      {
        name: UI_TEXT.newCostLegend,
        type: 'line',
        smooth: true,
        data: sortedList.map((item) => item.new_cost_time_ms ?? 0),
        lineStyle: { color: '#409eff' },
      },
    ],
  }
}

function renderCharts(): void {
  initCharts()
  if (!diffStatisticsChart || !costChart) {
    return
  }
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
    records.value = []
    await nextTick()
    renderCharts()
    return
  }

  loading.value = true
  try {
    const dateRangeParams = buildDateRangeParams()
    const [statisticsResult, listResult] = await Promise.all([
      getDiffStatistics({
        migration_key: filterForm.migration_key,
        ...dateRangeParams,
      }),
      getDiffRecordList({
        migration_key: filterForm.migration_key,
        page: 1,
        pageSize: 100,
        ...dateRangeParams,
      }),
    ])
    statistics.value = statisticsResult
    records.value = listResult.list
      .map((item) => ({
        ...item,
        diff_results: item.diff_results ?? [],
      }))
      .sort((a, b) => dayjs(a.create_time).valueOf() - dayjs(b.create_time).valueOf())
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

async function handleRefresh(): Promise<void> {
  await loadDashboard()
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
        style="width: 280px"
        @change="loadDashboard"
      >
        <el-option
          v-for="item in filteredTaskOptions"
          :key="item.value"
          :label="item.value"
          :value="item.value"
        />
      </el-select>

      <el-select
        v-model="filterForm.migration_status"
        clearable
        :placeholder="UI_TEXT.statusPlaceholder"
        style="width: 220px"
        @change="handleMigrationStatusChange"
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
        type="daterange"
        value-format="YYYY-MM-DD"
        :start-placeholder="UI_TEXT.startDatePlaceholder"
        :end-placeholder="UI_TEXT.endDatePlaceholder"
        :range-separator="UI_TEXT.rangeSeparator"
        style="width: 260px"
      />

      <el-button type="primary" @click="loadDashboard">{{ UI_TEXT.filterButton }}</el-button>
      <el-button :icon="Refresh" @click="handleRefresh">{{ UI_TEXT.refreshButton }}</el-button>
    </div>

    <el-row :gutter="12" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.totalLabel }}</span>
          <strong class="stat-value">{{ statistics?.total_count ?? 0 }}</strong>
          <small class="stat-trend" :class="getTrendClass(dailyTrend.totalCount)">
            {{ formatTrend(dailyTrend.totalCount, 'count', trendContextLabel) }}
          </small>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.diffLabel }}</span>
          <strong class="stat-value danger">{{ statistics?.diff_count ?? 0 }}</strong>
          <small class="stat-trend" :class="getTrendClass(dailyTrend.diffCount)">
            {{ formatTrend(dailyTrend.diffCount, 'count', trendContextLabel) }}
          </small>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <span class="stat-label">{{ UI_TEXT.consistencyLabel }}</span>
          <strong class="stat-value success">{{ consistencyRateDisplay }}</strong>
          <small class="stat-trend" :class="getTrendClass(dailyTrend.consistencyRate)">
            {{ formatTrend(dailyTrend.consistencyRate, 'percent', trendContextLabel) }}
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
