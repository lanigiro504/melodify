<script setup lang="ts">
import { Menu as IconMenu, UserFilled, Key, HomeFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const route = useRoute()
const router = useRouter()
const auth = useAdminAuthStore()

const active = computed(() => route.path)

const handleLogout = () => {
  auth.logout()
  void router.push('/login')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="aside">
      <div class="brand">Melodify 后台</div>
      <el-menu :default-active="active" router class="menu" background-color="#1a1a2e" text-color="#cbd5e1">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/roles">
          <el-icon><Key /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="hdr-icon"><IconMenu /></el-icon>
          <span class="hdr-title">运营控制台</span>
        </div>
        <div class="header-right">
          <span class="who">{{ auth.displayName || '管理员' }}</span>
          <el-button type="primary" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
}
.aside {
  background: #1a1a2e;
  color: #e2e8f0;
}
.brand {
  padding: 1.25rem 1rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #fff;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.hdr-icon {
  font-size: 1.25rem;
  color: var(--el-text-color-secondary);
}
.hdr-title {
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.who {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.main {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>
