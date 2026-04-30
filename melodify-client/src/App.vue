<script setup lang="ts">
/**
 * 应用根组件：根据路由 meta.layout 在「全屏子路由」与「AppShell + 子路由」之间切换。
 */
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'

const route = useRoute()

/** true 时不渲染 AppShell（登录、注册等） */
const isBlankLayout = computed(() => route.meta.layout === 'blank')
</script>

<template>
  <RouterView v-if="isBlankLayout" />
  <AppShell v-else>
    <RouterView />
  </AppShell>
</template>
