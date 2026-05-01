<script setup lang="ts">
/**
 * 登录 / 注册页外壳：置于 AppShell 主区内，与全站同色背景及玻璃卡片风格一致。
 * 插槽：eyebrow（可选）、title、subtitle、默认（表单）、footer。
 */
import { computed, useSlots } from 'vue'
import { RouterLink } from 'vue-router'

defineOptions({ name: 'AuthLayout' })

const slots = useSlots()
const hasEyebrow = computed(() => !!slots.eyebrow)
</script>

<template>
  <div class="auth-page">
    <transition name="auth-fade" appear>
      <div class="auth-panel melodify-glass-card">
        <header class="auth-panel__head">
          <p v-if="hasEyebrow" class="page-eyebrow">
            <slot name="eyebrow" />
          </p>
          <h1 class="page-title page-title--md">
            <slot name="title" />
          </h1>
          <p class="page-desc auth-panel__lead">
            <slot name="subtitle" />
          </p>
        </header>

        <div class="auth-panel__body">
          <slot />
        </div>

        <footer class="auth-panel__footer">
          <p class="auth-panel__switch">
            <slot name="footer" />
          </p>
          <RouterLink class="auth-panel__home" to="/">返回首页</RouterLink>
        </footer>
      </div>
    </transition>
  </div>
</template>

<style src="@/assets/auth-layout.css"></style>
