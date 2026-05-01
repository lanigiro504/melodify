<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

defineOptions({ name: 'AdminDashboard' })

const auth = useAdminAuthStore()
const { displayName } = storeToRefs(auth)
</script>

<template>
  <div class="dashboard">
    <header class="hero">
      <h1>{{ displayName ? `您好，${displayName}` : '工作台' }}</h1>
      <p class="sub">请选择左侧菜单或下方入口进入对应模块。</p>
    </header>

    <el-card class="panel" shadow="never">
      <template #header>常用入口</template>
      <nav class="links">
        <RouterLink to="/users">用户管理</RouterLink>
        <RouterLink to="/roles">角色管理</RouterLink>
        <RouterLink to="/tasks">生成任务</RouterLink>
        <RouterLink to="/assets">成品资产</RouterLink>
        <RouterLink to="/point-logs">积分流水</RouterLink>
        <RouterLink to="/recharge-orders">充值订单</RouterLink>
        <RouterLink to="/point-products">积分商品</RouterLink>
      </nav>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 960px;
}
.hero {
  margin-bottom: 1.25rem;
}
.hero h1 {
  margin: 0 0 0.35rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.sub {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.panel {
  border-radius: 8px;
}
.links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  font-size: 14px;
}
.links a {
  color: var(--el-color-primary);
  text-decoration: none;
}
.links a:hover {
  text-decoration: underline;
}
</style>
