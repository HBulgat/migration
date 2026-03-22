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
  Bell,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store'
import logoImg from '@/assets/logo.png'

interface MenuItem {
  path: string
  title: string
  icon: object
  children?: MenuItem[]
}

type UserCommand = 'profile' | 'logout'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

const menuItems: MenuItem[] = [
  { path: '/introduction', title: '系统简介', icon: Document },
  {
    path: '/migration-manage',
    title: '迁移管理',
    icon: List,
    children: [
      { path: '/migration-task', title: '迁移任务', icon: List },
      { path: '/gray-rule', title: '灰度规则', icon: Operation },
    ],
  },
  {
    path: '/diff-manage',
    title: 'Diff管理',
    icon: DataAnalysis,
    children: [
      { path: '/diff-dashboard', title: 'Diff大盘', icon: DataAnalysis },
      { path: '/diff-rule', title: 'Diff规则', icon: Operation },
      { path: '/diff-record', title: 'Diff记录', icon: Tickets },
    ],
  },
  {
    path: '/alert-manage',
    title: '告警管理',
    icon: Bell,
    children: [
      { path: '/alert-rule', title: '告警规则', icon: Bell },
      { path: '/alert-template', title: '告警模板', icon: Document },
    ],
  },
]

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const result: string[] = []
  for (const item of menuItems) {
    if (item.path === route.path) {
      result.push(item.title)
      break
    }
    if (item.children) {
      const child = item.children.find((c) => c.path === route.path)
      if (child) {
        result.push(item.title)
        result.push(child.title)
        break
      }
    }
  }
  return result
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
      <div class="sidebar-logo">
        <img :src="logoImg" class="logo-image" />
        <span v-if="!isCollapse" class="logo-text">MIGRATION</span>
      </div>

      <el-menu
        class="side-menu"
        :collapse="isCollapse"
        :default-active="activeMenu"
        router
      >
        <template v-for="item in menuItems" :key="item.path">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon>
                <component :is="child.icon" />
              </el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 无子菜单 -->
          <el-menu-item v-else :index="item.path">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
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
  border-right: none;
}

.side-menu {
  border-right: none;
  background: transparent;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.logo-image {
  width: 42px;
  height: 42px;
  object-fit: contain;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* 统一深色主题样式 */
:deep(.el-menu) {
  border-right: none;
  background-color: transparent !important;
}

:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-sub-menu__title:hover) {
  background-color: transparent !important;
  color: #fff !important;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-menu-item:hover) {
  background-color: transparent !important;
  color: #fff !important;
}

/* 子菜单背景颜色 */
:deep(.el-menu--inline) {
  background-color: #000c17 !important;
}

/* 激活状态 */
:deep(.el-menu-item.is-active) {
  color: #fff !important;
  background-color: #1890ff !important;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff !important;
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
