import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMigrationTaskStatusMeta } from '@/constants'
import { getMigrationTaskAll } from '@/api/migrationTask'
import type { TaskOption } from '@/types'

const CACHE_TTL = 30000 // 30 seconds

export const useMigrationTaskStore = defineStore('migration-task', () => {
  const taskOptions = ref<TaskOption[]>([])
  const loading = ref(false)
  let lastFetchTime = 0

  async function fetchTaskOptions(force = false): Promise<TaskOption[]> {
    const now = Date.now()
    if (!force && taskOptions.value.length > 0 && now - lastFetchTime < CACHE_TTL) {
      return taskOptions.value
    }

    loading.value = true
    try {
      const list = await getMigrationTaskAll()
      taskOptions.value = list.map((task) => ({
        value: task.migration_key,
        label: `${task.migration_key}（${getMigrationTaskStatusMeta(task.status).label}）`,
        status: task.status,
      }))
      lastFetchTime = Date.now()
      return taskOptions.value
    } finally {
      loading.value = false
    }
  }

  return {
    taskOptions,
    loading,
    fetchTaskOptions,
  }
})

export { useAuthStore } from '@/store/auth'
