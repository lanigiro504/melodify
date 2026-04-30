<script setup lang="ts">
/**
 * 主布局：顶栏导航 + 内容区。
 * 认证状态来自 Pinia，与登录/注册全屏页（App.vue 中 blank 布局）分离。
 */
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'AppShell' })

const auth = useAuthStore()
const { isAuthenticated, displayName } = storeToRefs(auth)

/** 调用 store 清除会话并刷新顶栏状态 */
const onLogout = () => {
  auth.logout()
}
</script>

<template>
  <div class="layout-default">
    <header class="app-header">
      <RouterLink class="brand" to="/">Melodify</RouterLink>
      <nav class="app-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink to="/about" class="nav-link">关于</RouterLink>
        <template v-if="isAuthenticated">
          <span class="nav-user">{{ displayName }}</span>
          <button type="button" class="nav-logout" @click="onLogout">退出</button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="nav-link">登录</RouterLink>
          <RouterLink to="/register" class="nav-link nav-link--emphasis">注册</RouterLink>
        </template>
      </nav>
    </header>
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-default {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 滚动时顶栏保持可见，背景轻微磨砂避免遮住正文 */
.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.75rem clamp(1rem, 4vw, 1.5rem);
  border-bottom: 1px solid var(--color-border);
  background: var(--melodify-app-header-bg);
  backdrop-filter: saturate(1.2) blur(10px);
}

.brand {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-heading);
}

.brand:hover {
  color: var(--el-color-primary);
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem 1rem;
}

.nav-link {
  font-size: 0.9375rem;
  color: var(--color-text);
  padding: 0.35rem 0;
}

.nav-link:hover {
  color: var(--el-color-primary);
}

/* 「注册」链也用 router-link-active，避免与高亮样式叠加得过粗 */
.nav-link.router-link-active:not(.nav-link--emphasis) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.nav-link--emphasis {
  color: var(--el-color-primary);
  font-weight: 600;
}

.nav-user {
  font-size: 0.9375rem;
  color: var(--color-text);
}

.nav-logout {
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  font-size: 0.9375rem;
  padding: 0.35rem 0;
  font-family: inherit;
}

.nav-logout:hover {
  text-decoration: underline;
}

.app-main {
  flex: 1;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}
</style>
