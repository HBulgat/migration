import { post } from './http'
import type { 
  AlertTemplate,
  CreateAlertTemplateRequest,
  UpdateAlertTemplateRequest,
  PageResult
} from '@/types'

export const alertTemplateApi = {
  // 获取模板列表
  list(channel?: string) {
    return post<PageResult<AlertTemplate>>(`/api/v1/alert-templates/list${channel ? `?channel=${channel}` : ''}`)
  },

  // 创建模板
  create(data: CreateAlertTemplateRequest) {
    return post<void>('/api/v1/alert-templates/create', data)
  },

  // 更新模板
  update(data: UpdateAlertTemplateRequest) {
    return post<void>('/api/v1/alert-templates/update', data)
  }
}
