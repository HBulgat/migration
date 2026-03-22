export interface OptionItem<T> {
  label: string
  value: T
}

interface StatusMeta {
  label: string
  tagClass: string
}

export const MIGRATION_STATUS_META: Record<number, StatusMeta> = {
  1: { label: '单旧', tagClass: 'status-old' },
  2: { label: '验证-灰度', tagClass: 'status-validation-gray' },
  3: { label: '验证-全开', tagClass: 'status-validation-all' },
  4: { label: '上线-灰度', tagClass: 'status-go-live-gray' },
  5: { label: '上线-全开', tagClass: 'status-go-live-all' },
  6: { label: '停用-灰度', tagClass: 'status-decommissioning-gray' },
  7: { label: '停用-全开', tagClass: 'status-decommissioning-all' },
}

export const MIGRATION_STATUS_OPTIONS: Array<OptionItem<number>> = [
  { label: '单旧', value: 1 },
  { label: '验证-灰度', value: 2 },
  { label: '验证-全开', value: 3 },
  { label: '上线-灰度', value: 4 },
  { label: '上线-全开', value: 5 },
  { label: '停用-灰度', value: 6 },
  { label: '停用-全开', value: 7 },
]

export const GRAY_RULE_TYPE_OPTIONS: Array<OptionItem<string>> = [
  { label: '百分比', value: 'PERCENTAGE' },
  { label: '黑名单', value: 'BLACKLIST' },
  { label: '白名单', value: 'WHITELIST' },
  { label: '表达式', value: 'EXPRESSION' },
]

export const GRAY_RULE_TYPE_DESC: Record<string, string> = {
  PERCENTAGE: '按比例分流到新接口，规则值示例：30',
  BLACKLIST: '指定用户不走新接口，规则值示例：["1001","1002"]',
  WHITELIST: '仅指定用户走新接口，规则值示例：["1001","1002"]',
  EXPRESSION: '自定义表达式，规则值示例：#userId > 10000',
}

export const HAS_DIFF_OPTIONS: Array<OptionItem<number>> = [
  { label: '有差异', value: 1 },
  { label: '无差异', value: 0 },
]

export function getMigrationTaskStatusMeta(status: number): StatusMeta {
  return MIGRATION_STATUS_META[status] ?? { label: '未知状态', tagClass: 'status-old' }
}

export function getGrayRuleTypeLabel(ruleType: string): string {
  const option = GRAY_RULE_TYPE_OPTIONS.find((item) => item.value === ruleType)
  return option?.label ?? ruleType
}

export function getDiffTypeTagType(diffType: string): 'success' | 'danger' | 'warning' | 'info' {
  if (diffType === 'ADD') {
    return 'success'
  }
  if (diffType === 'REMOVE') {
    return 'danger'
  }
  if (diffType === 'MODIFY') {
    return 'warning'
  }
  return 'info'
}

export function getDiffTypeLabel(diffType: string): string {
  if (diffType === 'ADD') {
    return '新增'
  }
  if (diffType === 'REMOVE') {
    return '删除'
  }
  if (diffType === 'MODIFY') {
    return '修改'
  }
  return diffType
}
