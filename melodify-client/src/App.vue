<script setup lang="ts">
/**
 * 应用根组件：全站共用 AppShell，登录/注册与其它页面视觉与导航语境一致。
 */
import { RouterView } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
</script>

<template>
  <AppShell>
    <RouterView />
  </AppShell>
</template>
