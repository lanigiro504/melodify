<script setup lang="ts">
/**
 * 主布局：产品化顶栏 + WebSocket 站内通知 + 个人中心独立页入口。
 */
import { storeToRefs } from 'pinia'
import { computed, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import NotificationBell from '@/components/layout/NotificationBell.vue'
import MelodifyPlayer from '@/components/MelodifyPlayer.vue'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { useRealtimeNotificationStore } from '@/stores/realtimeNotifications'
import { avatarDisplayUrl } from '@/utils/avatarDisplayUrl'

defineOptions({ name: 'AppShell' })

const brandLogoSrc = `${import.meta.env.BASE_URL}melodify.png`

const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeNotificationStore()
const { current: playingTrack } = storeToRefs(usePlayerStore())
const { currentUser, isAuthenticated, displayName, avatarText } = storeToRefs(auth)

const headerAvatarSrc = computed(() => avatarDisplayUrl(currentUser.value?.avatar))

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
  <div class="layout-default" :class="{ 'layout-default--playing': !!playingTrack }">
    <a href="#melodify-main" class="melodify-skip-link">跳到主内容</a>
    <header class="app-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/">
          <img
            class="brand-mark"
            :src="brandLogoSrc"
            width="40"
            height="40"
            alt=""
            decoding="async"
            fetchpriority="high"
          />
          <span class="brand-text">
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
        </nav>

        <div class="header-actions">
          <template v-if="isAuthenticated">
            <NotificationBell />
            <button type="button" class="user-pill" @click="goProfile">
              <span class="avatar" :style="headerAvatarSrc ? { backgroundImage: `url(${headerAvatarSrc})` } : {}">
                <span v-if="!headerAvatarSrc">{{ avatarText }}</span>
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

    <main id="melodify-main" class="app-main" tabindex="-1">
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

/* 播放器 dock 占位，避免主内容与底栏视觉上「叠在同一层」难以阅读 */
.layout-default--playing .app-main {
  padding-bottom: 7rem;
}

@media (max-width: 520px) {
  .layout-default--playing .app-main {
    padding-bottom: 8.75rem;
  }
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  padding: 0.75rem clamp(1rem, 4vw, 2rem);
  background: var(--melodify-app-header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--melodify-divider-strong);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8) inset;
  isolation: isolate;
}

.header-inner {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem 1.25rem;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  color: var(--melodify-strong);
  border-radius: var(--melodify-radius-sm);
  transition: opacity 0.18s ease;
}

.brand:hover {
  opacity: 0.94;
}

.brand:hover .brand-mark {
  transform: scale(1.02);
}

.brand-mark {
  flex-shrink: 0;
  width: 2.5rem;
  height: 2.5rem;
  display: block;
  border-radius: var(--melodify-radius-sm);
  object-fit: contain;
  background: transparent;
  transition: transform 0.2s ease;
}

.brand-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.brand-name,
.brand-subtitle,
.user-name,
.user-points {
  display: block;
}

.brand-name {
  font-size: 1.065rem;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  margin-top: -0.02rem;
  font-size: 0.65rem;
  color: var(--melodify-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-weight: 500;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.2rem 0.15rem;
}

.nav-link {
  position: relative;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--melodify-muted);
  padding: 0.42rem 0.72rem;
  border-radius: var(--melodify-radius-sm);
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.nav-link:hover {
  color: var(--melodify-strong);
  background: rgba(var(--melodify-primary-rgb), 0.06);
}

.nav-link.router-link-active {
  color: var(--melodify-strong);
  font-weight: 600;
  background: transparent;
  box-shadow: none;
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  left: 0.55rem;
  right: 0.55rem;
  bottom: 0.08rem;
  height: 2px;
  border-radius: 999px;
  background: var(--el-color-primary);
  opacity: 1;
}

@media (prefers-reduced-motion: reduce) {
  .nav-link.router-link-active::after {
    transition: none;
  }

  .brand:hover .brand-mark {
    transform: none;
  }

  .user-pill:active {
    transform: none;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.ghost-action,
.primary-action,
.user-pill {
  border-radius: var(--melodify-radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
}

.ghost-action {
  font-weight: 500;
  color: var(--melodify-strong);
  padding: 0.45rem 0.72rem;
  transition: background 0.18s ease;
}

.ghost-action:hover {
  background: rgba(0, 0, 0, 0.04);
}

.ghost-action.router-link-active {
  color: var(--melodify-strong);
  background: rgba(var(--melodify-primary-rgb), 0.07);
  border-radius: var(--melodify-radius-sm);
}

.primary-action {
  color: #fff;
  padding: 0.48rem 0.95rem;
  background: var(--el-color-primary);
  border: 1px solid transparent;
  box-shadow: none;
  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.primary-action:hover {
  transform: translateY(-1px);
  background-color: color-mix(in srgb, var(--el-color-primary) 88%, black);
}

.primary-action.router-link-active {
  background-color: var(--el-color-primary);
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-card-solid);
  color: var(--melodify-strong);
  padding: 0.32rem 0.7rem 0.32rem 0.38rem;
  cursor: pointer;
  font-family: inherit;
  box-shadow: none;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease;
}

.user-pill:hover {
  border-color: rgba(var(--melodify-primary-rgb), 0.25);
}

.user-pill:active {
  transform: scale(0.985);
}

.user-pill:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

.avatar {
  width: 2.05rem;
  height: 2.05rem;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 0.73rem;
  background-size: cover;
  background-position: center;
  background-color: var(--melodify-surface-muted);
  color: var(--melodify-strong);
  font-weight: 600;
  outline: 2px solid var(--melodify-card-solid);
  box-shadow: 0 0 0 1px var(--melodify-divider-strong);
}

.user-pill:hover .avatar {
  box-shadow: 0 0 0 1px rgba(var(--melodify-primary-rgb), 0.22);
}

.user-name {
  text-align: left;
  line-height: 1.15;
  font-weight: 600;
  font-size: 0.875rem;
}

.user-points {
  margin-top: 0.08rem;
  font-size: 0.684rem;
  color: var(--melodify-muted);
  text-align: left;
  font-weight: 500;
}

.app-main {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: min(1180px, 100%);
  margin: 0 auto;
  padding: 2.35rem clamp(1rem, 4vw, 2rem) 3.75rem;
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
