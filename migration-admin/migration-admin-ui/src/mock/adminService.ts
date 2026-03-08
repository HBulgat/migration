import dayjs from 'dayjs'
import type { DiffItem, DiffRecord, DiffStatistics, GrayscaleRule, MigrationTask, PageResult } from '@/types'

interface MigrationTaskListParams {
  page: number
  pageSize: number
  status?: number
  keyword?: string
}

interface GrayscaleRuleListParams {
  migration_key: string
  page: number
  pageSize: number
}

interface DiffRecordListParams {
  migration_key: string
  has_diff?: number
  start_date?: string
  end_date?: string
  page: number
  pageSize: number
}

interface DiffStatisticsParams {
  migration_key: string
  start_date?: string
  end_date?: string
}

const MOCK_NETWORK_DELAY_MS = 120

let mockTaskStore: MigrationTask[] = [
  {
    migration_key: 'user-getUser-api',
    status: 5,
    description: '用户中心查询接口迁移',
    create_time: '2026-02-18T10:15:00',
    update_time: '2026-02-23T16:32:00',
  },
  {
    migration_key: 'order-query-api',
    status: 4,
    description: '订单查询链路迁移',
    create_time: '2026-02-17T09:20:00',
    update_time: '2026-02-23T14:11:00',
  },
  {
    migration_key: 'payment-confirm-api',
    status: 2,
    description: '支付确认接口灰度验证',
    create_time: '2026-02-16T14:25:00',
    update_time: '2026-02-22T18:20:00',
  },
  {
    migration_key: 'inventory-lock-api',
    status: 7,
    description: '库存锁定接口已全量迁移',
    create_time: '2026-02-10T11:32:00',
    update_time: '2026-02-21T10:04:00',
  },
]

let mockRuleIdSequence = 6
let mockGrayscaleRuleStore: GrayscaleRule[] = [
  {
    rule_id: 'rule-0001',
    migration_key: 'user-getUser-api',
    rule_type: 'PERCENTAGE',
    rule_value: '30',
    enable: true,
    create_time: '2026-02-18T12:00:00',
    update_time: '2026-02-23T09:00:00',
  },
  {
    rule_id: 'rule-0002',
    migration_key: 'user-getUser-api',
    rule_type: 'WHITELIST',
    rule_value: '["1001","1002"]',
    enable: false,
    create_time: '2026-02-18T12:30:00',
    update_time: '2026-02-19T10:00:00',
  },
  {
    rule_id: 'rule-0003',
    migration_key: 'order-query-api',
    rule_type: 'BLACKLIST',
    rule_value: '["9001","9002"]',
    enable: true,
    create_time: '2026-02-17T10:10:00',
    update_time: '2026-02-22T15:30:00',
  },
  {
    rule_id: 'rule-0004',
    migration_key: 'payment-confirm-api',
    rule_type: 'EXPRESSION',
    rule_value: '#userLevel >= 4',
    enable: true,
    create_time: '2026-02-16T15:10:00',
    update_time: '2026-02-21T15:22:00',
  },
  {
    rule_id: 'rule-0005',
    migration_key: 'inventory-lock-api',
    rule_type: 'PERCENTAGE',
    rule_value: '100',
    enable: true,
    create_time: '2026-02-10T11:45:00',
    update_time: '2026-02-20T08:15:00',
  },
]

const mockDiffRecordStore: DiffRecord[] = buildMockDiffRecords()

