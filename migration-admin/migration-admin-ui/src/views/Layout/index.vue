<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  Document,
  Expand,
  Fold,
  List,
  Operation,
  Tickets,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store'

interface MenuItem {
  path: string
  title: string
  icon: object
}

type UserCommand = 'profile' | 'logout'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

const menuItems: MenuItem[] = [
  { path: '/introduction', title: '系统简介', icon: Document },
  { path: '/diff-dashboard', title: 'Diff大盘', icon: DataAnalysis },
  { path: '/migration-task', title: '迁移任务', icon: List },
  { path: '/grayscale-rule', title: '灰度规则', icon: Operation },
  { path: '/diff-rule', title: 'Diff规则', icon: Operation },
  { path: '/diff-record', title: 'Diff记录', icon: Tickets },
]

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  return route.matched
    .map((item) => item.meta?.title as string | undefined)
    .filter((item): item is string => Boolean(item))
})

const username = computed(() => authStore.displayName)

function toggleAside(): void {
  isCollapse.value = !isCollapse.value
}

async function handleUserCommand(command: UserCommand): Promise<void> {
  if (command === 'profile') {
    ElMessage.info('暂未开放个人设置')
    return
  }

  const success = await authStore.signOut()
  if (success) {
    ElMessage.success('已退出登录')
  } else {
    ElMessage.warning('退出请求失败，已清理本地登录态')
  }
  await router.replace('/login')
}
</script>

<template>
  <el-container class="layout-shell">
    <el-aside class="layout-aside" :width="isCollapse ? '64px' : '220px'">
      <el-menu
        class="side-menu"
        :collapse="isCollapse"
        :default-active="activeMenu"
        router
        unique-opened
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button link class="collapse-btn" @click="toggleAside">
            <el-icon>
              <component :is="isCollapse ? Expand : Fold" />
            </el-icon>
          </el-button>
          <span class="logo-dot" />
          <span class="header-title">后端接口迁移平台</span>
        </div>

        <el-dropdown @command="handleUserCommand">
          <span class="user-area">
            <el-icon><UserFilled /></el-icon>
            <span>{{ username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人设置</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="layout-main">
        <el-breadcrumb class="main-breadcrumb" separator=">">
          <el-breadcrumb-item>首页</el-breadcrumb-item>
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="main-content">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
}

.layout-aside {
  background: #001529;
  transition: width 0.2s ease;
}

.side-menu {
  border-right: none;
  background: transparent;
  padding-top: 8px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  color: rgb(255 255 255 / 78%);
}

:deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgb(24 144 255 / 20%);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-btn {
  color: #303133;
}

.logo-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #409eff;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.user-area {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  cursor: pointer;
}

.layout-main {
  padding: 16px;
}

.main-breadcrumb {
  margin-bottom: 12px;
}

.main-content {
  min-height: calc(100vh - 124px);
}
</style>
