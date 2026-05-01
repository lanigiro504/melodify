<script setup lang="ts">
/**
 * 登录页：表单校验 → authStore.login → 跳转 redirect 或首页。
 */
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/views/auth/AuthLayout.vue'
import { loginFormRules } from '@/constants/authFormRules'
import { useAuthStore } from '@/stores/auth'
import { safeInternalPath } from '@/utils/redirect'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'

defineOptions({ name: 'LoginPage' })

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = loginFormRules

/** 提交登录：校验通过后调用 store，成功则按 query.redirect 站内跳转 */
const onSubmit = async () => {
  if (!(await validateFormRef(formRef))) return
  submitting.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    await router.push(safeInternalPath(route.query.redirect))
  } catch (e) {
    showSubmitError(e, '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <template #eyebrow>Account</template>
    <template #title>登录</template>
    <template #subtitle>继续你的音乐创作之旅</template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="username" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="current-password"
          clearable
          @keyup.enter="onSubmit"
        />
      </el-form-item>
      <el-form-item class="auth-card__actions">
        <el-button type="primary" class="auth-card__submit" :loading="submitting" @click="onSubmit">
          登录
        </el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      还没有账号？
      <RouterLink class="auth-card__link" :to="{ path: '/register', query: route.query }">去注册</RouterLink>
    </template>
  </AuthLayout>
</template>