function buildMockDiffRecords(): DiffRecord[] {
  const taskKeys = mockTaskStore.map((task) => task.migration_key)
  const records: DiffRecord[] = []

  let id = 1
  taskKeys.forEach((migrationKey, taskIndex) => {
    for (let dayOffset = 0; dayOffset < 14; dayOffset += 1) {
      for (let sequence = 0; sequence < 6; sequence += 1) {
        const createTime = dayjs('2026-02-23T21:00:00')
          .subtract(dayOffset, 'day')
          .hour(9 + sequence)
          .minute((sequence * 13 + taskIndex * 7) % 60)
          .second((sequence * 9 + taskIndex * 3) % 60)

        const hasDiff = (id + sequence + dayOffset + taskIndex) % 4 === 0 || sequence === 4
        const payload = createDiffPayload(id, hasDiff)

        const oldCost = 90 + ((id + taskIndex) % 65)
        const newCost = 78 + ((id + taskIndex * 2) % 60)

        records.push({
          id,
          migration_key: migrationKey,
          trace_id: `trace-${migrationKey.replace(/[^a-z0-9]/gi, '')}-${id}`,
          old_response: JSON.stringify(payload.oldResponse, null, 2),
          new_response: JSON.stringify(payload.newResponse, null, 2),
          diff_results: payload.diffItems,
          has_diff: hasDiff,
          diff_type: payload.diffItems[0]?.diff_type,
          grayscale_param: JSON.stringify({ userId: `${1000 + (id % 50)}`, region: id % 2 === 0 ? 'beijing' : 'shanghai' }),
          old_cost_time_ms: oldCost,
          new_cost_time_ms: newCost,
          total_cost_time_ms: oldCost + newCost,
          create_time: createTime.format('YYYY-MM-DDTHH:mm:ss'),
        })

        id += 1
      }
    }
  })

  return records.sort((a, b) => dayjs(b.create_time).valueOf() - dayjs(a.create_time).valueOf())
}

function createDiffPayload(id: number, hasDiff: boolean): {
  oldResponse: Record<string, unknown>
  newResponse: Record<string, unknown>
  diffItems: DiffItem[]
} {
  const basePrice = 88 + (id % 24)
  const oldResponse = {
    code: 0,
    data: {
      name: `用户-${id}`,
      level: (id % 5) + 1,
      price: basePrice,
      orders: [
        {
          id: 100000 + id,
          amount: 1 + (id % 3),
        },
      ],
      tags: ['normal', id % 2 === 0 ? 'vip' : 'new'],
    },
  }

  const newResponse = cloneDeep(oldResponse)
  const diffItems: DiffItem[] = []

  if (!hasDiff) {
    return {
      oldResponse,
      newResponse,
      diffItems,
    }
  }

  const mode = id % 3
  if (mode === 0) {
    ;(newResponse.data as { name: string }).name = `用户-${id}-new`
    diffItems.push({
      field_path: 'data.name',
      old_value: `用户-${id}`,
      new_value: `用户-${id}-new`,
      diff_type: 'MODIFY',
    })
  } else if (mode === 1) {
    ;(newResponse.data as { price: number }).price = Number((basePrice + 0.78).toFixed(2))
    diffItems.push({
      field_path: 'data.price',
      old_value: String(basePrice),
      new_value: String((newResponse.data as { price: number }).price),
      diff_type: 'MODIFY',
    })
  } else {
    const newOrder = {
      id: 200000 + id,
      amount: 2,
    }
    ;(newResponse.data as { orders: Array<Record<string, unknown>> }).orders.push(newOrder)
    diffItems.push({
      field_path: 'data.orders[1]',
      old_value: null,
      new_value: JSON.stringify(newOrder),
      diff_type: 'ADD',
    })
  }

  return {
    oldResponse,
    newResponse,
    diffItems,
  }
}

function cloneDeep<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function nowIso(): string {
  return dayjs().format('YYYY-MM-DDTHH:mm:ss')
}

function ensureStatusRange(status: number): void {
  if (status < 1 || status > 7) {
    throw new Error('状态需在 1-7 之间')
  }
}

function paginate<T>(list: T[], page: number, pageSize: number): PageResult<T> {
  const safePage = Math.max(page, 1)
  const safePageSize = Math.max(pageSize, 1)
  const start = (safePage - 1) * safePageSize
  const end = start + safePageSize

  return {
    current: safePage,
    size: safePageSize,
    total: list.length,
    list: cloneDeep(list.slice(start, end)),
  }
}

