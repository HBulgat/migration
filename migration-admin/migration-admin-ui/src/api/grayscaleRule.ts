import { get, post } from '@/api/http'
import type { GrayscaleRule, PageResult } from '@/types'

const RULE_BASE_PATH = '/api/v1/grayscale_rule'

export interface GrayscaleRuleListParams {
  migration_key: string
  page: number
  pageSize: number
}

export interface CreateGrayscaleRulePayload {
  migration_key: string
  rule_type: string
  rule_value: string
  enable: boolean
  weight: number
}


export interface UpdateGrayscaleRulePayload {
  migration_key: string
  rule_id: string
  rule_type?: string
  rule_value?: string
  enable?: boolean
  weight?: number
}

export interface UpdateGrayscaleRuleEnablePayload {
  migration_key: string
  rule_id: string
  enable: boolean
}

export interface DeleteGrayscaleRulePayload {
  migration_key: string
  rule_id: string
}

export const getGrayscaleRuleList = (params: GrayscaleRuleListParams): Promise<PageResult<GrayscaleRule>> =>
  get<PageResult<GrayscaleRule>>(`${RULE_BASE_PATH}/list`, {
    params: {
      migration_key: params.migration_key,
      page: params.page,
      page_size: params.pageSize,
    },
  })

export const createGrayscaleRule = (data: CreateGrayscaleRulePayload): Promise<GrayscaleRule> =>
  post<GrayscaleRule>(`${RULE_BASE_PATH}/create`, data)

export const updateGrayscaleRule = (data: UpdateGrayscaleRulePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/update`, data)

export const updateGrayscaleRuleEnable = (data: UpdateGrayscaleRuleEnablePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/update_enable`, data)

export const deleteGrayscaleRule = (data: DeleteGrayscaleRulePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/delete`, data)
