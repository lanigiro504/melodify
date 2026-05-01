<script setup lang="ts">
/**
 * 首页：产品与入口；作品广场仅在导航与底部卡片进入。
 */
import { storeToRefs } from 'pinia'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'HomePage' })

const router = useRouter()
const auth = useAuthStore()
const { isAuthenticated, displayName } = storeToRefs(auth)

const goRegister = () => router.push('/register')
const goLogin = () => router.push('/login')
</script>

<template>
  <div class="home">
    <section class="hero melodify-glass-card">
      <div class="hero__copy">
        <p class="page-eyebrow hero__eyebrow">Melodify · AI Music</p>
        <h1 class="page-title page-title--xl hero__title">把灵感变成可以试听的完整歌曲</h1>
        <p class="page-desc hero__lead">
          描述风格、情绪或歌词，提交生成并在作品库统一管理；需要时可将成品公开，供他人在<strong>作品广场</strong>试听。
        </p>

        <div class="hero__actions">
          <template v-if="!isAuthenticated">
            <el-button type="primary" size="large" round @click="goRegister">免费注册创作</el-button>
            <el-button size="large" round plain @click="goLogin">登录</el-button>
            <el-button size="large" round plain @click="router.push('/explore')">作品广场</el-button>
          </template>
          <template v-else>
            <p class="hero__greeting">你好，{{ displayName }}</p>
            <div class="hero__quick">
              <el-button type="primary" size="large" round @click="router.push('/generate')">创作</el-button>
              <el-button size="large" round plain @click="router.push('/works')">作品库</el-button>
              <el-button size="large" round plain @click="router.push('/explore')">广场</el-button>
            </div>
          </template>
        </div>

        <p class="hero__fineprint">
          <RouterLink to="/about">产品说明</RouterLink>
          <span aria-hidden="true"> · </span>
          <RouterLink to="/explore">广场</RouterLink>
          <template v-if="isAuthenticated">
            <span aria-hidden="true"> · </span>
            <RouterLink to="/recharge">积分</RouterLink>
          </template>
        </p>
      </div>
    </section>

    <section class="foot-strip" aria-label="入口">
      <RouterLink class="foot-strip__link soft-card" to="/generate">
        <span class="foot-strip__k">创作</span>
        <span class="foot-strip__v">简单模式与自定义歌词</span>
      </RouterLink>
      <RouterLink class="foot-strip__link soft-card" to="/explore">
        <span class="foot-strip__k">广场</span>
        <span class="foot-strip__v">浏览公开发布的作品</span>
      </RouterLink>
      <RouterLink class="foot-strip__link soft-card" to="/about">
        <span class="foot-strip__k">帮助</span>
        <span class="foot-strip__v">计费与能力说明</span>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 0.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding-bottom: 0.5rem;
}

.hero {
  padding: clamp(1.5rem, 4vw, 2.75rem) clamp(1.25rem, 4vw, 2.25rem);
}

.hero__eyebrow {
  display: inline-flex;
  padding: 0.35rem 0.8rem;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 999px;
  background: #f5f3ff;
}

.hero__title {
  line-height: 1.08;
  margin: 0.85rem 0 0.75rem;
}

.hero__lead {
  font-size: 1.05rem;
  max-width: 38rem;
  margin: 0 0 1.25rem;
  line-height: 1.6;
}

.hero__lead strong {
  color: var(--el-color-primary);
  font-weight: 800;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem 0.75rem;
}

.hero__greeting {
  width: 100%;
  margin: 0 0 0.25rem;
  font-size: 1rem;
  color: var(--melodify-muted);
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.hero__fineprint {
  margin: 1.15rem 0 0;
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.5;
}

.hero__fineprint a {
  color: var(--el-color-primary);
  font-weight: 700;
  text-decoration: none;
}

.hero__fineprint a:hover {
  text-decoration: underline;
}

.foot-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

@media (max-width: 720px) {
  .foot-strip {
    grid-template-columns: 1fr;
  }
}

.foot-strip__link {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 1rem 1.1rem;
  border-radius: 1rem;
  text-decoration: none;
  color: inherit;
  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.foot-strip__link:hover {
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  border-color: rgba(99, 102, 241, 0.2);
}

.foot-strip__k {
  font-weight: 900;
  font-size: 0.95rem;
  color: var(--melodify-strong);
}

.foot-strip__v {
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}
</style>
