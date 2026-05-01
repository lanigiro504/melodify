<script setup lang="ts">
/**
 * 注册页：表单校验 → authStore.register → 跳转登录（保留 query 便于 redirect 连贯）。
 */
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/views/auth/AuthLayout.vue'
import { registerFormRules } from '@/constants/authFormRules'
import { useAuthStore } from '@/stores/auth'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'

defineOptions({ name: 'RegisterPage' })

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = registerFormRules

/** 提交注册：成功后提示并进入登录页（不自动写入登录态） */
const onSubmit = async () => {
  if (!(await validateFormRef(formRef))) return
  submitting.value = true
  try {
    await auth.register({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('注册成功')
    await router.push({ path: '/login', query: route.query })
  } catch (e) {
    showSubmitError(e, '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <template #eyebrow>Account</template>
    <template #title>注册</template>
    <template #subtitle>创建账号，开始生成音乐</template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="username" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="new-password"
          clearable
          @keyup.enter="onSubmit"
        />
      </el-form-item>
      <el-form-item class="auth-card__actions">
        <el-button type="primary" class="auth-card__submit" :loading="submitting" @click="onSubmit">
          注册
        </el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      已有账号？
      <RouterLink class="auth-card__link" :to="{ path: '/login', query: route.query }">去登录</RouterLink>
    </template>
  </AuthLayout>
</template>
