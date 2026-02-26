import { get, post } from '@/api/http'
import type { MigrationTask, PageResult } from '@/types'

const TASK_BASE_PATH = '/api/v1/migration_task'

export interface MigrationTaskListParams {
  page: number
  pageSize: number
  status?: number
  keyword?: string
}

export interface CreateMigrationTaskPayload {
  migration_key: string
  status: number
  description?: string
}

export interface UpdateMigrationTaskPayload {
  migration_key: string
  status?: number
  description?: string
}

export interface QueryMigrationTaskPayload {
  migration_key: string
}

export interface DeleteMigrationTaskPayload {
  migration_key: string
}

export interface UpdateMigrationStatusPayload {
  migration_key: string
  target_status: number
}

export const getMigrationTaskList = (params: MigrationTaskListParams): Promise<PageResult<MigrationTask>> =>
  get<PageResult<MigrationTask>>(`${TASK_BASE_PATH}/list`, {
    params: {
      page: params.page,
      page_size: params.pageSize,
      status: params.status,
      keyword: params.keyword,
    },
  })

export const createMigrationTask = (data: CreateMigrationTaskPayload): Promise<MigrationTask> =>
  post<MigrationTask>(`${TASK_BASE_PATH}/create`, data)

export const updateMigrationTask = (data: UpdateMigrationTaskPayload): Promise<void> =>
  post<void>(`${TASK_BASE_PATH}/update`, data)

export const queryMigrationTask = (data: QueryMigrationTaskPayload): Promise<MigrationTask> =>
  post<MigrationTask>(`${TASK_BASE_PATH}/query`, data)

export const deleteMigrationTask = (data: DeleteMigrationTaskPayload): Promise<void> =>
  post<void>(`${TASK_BASE_PATH}/delete`, data)

export const updateMigrationStatus = (data: UpdateMigrationStatusPayload): Promise<void> =>
  post<void>(`${TASK_BASE_PATH}/update_status`, data)
