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
        <h1 class="page-title page-title--xl hero__title">
          把灵感变成
          <span>可以试听的完整歌曲</span>
        </h1>
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

        <div class="hero__metrics" aria-label="能力摘要">
          <span>歌词模式</span>
          <span>任务追踪</span>
          <span>全局播放器</span>
        </div>

        <p class="hero__fineprint">
          <RouterLink to="/about">产品说明</RouterLink>
          <span aria-hidden="true"> · </span>
          <RouterLink to="/explore">广场</RouterLink>
          <template v-if="isAuthenticated">
            <span aria-hidden="true"> · </span>
            <RouterLink to="/recharge">积分</RouterLink>
            <span aria-hidden="true"> · </span>
            <RouterLink to="/profile">个人中心</RouterLink>
          </template>
        </p>
      </div>

      <div class="hero__visual" aria-hidden="true">
        <div class="studio-card">
          <div class="studio-card__top">
            <span class="studio-card__badge">Live Studio</span>
            <span class="studio-card__pulse" />
          </div>
          <div class="studio-wave">
            <span v-for="i in 18" :key="i" />
          </div>
          <div class="studio-card__track">
            <div>
              <strong>城市夜航</strong>
              <span>Dream pop · Female vocal</span>
            </div>
            <span class="studio-card__duration">3:26</span>
          </div>
        </div>
      </div>
    </section>

    <section class="foot-strip" aria-label="入口">
      <RouterLink class="foot-strip__link soft-card" to="/generate">
        <span class="foot-strip__icon">01</span>
        <span class="foot-strip__k">创作</span>
        <span class="foot-strip__v">简单模式与自定义歌词</span>
      </RouterLink>
      <RouterLink class="foot-strip__link soft-card" to="/explore">
        <span class="foot-strip__icon">02</span>
        <span class="foot-strip__k">广场</span>
        <span class="foot-strip__v">浏览公开发布的作品</span>
      </RouterLink>
      <RouterLink class="foot-strip__link soft-card" to="/about">
        <span class="foot-strip__icon">03</span>
        <span class="foot-strip__k">帮助</span>
        <span class="foot-strip__v">计费与能力说明</span>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 0.35rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-bottom: 0.5rem;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(18rem, 0.88fr);
  align-items: center;
  gap: clamp(1.25rem, 4vw, 3rem);
  min-height: 24rem;
  padding: clamp(1.75rem, 4vw, 3rem);
}

.hero__copy {
  min-width: 0;
}

.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 0.32rem 0.75rem;
  border: 1px solid var(--melodify-divider-strong);
  border-radius: 999px;
  background: var(--melodify-surface-muted);
  color: var(--melodify-muted);
}

.hero__title {
  max-width: 10.8em;
  line-height: 1.02;
  margin: 1rem 0;
  text-wrap: balance;
}

.hero__title span {
  display: block;
  color: var(--el-color-primary);
}

.hero__lead {
  font-size: 1.05rem;
  max-width: 41rem;
  margin: 0 0 1.25rem;
}

.hero__lead strong {
  color: var(--el-color-primary);
  font-weight: 700;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem 0.75rem;
}

.hero__actions :deep(.el-button) {
  min-width: 6.75rem;
}

.hero__greeting {
  width: 100%;
  margin: 0 0 0.35rem;
  font-size: 0.98rem;
  color: var(--melodify-muted);
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.hero__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 1rem;
}

.hero__metrics span {
  padding: 0.38rem 0.68rem;
  border-radius: 999px;
  border: 1px solid var(--melodify-divider);
  background: var(--melodify-surface-muted);
  color: var(--melodify-muted);
  font-size: 0.78rem;
  font-weight: 500;
}

.hero__fineprint {
  margin: 1rem 0 0;
  font-size: 0.8125rem;
  color: var(--melodify-muted);
}

.hero__fineprint a {
  color: var(--el-color-primary);
  font-weight: 600;
}

.hero__fineprint a:hover {
  text-decoration: underline;
}

.hero__visual {
  display: flex;
  justify-content: center;
}

.studio-card {
  width: min(100%, 24rem);
  padding: 1rem;
  border: 1px solid var(--melodify-divider-strong);
  border-radius: var(--melodify-radius-lg);
  background: var(--melodify-card-solid);
  box-shadow: var(--melodify-shadow-card);
}

.studio-card__top,
.studio-card__track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.studio-card__badge {
  color: var(--el-color-primary);
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.studio-card__pulse {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
  background: var(--el-color-success);
}

.studio-wave {
  height: 11rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.42rem;
  margin: 0.65rem 0;
  border-radius: var(--melodify-radius-lg);
  background: var(--melodify-surface-muted);
}

.studio-wave span {
  width: 0.4rem;
  height: 3rem;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    var(--el-color-primary),
    var(--el-color-primary-light-7)
  );
  opacity: 0.75;
}

.studio-wave span:nth-child(2n) {
  height: 6rem;
}

.studio-wave span:nth-child(3n) {
  height: 8.5rem;
}

.studio-wave span:nth-child(5n) {
  height: 10rem;
}

.studio-card__track {
  padding: 0.65rem 0.75rem;
  border-radius: var(--melodify-radius-md);
  background: var(--melodify-surface-muted);
}

.studio-card__track strong,
.studio-card__track span {
  display: block;
}

.studio-card__track strong {
  color: var(--melodify-strong);
  font-weight: 600;
}

.studio-card__track span {
  color: var(--melodify-muted);
  font-size: 0.76rem;
}

.studio-card__duration {
  color: var(--el-color-primary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.foot-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 720px) {
  .foot-strip {
    grid-template-columns: 1fr;
  }
}

.foot-strip__link {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.28rem;
  min-height: 6.75rem;
  padding: 1rem 1.1rem;
  border-radius: var(--melodify-radius-lg);
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-card-solid);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.foot-strip__link:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--melodify-primary-rgb), 0.22);
  box-shadow: var(--melodify-shadow-hover);
}

.foot-strip__link:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 3px;
}

.foot-strip__link:focus:not(:focus-visible) {
  outline: none;
}

.foot-strip__icon {
  display: inline-flex;
  width: fit-content;
  padding: 0.2rem 0.48rem;
  border-radius: var(--melodify-radius-sm);
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-surface-muted);
  color: var(--melodify-muted);
  font-size: 0.72rem;
  font-weight: 600;
}

.foot-strip__k {
  margin-top: auto;
  font-weight: 600;
  font-size: 1rem;
  color: var(--melodify-strong);
}

.foot-strip__v {
  font-size: 0.8125rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: unset;
  }

  .hero__visual {
    display: none;
  }
}
</style>
