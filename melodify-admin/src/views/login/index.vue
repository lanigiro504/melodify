<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { showSubmitError } from '@/utils/showSubmitError'

const auth = useAdminAuthStore()
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const onSubmit = async () => {
  const u = form.username.trim()
  const p = form.password
  if (!u || !p) {
    return
  }
  loading.value = true
  try {
    await auth.login(u, p)
    const redir = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redir && redir.startsWith('/') ? redir : '/')
  } catch (e) {
    showSubmitError(e, '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="card">
      <h1>管理控制台</h1>
      <el-form @submit.prevent="onSubmit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" native-type="submit" :loading="loading" block>
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.card {
  width: 100%;
  max-width: 400px;
  padding: 2rem 2rem 2.25rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
}
h1 {
  margin: 0 0 1.25rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
  text-align: center;
}
.submit {
  width: 100%;
  margin-top: 0.5rem;
}
</style>
