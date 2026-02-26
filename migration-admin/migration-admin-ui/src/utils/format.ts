import dayjs from 'dayjs'

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  if (!dayjs(value).isValid()) {
    return value
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatCost(value?: number | null): string {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${value} ms`
}

export function parseJsonSafely(value: unknown): unknown {
  if (typeof value !== 'string') {
    return value
  }
  const text = value.trim()
  if (!text) {
    return ''
  }
  try {
    return JSON.parse(text)
  } catch {
    return value
  }
}
