<script setup lang="ts">
/**
 * 后台首页：导航说明与后续统计卡片占位。
 */
import { RouterLink } from 'vue-router'

defineOptions({ name: 'AdminDashboard' })
</script>

<template>
  <div class="page">
    <el-page-header title="概览" />
    <el-row :gutter="16" class="cards">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <template #header>用户与角色</template>
          <p class="muted">在侧栏进入「用户管理」「角色管理」维护 sys_user / sys_role。</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <template #header>鉴权说明</template>
          <p class="muted">管理接口前缀 <code>/api/admin/**</code>，需 JWT 中角色键 <code>admin</code>。</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <template #header>快捷入口</template>
          <div class="quick">
            <RouterLink to="/tasks">生成任务</RouterLink>
            <RouterLink to="/assets">成品资产</RouterLink>
            <RouterLink to="/point-logs">积分流水</RouterLink>
            <RouterLink to="/recharge-orders">充值订单</RouterLink>
            <RouterLink to="/point-products">积分商品</RouterLink>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
}
.cards {
  margin-top: 1.25rem;
}
.muted {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  font-size: 14px;
}
.quick a {
  color: var(--el-color-primary);
  text-decoration: none;
}
.quick a:hover {
  text-decoration: underline;
}
code {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
</style>
