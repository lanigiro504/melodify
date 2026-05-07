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

.avatar {
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
