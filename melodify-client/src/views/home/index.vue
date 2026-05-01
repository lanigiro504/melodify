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
    <section class="hero">
      <h1 class="hero__title">Melodify</h1>
      <p class="hero__lead">
        AIGC 音乐生成平台：用简单描述生成属于你的旋律与编曲，登录后即可使用完整能力。
      </p>

      <div v-if="!isAuthenticated" class="hero__actions">
        <el-button type="primary" size="large" round @click="goRegister">立即注册</el-button>
        <el-button size="large" round plain @click="goLogin">已有账号，登录</el-button>
      </div>

      <div v-else class="hero__welcome">
        <p class="hero__greeting">
          欢迎回来，<strong>{{ displayName }}</strong>
        </p>
        <div class="hero__quick">
          <el-button type="primary" round @click="router.push('/generate')">去创作</el-button>
          <el-button round @click="router.push('/works')">我的作品</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 0.5rem;
}

.hero {
  text-align: center;
  padding: 2.5rem 0 1rem;
}

.hero__title {
  font-size: clamp(2rem, 5vw, 2.75rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--color-heading);
  margin-bottom: 1.25rem;
  position: relative;
  display: inline-block;
}

.hero__title::after {
  content: '';
  display: block;
  height: 3px;
  width: min(5rem, 40%);
  margin: 0.85rem auto 0;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, var(--el-color-primary), transparent);
  opacity: 0.85;
}

.hero__lead {
  font-size: 1.0625rem;
  line-height: 1.65;
  color: var(--color-text);
  max-width: 36rem;
  margin: 0 auto 2rem;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  justify-content: center;
  align-items: center;
}

.hero__welcome {
  padding: 1rem 0;
}

.hero__greeting {
  font-size: 1.125rem;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  margin-top: 1rem;
}
</style>
