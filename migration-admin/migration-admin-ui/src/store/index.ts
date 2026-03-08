import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMigrationTaskStatusMeta } from '@/constants'
import { getMigrationTaskList } from '@/api/migrationTask'
import type { TaskOption } from '@/types'

const TASK_PAGE_SIZE = 200

export const useMigrationTaskStore = defineStore('migration-task', () => {
  const taskOptions = ref<TaskOption[]>([])
  const loading = ref(false)

  async function fetchTaskOptions(force = false): Promise<TaskOption[]> {
    if (!force && taskOptions.value.length > 0) {
      return taskOptions.value
    }

    loading.value = true
    try {
      const pageResult = await getMigrationTaskList({
        page: 1,
        pageSize: TASK_PAGE_SIZE,
      })
      taskOptions.value = pageResult.list.map((task) => ({
        value: task.migration_key,
        label: `${task.migration_key}（${getMigrationTaskStatusMeta(task.status).label}）`,
        status: task.status,
      }))
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
