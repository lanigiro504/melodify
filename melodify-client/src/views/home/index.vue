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
  padding-top: 0.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-bottom: 0.5rem;
}

.hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(18rem, 0.88fr);
  align-items: center;
  gap: clamp(1.25rem, 4vw, 3rem);
  min-height: 25rem;
  padding: clamp(2rem, 5vw, 4rem);
  border-color: rgba(109, 93, 252, 0.14);
  background:
    radial-gradient(circle at 14% 16%, rgba(109, 93, 252, 0.16), transparent 34%),
    radial-gradient(circle at 85% 20%, rgba(45, 212, 191, 0.15), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92));
  box-shadow:
    0 24px 70px rgba(15, 23, 42, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
}

.hero::before {
  content: '';
  position: absolute;
  inset: 1px;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.7), transparent 38%),
    linear-gradient(90deg, rgba(109, 93, 252, 0.08), transparent 55%);
}

.hero::after {
  content: '';
  position: absolute;
  right: -7rem;
  bottom: -8rem;
  width: 20rem;
  height: 20rem;
  border-radius: 999px;
  background: rgba(109, 93, 252, 0.08);
  filter: blur(10px);
}

.hero__copy,
.hero__visual {
  position: relative;
  z-index: 1;
}

.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid rgba(99, 102, 241, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 26px rgba(99, 102, 241, 0.08);
  backdrop-filter: blur(10px);
}

.hero__title {
  max-width: 10.8em;
  line-height: 0.96;
  margin: 1rem 0 1rem;
  text-wrap: balance;
}

.hero__title span {
  display: block;
  background: linear-gradient(100deg, #111827 0%, #4338ca 48%, #0891b2 100%);
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero__lead {
  font-size: 1.08rem;
  max-width: 41rem;
  margin: 0 0 1.35rem;
  line-height: 1.75;
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

.hero__actions :deep(.el-button) {
  min-width: 6.75rem;
}

.hero__actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-color: transparent;
  box-shadow: 0 16px 34px rgba(79, 70, 229, 0.24);
}

.hero__actions :deep(.el-button.is-plain) {
  border-color: rgba(99, 102, 241, 0.14);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(10px);
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

.hero__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1.15rem;
}

.hero__metrics span {
  padding: 0.42rem 0.7rem;
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.64);
  color: #475569;
  font-size: 0.8rem;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
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

.hero__visual {
  display: flex;
  justify-content: center;
}

.studio-card {
  width: min(100%, 25rem);
  padding: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 1.75rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.58)),
    linear-gradient(135deg, rgba(109, 93, 252, 0.1), rgba(45, 212, 191, 0.08));
  box-shadow:
    0 22px 54px rgba(15, 23, 42, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
  backdrop-filter: blur(18px);
}

.studio-card__top,
.studio-card__track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.studio-card__badge {
  color: #4338ca;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.studio-card__pulse {
  width: 0.65rem;
  height: 0.65rem;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 0.4rem rgba(34, 197, 94, 0.14);
}

.studio-wave {
  height: 13rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.42rem;
  margin: 0.75rem 0;
  border-radius: 1.25rem;
  background:
    radial-gradient(circle at 50% 45%, rgba(109, 93, 252, 0.13), transparent 58%),
    rgba(248, 250, 252, 0.74);
}

.studio-wave span {
  width: 0.44rem;
  height: 3rem;
  border-radius: 999px;
  background: linear-gradient(180deg, #7c3aed, #22d3ee);
  opacity: 0.78;
}

.studio-wave span:nth-child(2n) {
  height: 6.5rem;
}

.studio-wave span:nth-child(3n) {
  height: 9rem;
}

.studio-wave span:nth-child(5n) {
  height: 11rem;
}

.studio-card__track {
  padding: 0.75rem 0.8rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.78);
}

.studio-card__track strong,
.studio-card__track span {
  display: block;
}

.studio-card__track strong {
  color: var(--melodify-strong);
  font-weight: 900;
}

.studio-card__track span {
  color: var(--melodify-muted);
  font-size: 0.78rem;
}

.studio-card__duration {
  color: #4338ca !important;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.foot-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
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
  min-height: 7rem;
  padding: 1.1rem 1.15rem;
  border-radius: 1.25rem;
  text-decoration: none;
  color: inherit;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.92);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.foot-strip__link:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
  border-color: rgba(99, 102, 241, 0.2);
}

.foot-strip__icon {
  display: inline-flex;
  width: fit-content;
  padding: 0.22rem 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(248, 250, 252, 0.95);
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 900;
}

.foot-strip__k {
  margin-top: auto;
  font-weight: 900;
  font-size: 1.02rem;
  color: var(--melodify-strong);
}

.foot-strip__v {
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero__visual {
    display: none;
  }
}
</style>
