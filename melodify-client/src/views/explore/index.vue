<script setup lang="ts">
/**
 * 公开广场：无需登录即可浏览 isPublic=1 的作品并发起到全局播放器。
 */
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { pageExploreAssets, type ExploreAssetItem } from '@/api/explore'
import { usePlayerStore } from '@/stores/player'
import { unwrapResult } from '@/utils/apiResult'
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
    subtitle: item.prompt?.trim() || undefined,
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

      <article v-for="item in rows" :key="item.id" class="card soft-card">
        <div class="cover">
          <span>{{ (item.title || item.prompt || 'AI').slice(0, 2) }}</span>
        </div>
        <div class="body">
          <h2>{{ item.title?.trim() || '未命名' }}</h2>
          <p class="prompt">{{ item.prompt?.trim() || '—' }}</p>
          <div class="meta">
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
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 1rem;
}

.card {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  gap: 0.85rem;
  padding: 1rem;
}

.cover {
  width: 4.5rem;
  height: 4.5rem;
  border-radius: 1rem;
  display: grid;
  place-items: center;
  background: #f5f3ff;
  color: #6d5dfc;
  font-weight: 900;
}

.body h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--melodify-strong);
}

.prompt {
  margin: 0.45rem 0;
  color: var(--melodify-muted);
  font-size: 0.86rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  gap: 0.75rem;
  color: var(--melodify-muted);
  font-size: 0.82rem;
  margin-bottom: 0.55rem;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}
</style>
