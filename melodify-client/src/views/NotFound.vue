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
  background: var(--melodify-page-bg, #f3f1ed);
}
.nf-card {
  max-width: 420px;
  width: 100%;
  padding: 2rem 2.25rem;
  text-align: center;
}
.eyebrow {
  margin: 0;
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--melodify-muted, #6f6864);
}
h1 {
  margin: 0.65rem 0 0.5rem;
  font-size: 1.5rem;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--melodify-strong, #292524);
}
.lead {
  margin: 0 0 1.75rem;
  font-size: 0.945rem;
  color: var(--melodify-muted, #6f6864);
  line-height: 1.75;
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
  padding: 0.68rem 1.28rem;
  border-radius: 999px;
  font-weight: 600;
  font-size: 0.9375rem;
  text-decoration: none;
}
.btn-primary {
  background: var(--el-color-primary);
  color: #fff;
  border: 1px solid transparent;
  box-shadow: none;
  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.btn-primary:hover {
  background-color: color-mix(in srgb, var(--el-color-primary) 88%, black);
}

.btn-primary:active {
  transform: translateY(1px);
}

.btn-primary:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

.btn-quiet {
  border: 1px solid var(--melodify-divider-strong);
  color: var(--melodify-strong);
  background: var(--melodify-card-solid);
  box-shadow: none;
  transition: border-color 0.18s ease;
}

.btn-quiet:hover {
  border-color: rgba(var(--melodify-primary-rgb), 0.28);
}

.btn-quiet:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}
</style>
