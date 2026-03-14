<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store'
import logoImg from '@/assets/logo.png'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formModel = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const redirectPath = computed(() => {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return '/introduction'
})

async function handleLogin(): Promise<void> {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    await authStore.signIn({
      username: formModel.username.trim(),
      password: formModel.password,
    })
    ElMessage.success('登录成功')
    await router.replace(redirectPath.value)
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <div class="login-header">
        <img :src="logoImg" class="login-logo" />
        <h1>后端接口迁移平台</h1>
        <p>Admin Console</p>
      </div>

      <el-form ref="formRef" :model="formModel" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formModel.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="formModel.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-button type="primary" class="login-btn" :loading="submitLoading" @click="handleLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3fb 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-header h1 {
  margin: 12px 0 0;
  font-size: 24px;
  color: #1f2d3d;
}

.login-logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.login-header p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
