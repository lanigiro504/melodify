<script setup lang="ts">
/**
 * 公开广场：无需登录即可浏览 isPublic=1 的作品并发起到全局播放器。
 */
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { pageExploreAssets, type ExploreAssetItem } from '@/api/explore'
import { usePlayerStore } from '@/stores/player'
import { unwrapResult } from '@/utils/apiResult'
import { exploreItemPlaySubtitle } from '@/utils/exploreDisplay'
import { extractTrackLyrics } from '@/utils/trackLyrics'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'ExplorePage' })

const player = usePlayerStore()
const loading = ref(false)
const rows = ref<ExploreAssetItem[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 12 })

async function fetchList() {
  loading.value = true
  try {
    const page = unwrapResult(await pageExploreAssets(pager.current, pager.size))
    rows.value = page.records
    total.value = page.total
  } catch (e) {
    showSubmitError(e, '加载广场作品失败')
  } finally {
    loading.value = false
  }
}

const onPlay = (item: ExploreAssetItem) => {
  if (!item.fileUrl?.trim()) {
    ElMessage.warning('暂无可播放地址')
    return
  }
  player.playTrack({
    title: item.title?.trim() || item.prompt?.trim() || '未命名作品',
    fileUrl: item.fileUrl,
    subtitle: exploreItemPlaySubtitle(item),
    lyrics: extractTrackLyrics(item.prompt, undefined),
    durationSec: item.durationSec ?? undefined,
  })
}

onMounted(() => void fetchList())
</script>

<template>
  <div class="page-stack">
    <section class="page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Explore</p>
        <h1 class="page-title page-title--lg">作品广场</h1>
        <p class="page-desc page-desc--wide">创作者公开分享的成品，点击即可用底部播放器试听（无需登录）。</p>
      </div>
      <el-button round :loading="loading" @click="fetchList">刷新</el-button>
    </section>

    <section v-loading="loading" class="explore-grid">
      <el-empty v-if="!rows.length && !loading" description="暂无人公开作品，去创作并打开「公开到广场」吧" />

      <article v-for="item in rows" :key="item.id" class="explore-card soft-card">
        <div class="explore-card__cover">
          <span class="explore-card__abbr">{{ (item.title || item.prompt || 'AI').slice(0, 2) }}</span>
        </div>
        <div class="explore-card__body">
          <h2>{{ item.title?.trim() || '未命名' }}</h2>
          <p class="explore-card__prompt">{{ item.prompt?.trim() || '—' }}</p>
          <div class="explore-card__meta">
            <span>{{ item.durationSec ?? 0 }} 秒</span>
            <span>{{ item.likeCount }} 赞</span>
          </div>
          <el-button type="primary" round size="small" @click="onPlay(item)">播放</el-button>
        </div>
      </article>
    </section>

    <div v-if="total > pager.size" class="pager-wrap">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pager.size"
        :current-page="pager.current"
        @current-change="
          (c: number) => {
            pager.current = c
            void fetchList()
          }
        "
      />
    </div>
  </div>
</template>

<style scoped>
.explore-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(17rem, 1fr));
  gap: 1.1rem;
}

.explore-card {
  display: grid;
  grid-template-columns: 4.75rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1.05rem 1.1rem;
  border-radius: 1.15rem;
  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.explore-card:hover {
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  border-color: rgba(99, 102, 241, 0.2);
}

.explore-card__cover {
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

.explore-card__abbr {
  font-size: 1.05rem;
  letter-spacing: -0.02em;
}

.explore-card__body h2 {
  margin: 0;
  font-size: 1.02rem;
  font-weight: 900;
  color: var(--melodify-strong);
  line-height: 1.25;
}

.explore-card__prompt {
  margin: 0.4rem 0;
  color: var(--melodify-muted);
  font-size: 0.84rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.explore-card__meta {
  display: flex;
  gap: 0.75rem;
  color: var(--melodify-muted);
  font-size: 0.8rem;
  margin-bottom: 0.55rem;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}
</style>
