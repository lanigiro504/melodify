<script setup lang="ts">
/**
 * 全屏 404：不依赖登录态，文案简洁。
 */
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="nf">
    <div class="nf-card melodify-glass-card">
      <p class="eyebrow">404</p>
      <h1>页面不存在</h1>
      <p class="lead">链接可能失效，或页面已移动。</p>
      <div class="nf-actions">
        <RouterLink to="/" class="btn-primary">回首页</RouterLink>
        <RouterLink to="/explore" class="btn-quiet">逛逛广场</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.nf {
  min-height: min(70vh, 32rem);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: var(--melodify-page-bg, #f1f5f9);
}
.nf-card {
  max-width: 420px;
  width: 100%;
  padding: 2rem 2.25rem;
  text-align: center;
}
.eyebrow {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--melodify-muted, #64748b);
}
h1 {
  margin: 0.65rem 0 0.5rem;
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--melodify-strong, #0f172a);
}
.lead {
  margin: 0 0 1.75rem;
  font-size: 0.9375rem;
  color: var(--melodify-muted, #64748b);
  line-height: 1.6;
}
.nf-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}
.btn-primary,
.btn-quiet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.65rem 1.25rem;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.9375rem;
  text-decoration: none;
}
.btn-primary {
  background: #6d5dfc;
  color: #fff;
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.2);
}
.btn-quiet {
  border: 1px solid rgba(148, 163, 184, 0.45);
  color: var(--melodify-strong, #0f172a);
  background: #fff;
}
</style>
