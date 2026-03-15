import { get } from '@/api/http'
import type { DiffRecord, DiffStatistics, PageResult } from '@/types'

const DIFF_RECORD_BASE_PATH = '/api/v1/diff_record'

export interface DiffRecordListParams {
  migration_key: string
  has_diff?: number
  migration_status?: number
  trace_id?: string
  start_date?: string
  end_date?: string
  page: number
  pageSize: number
}

export interface DiffStatisticsParams {
  migration_key: string
  migration_status?: number
  start_date?: string
  end_date?: string
  granularity?: 'MINUTE' | 'HOUR' | 'DAY'
}

export const getDiffRecordList = (params: DiffRecordListParams): Promise<PageResult<DiffRecord>> =>
  get<PageResult<DiffRecord>>(`${DIFF_RECORD_BASE_PATH}/list`, {
    params: {
      migration_key: params.migration_key,
      has_diff: params.has_diff,
      migration_status: params.migration_status,
      trace_id: params.trace_id,
      start_date: params.start_date,
      end_date: params.end_date,
      page: params.page,
      page_size: params.pageSize,
    },
  })

export const getDiffRecordDetail = (id: number): Promise<DiffRecord> =>
  get<DiffRecord>(`${DIFF_RECORD_BASE_PATH}/detail`, { params: { id } })

export const getDiffStatistics = (params: DiffStatisticsParams): Promise<DiffStatistics> =>
  get<DiffStatistics>(`${DIFF_RECORD_BASE_PATH}/statistics`, {
    params: {
      migration_key: params.migration_key,
      migration_status: params.migration_status,
      start_date: params.start_date,
      end_date: params.end_date,
      granularity: params.granularity || 'HOUR',
    },
  })
