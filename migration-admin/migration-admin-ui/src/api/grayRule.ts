import { get, post } from '@/api/http'
import type { GrayRule, PageResult } from '@/types'

const RULE_BASE_PATH = '/api/v1/gray_rule'

export interface GrayRuleListParams {
  migration_key: string
  page: number
  pageSize: number
}

export interface CreateGrayRulePayload {
  migration_key: string
  rule_type: string
  rule_value: string
  enable: boolean
}


export interface UpdateGrayRulePayload {
  migration_key: string
  rule_id: string
  rule_type?: string
  rule_value?: string
  enable?: boolean
}

export interface UpdateGrayRuleEnablePayload {
  migration_key: string
  rule_id: string
  enable: boolean
}

export interface DeleteGrayRulePayload {
  migration_key: string
  rule_id: string
}

export const getGrayRuleList = (params: GrayRuleListParams): Promise<PageResult<GrayRule>> =>
  get<PageResult<GrayRule>>(`${RULE_BASE_PATH}/list`, {
    params: {
      migration_key: params.migration_key,
      page: params.page,
      page_size: params.pageSize,
    },
  })

export const createGrayRule = (data: CreateGrayRulePayload): Promise<GrayRule> =>
  post<GrayRule>(`${RULE_BASE_PATH}/create`, data)

export const updateGrayRule = (data: UpdateGrayRulePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/update`, data)

export const updateGrayRuleEnable = (data: UpdateGrayRuleEnablePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/update_enable`, data)

export const deleteGrayRule = (data: DeleteGrayRulePayload): Promise<void> =>
  post<void>(`${RULE_BASE_PATH}/delete`, data)
