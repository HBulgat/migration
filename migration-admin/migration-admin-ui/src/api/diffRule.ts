import { get, post } from '@/api/http'
import type { PageResult } from '@/types'

// ==================== 接口定义 ====================

export interface DiffRule {
    migration_key: string
    rule_id: string
    rule_type: string
    field_path: string
    rule_value: string
    enable: boolean
    weight: number
    create_time: string
    update_time: string
}

export interface CreateDiffRuleRequest {
    migration_key: string
    rule_type: string
    field_path: string
    rule_value: string
    enable: boolean
    weight: number
}


export interface UpdateDiffRuleRequest {
    migration_key: string
    rule_id: string
    rule_type: string
    field_path: string
    rule_value: string
    enable: boolean
    weight: number
}

export interface UpdateDiffRuleEnableRequest {
    migration_key: string
    rule_id: string
    enable: boolean
}

export interface DeleteDiffRuleRequest {
    migration_key: string
    rule_id: string
}

// ==================== API 方法 ====================

export const createDiffRule = (data: CreateDiffRuleRequest) => {
    return post<DiffRule>('/api/v1/diff_rule/create', data)
}

export const updateDiffRule = (data: UpdateDiffRuleRequest) => {
    return post<void>('/api/v1/diff_rule/update', data)
}

export const deleteDiffRule = (data: DeleteDiffRuleRequest) => {
    return post<void>('/api/v1/diff_rule/delete', data)
}

export const updateDiffRuleEnable = (data: UpdateDiffRuleEnableRequest) => {
    return post<void>('/api/v1/diff_rule/update_enable', data)
}

export const getDiffRuleList = (params: {
    migration_key: string
    page: number
    page_size: number
}) => {
    return get<PageResult<DiffRule>>('/api/v1/diff_rule/list', { params })
}
