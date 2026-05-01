<script setup lang="ts">
/**
 * 主布局：产品化顶栏 + 背景装饰 + 个人资料抽屉。
 */
import type { FormInstance, FormRules } from 'element-plus'
import { storeToRefs } from 'pinia'
import { reactive, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import MelodifyPlayer from '@/components/MelodifyPlayer.vue'
import { useAuthStore } from '@/stores/auth'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'

defineOptions({ name: 'AppShell' })

const router = useRouter()
const auth = useAuthStore()
const { currentUser, isAuthenticated, displayName, avatarText } = storeToRefs(auth)

const profileVisible = ref(false)
const profileRef = ref<FormInstance>()
const savingProfile = ref(false)
const refreshingProfile = ref(false)

const profileForm = reactive({
  nickname: '',
  avatar: '',
  email: '',
  phone: '',
})

const profileRules: FormRules = {
  nickname: [{ max: 50, message: '昵称最多 50 个字符', trigger: 'blur' }],
  avatar: [{ max: 255, message: '头像地址最多 255 个字符', trigger: 'blur' }],
  email: [{ max: 100, message: '邮箱最多 100 个字符', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号最多 20 个字符', trigger: 'blur' }],
}

watch(
  currentUser,
  (user) => {
    profileForm.nickname = user?.nickname ?? ''
    profileForm.avatar = user?.avatar ?? ''
    profileForm.email = user?.email ?? ''
    profileForm.phone = user?.phone ?? ''
  },
  { immediate: true },
)

const openProfile = () => {
  profileVisible.value = true
  void refreshProfile()
}

const refreshProfile = async () => {
  if (!isAuthenticated.value) return
  refreshingProfile.value = true
  try {
    await auth.refreshMe()
  } catch (e) {
    showSubmitError(e, '刷新资料失败')
  } finally {
    refreshingProfile.value = false
  }
}

const saveProfile = async () => {
  if (!(await validateFormRef(profileRef))) return
  savingProfile.value = true
  try {
    await auth.updateProfile({
      nickname: profileForm.nickname.trim(),
      avatar: profileForm.avatar.trim(),
      email: profileForm.email.trim(),
      phone: profileForm.phone.trim(),
    })
    profileVisible.value = false
  } catch (e) {
    showSubmitError(e, '保存资料失败')
  } finally {
    savingProfile.value = false
  }
}

const onLogout = () => {
  auth.logout()
  profileVisible.value = false
  void router.push('/')
}
</script>

<template>
  <div class="layout-default">
    <header class="app-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">M</span>
          <span>
            <span class="brand-name">Melodify</span>
            <span class="brand-subtitle">AI Music Studio</span>
          </span>
        </RouterLink>

        <nav class="app-nav" aria-label="主导航">
          <RouterLink to="/" class="nav-link">首页</RouterLink>
          <RouterLink to="/explore" class="nav-link">广场</RouterLink>
          <RouterLink v-if="isAuthenticated" to="/generate" class="nav-link">创作</RouterLink>
          <RouterLink v-if="isAuthenticated" to="/works" class="nav-link">作品库</RouterLink>
          <RouterLink v-if="isAuthenticated" to="/recharge" class="nav-link">充值</RouterLink>
          <RouterLink to="/about" class="nav-link">关于</RouterLink>
        </nav>

        <div class="header-actions">
          <template v-if="isAuthenticated">
        <button type="button" class="user-pill" :aria-expanded="profileVisible ? 'true' : 'false'" @click="openProfile">
              <span class="avatar" :style="currentUser?.avatar ? { backgroundImage: `url(${currentUser.avatar})` } : {}">
                <span v-if="!currentUser?.avatar">{{ avatarText }}</span>
              </span>
              <span class="user-meta">
                <span class="user-name">{{ displayName }}</span>
                <span class="user-points">{{ currentUser?.points ?? 0 }} 积分</span>
              </span>
            </button>
          </template>
          <template v-else>
            <RouterLink to="/login" class="ghost-action">登录</RouterLink>
            <RouterLink to="/register" class="primary-action">免费注册</RouterLink>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>

    <MelodifyPlayer />

    <el-drawer v-model="profileVisible" title="个人中心" size="380px" append-to-body>
      <div class="profile-panel">
        <div class="profile-card">
          <span class="profile-avatar" :style="currentUser?.avatar ? { backgroundImage: `url(${currentUser.avatar})` } : {}">
            <span v-if="!currentUser?.avatar">{{ avatarText }}</span>
          </span>
          <div>
            <p class="profile-name">{{ displayName || '未命名用户' }}</p>
            <p class="profile-id">用户名：{{ currentUser?.username }}</p>
          </div>
        </div>

        <div class="profile-stats">
          <div>
            <span class="stat-label">可用积分</span>
            <strong>{{ currentUser?.points ?? 0 }}</strong>
          </div>
          <el-button link type="primary" :loading="refreshingProfile" @click="refreshProfile()">
            刷新
          </el-button>
        </div>

        <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-position="top" class="profile-form">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="profileForm.nickname" placeholder="给自己取个创作者昵称" maxlength="50" clearable />
          </el-form-item>
          <el-form-item label="头像地址" prop="avatar">
            <el-input v-model="profileForm.avatar" placeholder="https://..." maxlength="255" clearable />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="用于后续通知能力" maxlength="100" clearable />
          </el-form-item>
          <el-form-item label="手机" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" clearable />
          </el-form-item>
        </el-form>

        <div class="profile-actions">
          <el-button class="profile-save" type="primary" :loading="savingProfile" @click="saveProfile">
            保存资料
          </el-button>
          <el-button class="profile-logout" plain @click="onLogout">退出登录</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.layout-default {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--melodify-page-bg);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  padding: 0.85rem clamp(1rem, 4vw, 2rem);
  background: var(--melodify-app-header-bg);
  backdrop-filter: saturate(1.25) blur(16px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.header-inner {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.85rem 1.25rem;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--melodify-strong);
}

.brand-mark {
  width: 2.65rem;
  height: 2.65rem;
  display: grid;
  place-items: center;
  border-radius: 1rem;
  color: #fff;
  font-weight: 800;
  background: #6d5dfc;
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.16);
}

.brand-name,
.brand-subtitle,
.user-name,
.user-points {
  display: block;
}

.brand-name {
  font-size: 1.08rem;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.brand-subtitle {
  margin-top: -0.12rem;
  font-size: 0.72rem;
  color: var(--melodify-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  background: #f8fafc;
}

.nav-link {
  font-size: 0.9375rem;
  color: var(--melodify-muted);
  padding: 0.48rem 0.9rem;
  border-radius: 999px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.nav-link:hover {
  color: var(--melodify-strong);
  background: rgba(99, 102, 241, 0.08);
}

.nav-link.router-link-active {
  color: #fff;
  font-weight: 700;
  background: #6d5dfc;
  box-shadow: 0 8px 16px rgba(99, 102, 241, 0.14);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.ghost-action,
.primary-action,
.user-pill {
  border-radius: 999px;
  font-size: 0.9375rem;
  font-weight: 700;
}

.ghost-action {
  color: var(--el-color-primary);
  padding: 0.55rem 0.95rem;
}

.ghost-action.router-link-active {
  color: var(--melodify-strong);
  background: rgba(99, 102, 241, 0.12);
  border-radius: 999px;
}

.primary-action {
  color: #fff;
  padding: 0.62rem 1rem;
  background: #6d5dfc;
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.14);
}

.primary-action.router-link-active {
  box-shadow: 0 10px 26px rgba(99, 102, 241, 0.24);
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #ffffff;
  color: var(--melodify-strong);
  padding: 0.35rem 0.85rem 0.35rem 0.35rem;
  cursor: pointer;
  font-family: inherit;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
}

.avatar,
.profile-avatar {
  background-size: cover;
  background-position: center;
  background-color: #6366f1;
  color: #fff;
  font-weight: 800;
}

.avatar {
  width: 2.2rem;
  height: 2.2rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
}

.user-name {
  text-align: left;
  line-height: 1.15;
}

.user-points {
  margin-top: 0.1rem;
  font-size: 0.75rem;
  color: var(--melodify-muted);
  text-align: left;
}

.app-main {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: min(1180px, 100%);
  margin: 0 auto;
  padding: 2rem clamp(1rem, 4vw, 2rem) 3.5rem;
}

.profile-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.profile-card,
.profile-stats {
  border-radius: 1.25rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fafc;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  padding: 1rem;
}

.profile-avatar {
  width: 3.8rem;
  height: 3.8rem;
  display: grid;
  place-items: center;
  flex: none;
  border-radius: 1.3rem;
  font-size: 1.35rem;
}

.profile-name {
  font-size: 1.08rem;
  font-weight: 800;
  color: var(--melodify-strong);
}

.profile-id {
  margin-top: 0.2rem;
  color: var(--melodify-muted);
  font-size: 0.85rem;
}

.profile-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
}

.stat-label {
  display: block;
  font-size: 0.78rem;
  color: var(--melodify-muted);
}

.profile-stats strong {
  font-size: 1.45rem;
  color: var(--melodify-strong);
}

.profile-form {
  margin-top: 0.25rem;
}

.profile-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.profile-save,
.profile-logout {
  width: 100%;
}

@media (max-width: 720px) {
  .header-inner {
    align-items: stretch;
  }

  .app-nav {
    order: 3;
    width: 100%;
    justify-content: space-between;
  }

  .nav-link {
    flex: 1;
    text-align: center;
  }
}
</style>