function simulateNetwork<T>(resolver: () => T): Promise<T> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        resolve(resolver())
      } catch (error) {
        reject(error)
      }
    }, MOCK_NETWORK_DELAY_MS)
  })
}

function filterDiffRecords(params: Pick<DiffRecordListParams, 'migration_key' | 'has_diff' | 'start_date' | 'end_date'>): DiffRecord[] {
  let list = mockDiffRecordStore.filter((item) => item.migration_key === params.migration_key)

  if (params.has_diff !== undefined) {
    const target = params.has_diff === 1
    list = list.filter((item) => item.has_diff === target)
  }

  if (params.start_date) {
    list = list.filter((item) => dayjs(item.create_time).startOf('day').valueOf() >= dayjs(params.start_date).startOf('day').valueOf())
  }

  if (params.end_date) {
    list = list.filter((item) => dayjs(item.create_time).startOf('day').valueOf() <= dayjs(params.end_date).startOf('day').valueOf())
  }

  return list.sort((a, b) => dayjs(b.create_time).valueOf() - dayjs(a.create_time).valueOf())
}

export function listMigrationTasks(params: MigrationTaskListParams): Promise<PageResult<MigrationTask>> {
  return simulateNetwork(() => {
    let list = [...mockTaskStore]
    if (params.status !== undefined) {
      list = list.filter((item) => item.status === params.status)
    }
    const keyword = params.keyword?.trim().toLowerCase()
    if (keyword) {
      list = list.filter((item) => item.migration_key.toLowerCase().includes(keyword))
    }
    list.sort((a, b) => dayjs(b.update_time).valueOf() - dayjs(a.update_time).valueOf())
    return paginate(list, params.page, params.pageSize)
  })
}

export function createMigrationTask(payload: {
  migration_key: string
  status: number
  description?: string
}): Promise<MigrationTask> {
  return simulateNetwork(() => {
    ensureStatusRange(payload.status)
    if (mockTaskStore.some((item) => item.migration_key === payload.migration_key)) {
      throw new Error('migration_key 已存在')
    }

    const now = nowIso()
    const task: MigrationTask = {
      migration_key: payload.migration_key,
      status: payload.status,
      description: payload.description ?? '',
      create_time: now,
      update_time: now,
    }
    mockTaskStore = [task, ...mockTaskStore]
    return cloneDeep(task)
  })
}

export function updateMigrationTask(payload: {
  migration_key: string
  status?: number
  description?: string
}): Promise<void> {
  return simulateNetwork(() => {
    const target = mockTaskStore.find((item) => item.migration_key === payload.migration_key)
    if (!target) {
      throw new Error('迁移任务不存在')
    }

    if (payload.status !== undefined) {
      ensureStatusRange(payload.status)
      target.status = payload.status
    }
    if (payload.description !== undefined) {
      target.description = payload.description
    }
    target.update_time = nowIso()
  })
}

export function queryMigrationTask(payload: { migration_key: string }): Promise<MigrationTask> {
  return simulateNetwork(() => {
    const target = mockTaskStore.find((item) => item.migration_key === payload.migration_key)
    if (!target) {
      throw new Error('迁移任务不存在')
    }
    return cloneDeep(target)
  })
}

export function deleteMigrationTask(payload: { migration_key: string }): Promise<void> {
  return simulateNetwork(() => {
    mockTaskStore = mockTaskStore.filter((item) => item.migration_key !== payload.migration_key)
    mockGrayscaleRuleStore = mockGrayscaleRuleStore.filter((item) => item.migration_key !== payload.migration_key)
  })
}

export function updateMigrationTaskStatus(payload: {
  migration_key: string
  target_status: number
}): Promise<void> {
  return simulateNetwork(() => {
    ensureStatusRange(payload.target_status)
    const target = mockTaskStore.find((item) => item.migration_key === payload.migration_key)
    if (!target) {
      throw new Error('迁移任务不存在')
    }
    target.status = payload.target_status
    target.update_time = nowIso()
  })
}

