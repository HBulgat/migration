import { post } from '@/api/http'

// ==================== 接口定义 ====================

export interface AlertRule {
    migration_key: string
    rule_id: string
    name: string
    channel: string
    template_key: string
    receivers: string[]
    enable: boolean
    create_time: string
    update_time: string
}

export interface CreateAlertRuleRequest {
    migration_key: string
    name: string
    channel: string
    template_key: string
    receivers: string[]
    enable: boolean
}

export interface UpdateAlertRuleRequest {
    migration_key: string
    rule_id: string
    name: string
    channel: string
    template_key: string
    receivers: string[]
    enable: boolean
}

export interface UpdateAlertRuleEnableRequest {
    migration_key: string
    rule_id: string
    enable: boolean
}

export interface DeleteAlertRuleRequest {
    migration_key: string
    rule_id: string
}

export interface CreateAlertRuleResponse {
    rule_id: string
}

// ==================== API 方法 ====================

export const createAlertRule = (data: CreateAlertRuleRequest) => {
    return post<CreateAlertRuleResponse>('/api/v1/alert-rules/create', data)
}

export const updateAlertRule = (data: UpdateAlertRuleRequest) => {
    return post<boolean>('/api/v1/alert-rules/update', data)
}

export const deleteAlertRule = (data: DeleteAlertRuleRequest) => {
    return post<boolean>('/api/v1/alert-rules/delete', data)
}

export const updateAlertRuleEnable = (data: UpdateAlertRuleEnableRequest) => {
    return post<boolean>('/api/v1/alert-rules/update-enable', data)
}

export interface GetAlertRuleListRequest {
  migration_key: string
  channel?: string
}

export const getAlertRuleList = (data: GetAlertRuleListRequest) => {
    return post<AlertRule[]>('/api/v1/alert-rules/list', data)
}
