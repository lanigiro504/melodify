<script setup lang="ts">
/**
 * 主布局：产品化顶栏 + WebSocket 站内通知 + 个人中心独立页入口。
 */
import { storeToRefs } from 'pinia'
import { watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import NotificationBell from '@/components/layout/NotificationBell.vue'
import MelodifyPlayer from '@/components/MelodifyPlayer.vue'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeNotificationStore } from '@/stores/realtimeNotifications'

defineOptions({ name: 'AppShell' })

const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeNotificationStore()
const { currentUser, isAuthenticated, displayName, avatarText } = storeToRefs(auth)

watch(
  () => auth.isAuthenticated,
  (ok) => {
    if (ok) {
      realtime.connect()
    } else {
      realtime.disconnect()
    }
  },
  { immediate: true },
)

const goProfile = async () => {
  await router.push('/profile')
}

const onLogout = () => {
  auth.logout()
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
          <RouterLink v-if="isAuthenticated" to="/profile" class="nav-link">个人中心</RouterLink>
          <RouterLink to="/about" class="nav-link">关于</RouterLink>
        </nav>

        <div class="header-actions">
          <template v-if="isAuthenticated">
            <NotificationBell />
            <button type="button" class="user-pill" @click="goProfile">
              <span class="avatar" :style="currentUser?.avatar ? { backgroundImage: `url(${currentUser.avatar})` } : {}">
                <span v-if="!currentUser?.avatar">{{ avatarText }}</span>
              </span>
              <span class="user-meta">
                <span class="user-name">{{ displayName }}</span>
                <span class="user-points">{{ currentUser?.points ?? 0 }} 积分</span>
              </span>
            </button>
            <el-button size="small" round plain @click="onLogout">退出</el-button>
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
  width: 2.25rem;
  height: 2.25rem;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  font-size: 0.95rem;
  background: #5b52f2;
}

.brand-name,
.brand-subtitle,
.user-name,
.user-points {
  display: block;
}

.brand-name {
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  margin-top: -0.06rem;
  font-size: 0.6875rem;
  color: var(--melodify-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  font-weight: 500;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.15rem 0.25rem;
}

.nav-link {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--melodify-muted);
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  transition:
    color 0.12s ease,
    background 0.12s ease;
}

.nav-link:hover {
  color: var(--melodify-strong);
  background: #f3f4f6;
}

.nav-link.router-link-active {
  color: var(--melodify-strong);
  font-weight: 600;
  background: transparent;
  box-shadow: none;
  position: relative;
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  left: 0.65rem;
  right: 0.65rem;
  bottom: 0.1rem;
  height: 2px;
  border-radius: 1px;
  background: var(--el-color-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.ghost-action,
.primary-action,
.user-pill {
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
}

.ghost-action {
  color: var(--melodify-strong);
  padding: 0.45rem 0.65rem;
}

.ghost-action.router-link-active {
  color: var(--melodify-strong);
  background: #f3f4f6;
  border-radius: 8px;
}

.primary-action {
  color: #fff;
  padding: 0.45rem 0.85rem;
  background: #5b52f2;
  box-shadow: none;
}

.primary-action.router-link-active {
  box-shadow: none;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  border: 1px solid var(--melodify-border, #e5e7eb);
  background: #ffffff;
  color: var(--melodify-strong);
  padding: 0.3rem 0.65rem 0.3rem 0.35rem;
  cursor: pointer;
  font-family: inherit;
  box-shadow: none;
}

.avatar {
  background-size: cover;
  background-position: center;
  background-color: #d1d5db;
  color: #374151;
  font-weight: 600;
}

.avatar {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 0.75rem;
}

.user-name {
  text-align: left;
  line-height: 1.15;
  font-weight: 500;
  font-size: 0.875rem;
}

.user-points {
  margin-top: 0.08rem;
  font-size: 0.6875rem;
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
