<script setup lang="ts">
import {
  Coin,
  Cpu,
  FolderOpened,
  Goods,
  Key,
  Menu as IconMenu,
  Postcard,
  Sell,
  UserFilled,
} from '@element-plus/icons-vue'
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
      <div class="brand">
        <span class="brand-name">Melodify</span>
        <span class="brand-tag">控制台</span>
      </div>
      <el-menu
        :default-active="active"
        :default-openeds="['ops']"
        router
        class="menu"
        background-color="#1a1a2e"
        text-color="#cbd5e1"
      >
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/roles">
          <el-icon><Key /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
        <el-sub-menu index="ops">
          <template #title>
            <el-icon><Postcard /></el-icon>
            <span>运营管理</span>
          </template>
          <el-menu-item index="/tasks">
            <el-icon><Cpu /></el-icon>
            <span>生成任务</span>
          </el-menu-item>
          <el-menu-item index="/assets">
            <el-icon><FolderOpened /></el-icon>
            <span>成品资产</span>
          </el-menu-item>
          <el-menu-item index="/point-logs">
            <el-icon><Coin /></el-icon>
            <span>积分流水</span>
          </el-menu-item>
          <el-menu-item index="/recharge-orders">
            <el-icon><Sell /></el-icon>
            <span>充值订单</span>
          </el-menu-item>
          <el-menu-item index="/point-products">
            <el-icon><Goods /></el-icon>
            <span>积分商品</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="hdr-icon"><IconMenu /></el-icon>
          <span class="hdr-title">工作台</span>
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
  padding: 1.125rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.brand-name {
  font-weight: 700;
  font-size: 1.125rem;
  letter-spacing: 0.03em;
}
.brand-tag {
  font-size: 11px;
  font-weight: 500;
  color: rgba(148, 163, 184, 0.95);
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
