<script setup lang="ts">
import { Menu as IconMenu } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminNavSections, adminSubmenuDefaultOpenIndexes } from '@/router/adminNavConfig'
import { resolveMatchedPageTitle } from '@/router/routeTitle'
import { useAdminAuthStore } from '@/stores/adminAuth'

const route = useRoute()
const router = useRouter()
const auth = useAdminAuthStore()

const activePath = computed(() => route.path)
const hdrTitle = computed(() => resolveMatchedPageTitle(route))

const submenuOpenDefaults = adminSubmenuDefaultOpenIndexes()

const handleLogout = () => {
  auth.logout()
  void router.push('/login')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="232px" class="aside">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">M</span>
        <div class="brand-text">
          <span class="brand-name">Melodify</span>
          <span class="brand-tag">控制台</span>
        </div>
      </div>
      <el-menu
        :default-active="activePath"
        :default-openeds="submenuOpenDefaults"
        router
        class="menu"
        background-color="transparent"
        text-color="#b8c0d4"
        active-text-color="#f1f5f9"
      >
        <template v-for="section in adminNavSections" :key="section.type === 'item' ? section.leaf.path : section.index">
          <el-menu-item v-if="section.type === 'item'" :index="`/${section.leaf.path}`">
            <el-icon><component :is="section.leaf.icon" /></el-icon>
            <span>{{ section.leaf.title }}</span>
          </el-menu-item>
          <el-sub-menu v-else :index="section.index">
            <template #title>
              <el-icon><component :is="section.icon" /></el-icon>
              <span>{{ section.title }}</span>
            </template>
            <el-menu-item v-for="c in section.children" :key="c.path" :index="`/${c.path}`">
              <el-icon><component :is="c.icon" /></el-icon>
              <span>{{ c.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header" height="56px">
        <div class="header-left">
          <el-icon class="hdr-icon" aria-hidden="true"><IconMenu /></el-icon>
          <span class="hdr-title">{{ hdrTitle }}</span>
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
  background: var(--admin-sidebar-bg, #141428);
  color: #e2e8f0;
  border-right: 1px solid var(--admin-sidebar-border, rgba(255, 255, 255, 0.06));
  display: flex;
  flex-direction: column;
}
.brand {
  padding: 1.2rem 1rem 1.15rem;
  border-bottom: 1px solid var(--admin-sidebar-border, rgba(255, 255, 255, 0.06));
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.brand-mark {
  width: 2.35rem;
  height: 2.35rem;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 0.75rem;
  font-size: 1.05rem;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(145deg, var(--admin-accent, #6d5dfc), #5b4cdb);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}
.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.brand-name {
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.02em;
  line-height: 1.2;
}
.brand-tag {
  font-size: 10px;
  font-weight: 600;
  color: rgba(148, 163, 184, 0.95);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.menu {
  flex: 1;
  border-right: none;
  padding: 0.65rem 0.5rem 1rem;
  background: transparent !important;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin: 2px 0;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(92deg, rgba(109, 93, 252, 0.42), rgba(109, 93, 252, 0.12)) !important;
  box-shadow: inset 3px 0 0 0 var(--admin-accent, #6d5dfc);
  font-weight: 600;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(1rem, 3vw, 1.65rem);
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: saturate(1.25) blur(14px);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.7) inset;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}
.hdr-icon {
  font-size: 1.35rem;
  color: var(--el-text-color-secondary);
  opacity: 0.82;
}
.hdr-title {
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: -0.02em;
  color: var(--el-text-color-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.who {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  font-weight: 500;
}
.main {
  position: relative;
  background: transparent;
  min-height: calc(100vh - 56px);
  padding: 1.15rem clamp(1rem, 2.5vw, 1.5rem) 1.75rem;
}
</style>
