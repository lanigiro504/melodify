<script setup lang="ts">
/**
 * 登录 / 注册页外壳：渐变背景、卡片容器与入场动画。
 * 插槽：title / subtitle（卡片头）、默认（表单主体）、footer（底部文案与链接）。
 */
defineOptions({ name: 'AuthLayout' })
</script>

<template>
  <div class="auth-page">
    <transition name="auth-fade" appear>
      <el-card class="auth-card" shadow="always">
        <template #header>
          <div class="auth-card__header">
            <span class="auth-card__title">
              <slot name="title" />
            </span>
            <span class="auth-card__subtitle">
              <slot name="subtitle" />
            </span>
          </div>
        </template>

        <slot />

        <p class="auth-card__footer">
          <slot name="footer" />
        </p>
      </el-card>
    </transition>
  </div>
</template>

<style src="@/assets/auth-layout.css"></style>
