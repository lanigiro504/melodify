<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { safeInternalPath } from '@/utils/safeInternalPath'
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
    await router.replace(safeInternalPath(route.query.redirect))
  } catch (e) {
    showSubmitError(e, '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="admin-login-card">
      <div class="login-head">
        <p class="login-eyebrow">Melodify Admin</p>
        <h1 class="login-title">管理控制台</h1>
        <p class="login-lead">请使用管理员账号登录</p>
      </div>
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
  padding: 2rem 1.25rem;
  background:
    radial-gradient(circle at 18% 12%, rgba(109, 93, 252, 0.22), transparent 42%),
    radial-gradient(circle at 92% 6%, rgba(56, 189, 248, 0.12), transparent 38%),
    linear-gradient(155deg, #12122a 0%, #1a1a32 36%, #14142a 100%);
}

.login-head {
  text-align: center;
  margin-bottom: 1.5rem;
}

.login-eyebrow {
  margin: 0 0 0.45rem;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--el-color-primary);
}

.login-title {
  margin: 0 0 0.35rem;
  font-size: 1.45rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--el-text-color-primary);
}

.login-lead {
  margin: 0;
  font-size: 0.9rem;
  color: var(--el-text-color-secondary);
}

.submit {
  width: 100%;
  margin-top: 0.5rem;
  font-weight: 700;
}
</style>
