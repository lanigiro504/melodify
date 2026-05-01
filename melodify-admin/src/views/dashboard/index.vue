<script setup lang="ts">
/**
 * 后台首页：导航说明与后续统计卡片占位。
 */
defineOptions({ name: 'AdminDashboard' })
</script>

<template>
  <div class="page">
    <el-page-header content="概览" />
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
          <template #header>后续扩展</template>
          <p class="muted">可在此接入任务监控、积分订单、内容审核等运营指标。</p>
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
code {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
</style>
