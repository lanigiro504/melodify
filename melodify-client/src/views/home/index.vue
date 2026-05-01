<script setup lang="ts">
/**
 * 首页：产品主入口、公开作品预览、核心能力说明；已登录时提供创作/作品库快捷入口。
 */
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { pageExploreAssets, type ExploreAssetItem } from '@/api/explore'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { extractTrackLyrics } from '@/utils/trackLyrics'

defineOptions({ name: 'HomePage' })

const router = useRouter()
const auth = useAuthStore()
const player = usePlayerStore()
const { isAuthenticated, displayName } = storeToRefs(auth)

const previewLoading = ref(false)
const previewRows = ref<ExploreAssetItem[]>([])

const fetchPreview = async () => {
  previewLoading.value = true
  try {
    const page = unwrapResult(await pageExploreAssets(1, 6))
    previewRows.value = page.records
  } catch (e) {
    showSubmitError(e, '加载广场预览失败')
  } finally {
    previewLoading.value = false
  }
}

const previewPlaySubtitle = (item: ExploreAssetItem) => {
  const parts: string[] = []
  if (item.durationSec != null && item.durationSec > 0) parts.push(`${item.durationSec} 秒`)
  parts.push(`${item.likeCount} 赞`)
  return parts.join(' · ')
}

const onPlayPreview = (item: ExploreAssetItem) => {
  if (!item.fileUrl?.trim()) {
    ElMessage.warning('暂无可播放地址')
    return
  }
  player.playTrack({
    title: item.title?.trim() || item.prompt?.trim() || '未命名作品',
    fileUrl: item.fileUrl,
    subtitle: previewPlaySubtitle(item),
    lyrics: extractTrackLyrics(item.prompt, undefined),
    durationSec: item.durationSec ?? undefined,
  })
}

const goRegister = () => {
  router.push('/register')
}

const goLogin = () => {
  router.push('/login')
}

onMounted(() => void fetchPreview())
</script>

<template>
  <div class="home">
    <section class="hero melodify-glass-card">
      <div class="hero__copy">
        <p class="page-eyebrow hero__eyebrow">Text to Music · AI Studio</p>
        <h1 class="page-title page-title--xl hero__title">把灵感变成可以试听的完整歌曲</h1>
        <p class="page-desc hero__lead">
          输入创意或歌词描述，Melodify 对接生成管线、追踪任务并在作品库沉淀成品；广场上的公开作品无需登录即可试听。
        </p>

        <div class="hero__primary-row">
          <template v-if="!isAuthenticated">
            <el-button type="primary" size="large" round @click="goRegister">免费开始创作</el-button>
            <el-button size="large" round plain @click="goLogin">已有账号登录</el-button>
          </template>
          <template v-else>
            <p class="hero__greeting">
              欢迎回来，<strong>{{ displayName }}</strong>
            </p>
            <div class="hero__quick">
              <el-button type="primary" size="large" round @click="router.push('/generate')">继续创作</el-button>
              <el-button size="large" round @click="router.push('/works')">作品库</el-button>
            </div>
          </template>
        </div>

        <div class="hero__secondary-row">
          <RouterLink class="hero__text-link" to="/explore">逛逛作品广场</RouterLink>
          <span class="hero__sep" aria-hidden="true">·</span>
          <RouterLink class="hero__text-link" to="/about">了解 Melodify</RouterLink>
          <template v-if="isAuthenticated">
            <span class="hero__sep" aria-hidden="true">·</span>
            <RouterLink class="hero__text-link" to="/recharge">积分充值</RouterLink>
          </template>
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

    <section class="preview-block">
      <div class="preview-head">
        <div>
          <p class="page-eyebrow preview-eyebrow">Explore</p>
          <h2 class="page-title page-title--lg preview-title">听听创作者公开分享的成品</h2>
          <p class="page-desc preview-desc">点击下方卡片即可用底部播放器试听，与广场页使用同一套公开作品数据。</p>
        </div>
        <RouterLink class="preview-more" to="/explore">查看全部</RouterLink>
      </div>

      <div v-loading="previewLoading" class="preview-grid">
        <el-empty
          v-if="!previewRows.length && !previewLoading"
          description="暂无公开作品，注册后去创作并打开「公开到广场」吧"
        />

        <article
          v-for="item in previewRows"
          :key="item.id"
          class="preview-card soft-card"
        >
          <div class="preview-cover">
            <span>{{ (item.title || item.prompt || 'AI').slice(0, 2) }}</span>
          </div>
          <div class="preview-body">
            <h3>{{ item.title?.trim() || '未命名' }}</h3>
            <p class="preview-prompt">{{ item.prompt?.trim() || '—' }}</p>
            <div class="preview-meta">
              <span>{{ item.durationSec ?? 0 }} 秒</span>
              <span>{{ item.likeCount }} 赞</span>
            </div>
            <el-button type="primary" round size="small" @click="onPlayPreview(item)">播放</el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="section-grid section-grid--3 feature-grid">
      <article class="feature-card soft-card">
        <span class="feature-index">01</span>
        <h2>简单与自定义</h2>
        <p>一句话快速出 demo，或补充标题、风格与歌词做更细的控制，适配不同创作节奏。</p>
      </article>
      <article class="feature-card soft-card">
        <span class="feature-index">02</span>
        <h2>任务与作品库</h2>
        <p>生成中、成功与失败状态集中展示，试听与复盘都留在作品库里，避免散落在各处。</p>
      </article>
      <article class="feature-card soft-card">
        <span class="feature-index">03</span>
        <h2>广场与播放器</h2>
        <p>可选将成品公开到广场；全局迷你播放器支持展开查看歌词，试听路径一致。</p>
      </article>
    </section>

    <section class="section-grid section-grid--3 cap-grid">
      <article class="cap-card soft-card">
        <h3>Suno 管线</h3>
        <p>由后端统一提交与回调，前端只关心进度与结果。</p>
      </article>
      <article class="cap-card soft-card">
        <h3>积分消费</h3>
        <p>按任务扣减积分，充值与个人中心展示余额，成本可见。</p>
      </article>
      <article class="cap-card soft-card">
        <h3>本地化试听</h3>
        <p>音频可镜像存储并由本站下发，减少外链不稳定带来的影响。</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 0.25rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
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
  margin: 0 0 1.5rem;
}

