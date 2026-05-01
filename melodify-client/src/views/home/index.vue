<script setup lang="ts">
/**
 * 首页：产品介绍与未登录时的注册/登录入口；已登录时展示问候语。
 * 路由：/ ，与 login、register 同为「按目录 + index.vue」约定。
 */
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'HomePage' })

const router = useRouter()
const auth = useAuthStore()
const { isAuthenticated, displayName } = storeToRefs(auth)

/** 跳转注册页（保留现有 query，便于登录后回到原目标） */
const goRegister = () => {
  router.push('/register')
}

/** 跳转登录页 */
const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="home">
    <section class="hero melodify-glass-card">
      <div class="hero__copy">
        <p class="page-eyebrow hero__eyebrow">Text to Music · AI Studio</p>
        <h1 class="page-title page-title--xl hero__title">把灵感变成可以试听的完整歌曲</h1>
        <p class="page-desc hero__lead">
          输入一句创意、歌词或风格描述，Melodify 会帮你提交生成任务、追踪进度，并沉淀到作品库。
        </p>

        <div v-if="!isAuthenticated" class="hero__actions">
          <el-button type="primary" size="large" round @click="goRegister">免费开始创作</el-button>
          <el-button size="large" round plain @click="goLogin">已有账号登录</el-button>
        </div>

        <div v-else class="hero__welcome">
          <p class="hero__greeting">
            欢迎回来，<strong>{{ displayName }}</strong>
          </p>
          <div class="hero__quick">
            <el-button type="primary" round @click="router.push('/generate')">继续创作</el-button>
            <el-button round @click="router.push('/works')">查看作品库</el-button>
          </div>
        </div>
      </div>

      <div class="hero__visual" aria-hidden="true">
        <div class="player-card">
          <div class="wave">
            <span v-for="i in 18" :key="i" />
          </div>
          <p class="player-title">Dream Pop · 生成预览</p>
          <div class="player-line">
            <span />
          </div>
        </div>
      </div>
    </section>

    <section class="section-grid section-grid--3 feature-grid">
      <article class="feature-card soft-card">
        <span class="feature-index">01</span>
        <h2>简单模式</h2>
        <p>一句话描述情绪、风格和场景，适合快速把灵感变成 demo。</p>
      </article>
      <article class="feature-card soft-card">
        <span class="feature-index">02</span>
        <h2>自定义创作</h2>
        <p>补充标题、风格和歌词，适合更精细地控制成品方向。</p>
      </article>
      <article class="feature-card soft-card">
        <span class="feature-index">03</span>
        <h2>作品沉淀</h2>
        <p>任务状态、失败原因和试听音频统一进入作品库，便于复盘。</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 0.25rem;
}

.hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  align-items: center;
  gap: 2rem;
  padding: clamp(2rem, 6vw, 4.5rem);
}

.hero__copy,
.hero__visual {
  position: relative;
  z-index: 1;
}

.hero__eyebrow {
  display: inline-flex;
  padding: 0.35rem 0.8rem;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 999px;
  background: #f5f3ff;
}

.hero__title {
  line-height: 1.02;
  margin: 1rem 0 1.25rem;
}

.hero__lead {
  font-size: 1.08rem;
  max-width: 42rem;
  margin: 0 0 2rem;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  justify-content: flex-start;
  align-items: center;
}

.hero__welcome {
  padding: 0.25rem 0;
}

.hero__greeting {
  font-size: 1.125rem;
  color: var(--melodify-muted);
  margin-bottom: 0.5rem;
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: flex-start;
  margin-top: 1rem;
}

.hero__visual {
  display: flex;
  justify-content: center;
}

.player-card {
  width: min(100%, 25rem);
  padding: 1.5rem;
  border-radius: 2rem;
  background: #f8fafc;
  color: var(--melodify-strong);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 14px 38px rgba(15, 23, 42, 0.06);
}

.wave {
  height: 12rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
}

.wave span {
  width: 0.55rem;
  height: calc(2.5rem + (var(--i, 1) * 0.2rem));
  border-radius: 999px;
  background: #6d5dfc;
  opacity: 0.82;
}

.wave span:nth-child(3n) {
  height: 8rem;
}

.wave span:nth-child(4n) {
  height: 5.5rem;
}

.wave span:nth-child(5n) {
  height: 10rem;
}

.player-title {
  margin: 0.5rem 0 1rem;
  font-weight: 800;
}

.player-line {
  height: 0.55rem;
  border-radius: 999px;
  background: #e2e8f0;
}

.player-line span {
  display: block;
  width: 62%;
  height: 100%;
  border-radius: inherit;
  background: #6d5dfc;
}

.feature-grid {
  margin-top: 1.1rem;
}

.feature-card {
  padding: 1.35rem;
}

.feature-index {
  color: var(--el-color-primary);
  font-weight: 900;
  font-size: 0.82rem;
}

.feature-card h2 {
  margin: 0.45rem 0 0.45rem;
  color: var(--melodify-strong);
  font-size: 1.05rem;
  font-weight: 800;
}

.feature-card p {
  color: var(--melodify-muted);
  line-height: 1.7;
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
