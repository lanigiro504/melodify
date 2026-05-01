<script setup lang="ts">
/**
 * 首页：轻量产品介绍 + 作品广场（社区）为主，减少重复信息密度。
 */
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
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
const exploreTotal = ref(0)

const communityStatsLine = computed(() => {
  if (previewLoading.value) return '加载社区作品…'
  const n = exploreTotal.value
  if (n <= 0) return '成为第一个把作品公开到广场的创作者吧'
  return `共 ${n} 首公开作品 · 访客可直接试听`
})

const fetchPreview = async () => {
  previewLoading.value = true
  try {
    const page = unwrapResult(await pageExploreAssets(1, 10))
    previewRows.value = page.records
    exploreTotal.value = page.total
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
        <p class="page-eyebrow hero__eyebrow">Melodify · AI Music</p>
        <h1 class="page-title page-title--xl hero__title">创作音乐，也在社区里被听见</h1>
        <p class="page-desc hero__lead">
          一句话生成完整歌曲；若愿意，可把成品公开到<strong>作品广场</strong>，让更多人用同一套播放器即时试听。
        </p>

        <div class="hero__actions">
          <template v-if="!isAuthenticated">
            <el-button type="primary" size="large" round @click="goRegister">免费注册创作</el-button>
            <el-button size="large" round plain @click="router.push('/explore')">先逛广场</el-button>
            <el-button size="large" round text @click="goLogin">登录</el-button>
          </template>
          <template v-else>
            <p class="hero__greeting">
              <span class="hero__hi">你好，{{ displayName }}</span>
            </p>
            <div class="hero__quick">
              <el-button type="primary" size="large" round @click="router.push('/generate')">创作</el-button>
              <el-button size="large" round @click="router.push('/explore')">广场</el-button>
              <el-button size="large" round plain @click="router.push('/works')">作品库</el-button>
            </div>
          </template>
        </div>

        <p class="hero__fineprint">
          <RouterLink to="/about">产品说明</RouterLink>
          <span aria-hidden="true"> · </span>
          <RouterLink v-if="isAuthenticated" to="/recharge">积分</RouterLink>
          <template v-if="isAuthenticated"><span aria-hidden="true"> · </span></template>
          <RouterLink to="/explore">广场规则：公开作品对所有人可见</RouterLink>
        </p>
      </div>
    </section>

    <section class="community melodify-glass-card">
      <header class="community__head">
        <div class="community__titles block-start">
          <p class="page-eyebrow community__eyebrow">社区</p>
          <h2 class="page-title page-title--lg community__title">作品广场</h2>
          <p class="community__stats">{{ communityStatsLine }}</p>
        </div>
        <div class="community__tools">
          <el-button
            type="primary"
            round
            :loading="previewLoading"
            @click="router.push('/explore')"
          >
            进入广场
          </el-button>
          <el-button v-if="isAuthenticated" round plain @click="router.push('/works')">
            我的作品与公开设置
          </el-button>
          <el-button round plain :loading="previewLoading" @click="fetchPreview">换一批预览</el-button>
        </div>
      </header>

      <p class="community__howto">
        在作品详情打开<strong>公开到广场</strong>，即可把成品分享给所有访客；访客点击卡片会通过底部播放器试听（与广场列表一致）。
      </p>

      <div v-loading="previewLoading" class="community__rail-wrap">
        <el-empty
          v-if="!previewRows.length && !previewLoading"
          description="暂无人公开作品，去创作并打开「公开到广场」吧"
        />

        <div v-else class="community__rail">
          <article
            v-for="item in previewRows"
            :key="item.id"
            class="community-card soft-card"
          >
            <div class="community-card__cover">
              <span>{{ (item.title || item.prompt || 'AI').slice(0, 2) }}</span>
            </div>
            <div class="community-card__body">
              <h3>{{ item.title?.trim() || '未命名' }}</h3>
              <p class="community-card__hint">
                {{ item.prompt?.trim() || '—' }}
              </p>
              <div class="community-card__meta">
                <span>{{ item.durationSec ?? 0 }} 秒</span>
                <span>·</span>
                <span>{{ item.likeCount }} 赞</span>
              </div>
              <el-button type="primary" round size="small" @click="onPlayPreview(item)">
                试听
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="foot-strip" aria-label="更多">
      <RouterLink class="foot-strip__link soft-card" to="/generate">
        <span class="foot-strip__k">创作</span>
        <span class="foot-strip__v">模型与歌词模式</span>
      </RouterLink>
      <RouterLink class="foot-strip__link soft-card" to="/explore">
        <span class="foot-strip__k">广场</span>
        <span class="foot-strip__v">只展示公开发布的作品</span>
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
  gap: 1.75rem;
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
  max-width: 20ch;
}

@media (min-width: 720px) {
  .hero__title {
    max-width: none;
  }
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
  margin: 0;
}

.hero__hi {
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

.community {
  padding: 1.35rem clamp(1rem, 3vw, 1.5rem) 1.5rem;
}

.community__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem 1.25rem;
  margin-bottom: 0.85rem;
}

.block-start {
  min-width: min(100%, 18rem);
}

.community__eyebrow {
  margin-bottom: 0.35rem;
}

.community__title {
  margin: 0 0 0.35rem;
}

.community__stats {
  margin: 0;
  font-size: 0.94rem;
  color: var(--melodify-muted);
  max-width: 36rem;
  line-height: 1.5;
}

.community__tools {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.community__howto {
  margin: 0 0 1rem;
  font-size: 0.88rem;
  color: var(--melodify-muted);
  line-height: 1.55;
  max-width: 48rem;
}

.community__howto strong {
  color: var(--melodify-strong);
  font-weight: 800;
}

.community__rail-wrap {
  min-height: 3rem;
}

.community__rail {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  padding: 0.2rem 0.15rem 0.6rem;
  margin: 0 -0.15rem;
  scroll-snap-type: x mandatory;
  scrollbar-width: thin;
}

.community__rail::-webkit-scrollbar {
  height: 6px;
}

.community__rail::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.45);
  border-radius: 999px;
}

.community-card {
  flex: 0 0 min(17.5rem, calc(100vw - 4.5rem));
  scroll-snap-align: start;
  display: grid;
  grid-template-columns: 4.75rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem 1.05rem;
  border-radius: 1.15rem;
  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.community-card:hover {
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.07);
  border-color: rgba(99, 102, 241, 0.22);
}

.community-card__cover {
  width: 4.75rem;
  height: 4.75rem;
  border-radius: 1.05rem;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #f5f3ff, #eef2ff);
  color: #6d5dfc;
  font-weight: 900;
  border: 1px solid rgba(99, 102, 241, 0.12);
}

.community-card__body h3 {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  font-weight: 900;
  color: var(--melodify-strong);
  line-height: 1.25;
}

.community-card__hint {
  margin: 0 0 0.45rem;
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.community-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  align-items: center;
  font-size: 0.78rem;
  color: var(--melodify-muted);
  margin-bottom: 0.55rem;
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