.hero__primary-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1rem;
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.hero__greeting {
  font-size: 1.125rem;
  color: var(--melodify-muted);
  margin: 0;
}

.hero__secondary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.5rem;
  margin-top: 1.25rem;
  font-size: 0.9375rem;
}

.hero__text-link {
  color: var(--el-color-primary);
  font-weight: 700;
  text-decoration: none;
}

.hero__text-link:hover {
  text-decoration: underline;
}

.hero__sep {
  color: var(--melodify-muted);
  user-select: none;
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

.preview-block {
  padding: 0.15rem 0;
}

.preview-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.1rem;
}

.preview-eyebrow {
  margin-bottom: 0.35rem;
}

.preview-title {
  margin: 0 0 0.35rem;
}

.preview-desc {
  margin: 0;
  max-width: 36rem;
}

.preview-more {
  flex-shrink: 0;
  font-weight: 800;
  color: var(--el-color-primary);
  text-decoration: none;
  padding: 0.5rem 0;
}

.preview-more:hover {
  text-decoration: underline;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 1rem;
  min-height: 4rem;
}

.preview-card {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  gap: 0.85rem;
  padding: 1rem;
}

.preview-cover {
  width: 4.5rem;
  height: 4.5rem;
  border-radius: 1rem;
  display: grid;
  place-items: center;
  background: #f5f3ff;
  color: #6d5dfc;
  font-weight: 900;
  font-size: 1rem;
}

.preview-body h3 {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  font-weight: 800;
  color: var(--melodify-strong);
}

.preview-prompt {
  margin: 0 0 0.5rem;
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-meta {
  display: flex;
  gap: 0.65rem;
  font-size: 0.78rem;
  color: var(--melodify-muted);
  margin-bottom: 0.6rem;
}

.feature-grid {
  margin-top: 0;
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
  margin: 0;
}

.cap-grid {
  margin-top: 0;
}

.cap-card {
  padding: 1.15rem 1.25rem;
}

.cap-card h3 {
  margin: 0 0 0.45rem;
  font-size: 0.98rem;
  font-weight: 800;
  color: var(--melodify-strong);
}

.cap-card p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--melodify-muted);
  line-height: 1.65;
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
