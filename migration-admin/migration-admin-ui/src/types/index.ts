export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  current: number
  size: number
  total: number
  list: T[]
}

export interface MigrationTask {
  migration_key: string
  status: number
  description?: string | null
  create_time?: string
  update_time?: string
}

export interface GrayscaleRule {
  rule_id: string
  migration_key: string
  rule_type: string
  rule_value: string
  enable: boolean
  weight: number
  create_time?: string
  update_time?: string
}

export interface DiffItem {
  field_path: string
  old_value: string | null
  new_value: string | null
  diff_type: string
}

export interface DiffRecord {
  id: number
  migration_key: string
  trace_id: string
  migration_status: number
  old_response?: string | null
  new_response?: string | null
  diff_results: DiffItem[]
  has_diff: boolean
  diff_type?: string | null
  grayscale_param?: string | null
  old_cost_time_ms?: number | null
  new_cost_time_ms?: number | null
  total_cost_time_ms?: number | null
  create_time?: string
}

export interface DiffStatisticsPoint {
  time_point: string
  total_count: number
  diff_count: number
  diff_rate: number
  avg_old_cost_time: number
  avg_new_cost_time: number
}

export interface DiffStatistics {
  points: DiffStatisticsPoint[]
}

export interface TaskOption {
  label: string
  value: string
  status: number
}

export interface AuthUserInfo {
  username: string
  display_name: string
}

export interface LoginResponse {
  access_token: string
  user_info: AuthUserInfo
}

export interface AlertTemplate {
  template_key: string
  channel: 'FEISHU' | 'EMAIL'
  name: string
  template: Record<string, any>
  create_time: string
  update_time: string
}

export interface CreateAlertTemplateRequest {
  template_key: string
  channel: 'FEISHU' | 'EMAIL'
  name: string
  template: Record<string, any>
}

export interface UpdateAlertTemplateRequest {
  template_key: string
  channel: 'FEISHU' | 'EMAIL'
  name: string
  template: Record<string, any>
}