export function listGrayscaleRules(params: GrayscaleRuleListParams): Promise<PageResult<GrayscaleRule>> {
  return simulateNetwork(() => {
    const list = mockGrayscaleRuleStore
      .filter((item) => item.migration_key === params.migration_key)
      .sort((a, b) => dayjs(b.update_time).valueOf() - dayjs(a.update_time).valueOf())
    return paginate(list, params.page, params.pageSize)
  })
}

export function createGrayscaleRule(payload: {
  migration_key: string
  rule_type: string
  rule_value: string
  enable: boolean
}): Promise<GrayscaleRule> {
  return simulateNetwork(() => {
    const now = nowIso()
    const rule: GrayscaleRule = {
      rule_id: `rule-${String(mockRuleIdSequence).padStart(4, '0')}`,
      migration_key: payload.migration_key,
      rule_type: payload.rule_type,
      rule_value: payload.rule_value,
      enable: payload.enable,
      create_time: now,
      update_time: now,
    }
    mockRuleIdSequence += 1
    mockGrayscaleRuleStore = [rule, ...mockGrayscaleRuleStore]
    return cloneDeep(rule)
  })
}

export function updateGrayscaleRule(payload: {
  migration_key: string
  rule_id: string
  rule_type?: string
  rule_value?: string
  enable?: boolean
}): Promise<void> {
  return simulateNetwork(() => {
    const target = mockGrayscaleRuleStore.find(
      (item) => item.migration_key === payload.migration_key && item.rule_id === payload.rule_id,
    )
    if (!target) {
      throw new Error('灰度规则不存在')
    }

    if (payload.rule_type !== undefined) {
      target.rule_type = payload.rule_type
    }
    if (payload.rule_value !== undefined) {
      target.rule_value = payload.rule_value
    }
    if (payload.enable !== undefined) {
      target.enable = payload.enable
    }
    target.update_time = nowIso()
  })
}

export function updateGrayscaleRuleEnable(payload: {
  migration_key: string
  rule_id: string
  enable: boolean
}): Promise<void> {
  return updateGrayscaleRule(payload)
}

export function deleteGrayscaleRule(payload: {
  migration_key: string
  rule_id: string
}): Promise<void> {
  return simulateNetwork(() => {
    mockGrayscaleRuleStore = mockGrayscaleRuleStore.filter(
      (item) => !(item.migration_key === payload.migration_key && item.rule_id === payload.rule_id),
    )
  })
}

export function listDiffRecords(params: DiffRecordListParams): Promise<PageResult<DiffRecord>> {
  return simulateNetwork(() => {
    const list = filterDiffRecords(params)
    return paginate(list, params.page, params.pageSize)
  })
}

export function detailDiffRecord(id: number): Promise<DiffRecord> {
  return simulateNetwork(() => {
    const target = mockDiffRecordStore.find((item) => item.id === id)
    if (!target) {
      throw new Error('Diff记录不存在')
    }
    return cloneDeep(target)
  })
}

export function statisticsDiffRecord(params: DiffStatisticsParams): Promise<DiffStatistics> {
  return simulateNetwork(() => {
    const list = filterDiffRecords({
      migration_key: params.migration_key,
      start_date: params.start_date,
      end_date: params.end_date,
    })

    const totalCount = list.length
    const diffCount = list.filter((item) => item.has_diff).length
    const diffRate = totalCount === 0 ? 0 : diffCount / totalCount

    const avgOldCostTime =
      totalCount === 0 ? 0 : Math.round(list.reduce((sum, item) => sum + (item.old_cost_time_ms ?? 0), 0) / totalCount)
    const avgNewCostTime =
      totalCount === 0 ? 0 : Math.round(list.reduce((sum, item) => sum + (item.new_cost_time_ms ?? 0), 0) / totalCount)

    return {
      total_count: totalCount,
      diff_count: diffCount,
      diff_rate: diffRate,
      avg_old_cost_time: avgOldCostTime,
      avg_new_cost_time: avgNewCostTime,
    }
  })
}
