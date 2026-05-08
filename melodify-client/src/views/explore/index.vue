<script setup lang="ts">
/**
 * 公开广场：试听、关键词检索、最热排序、登录用户点赞（与详情页共用 API）。
 */
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { pageExploreAssets, type ExploreAssetItem, type ExploreSortMode } from '@/api/explore'
import { likeMusicAsset, unlikeMusicAsset } from '@/api/musicAssets'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { unwrapResult } from '@/utils/apiResult'
import { downloadAudioByFileUrl } from '@/utils/downloadAudio'
import { exploreItemPlaySubtitle } from '@/utils/exploreDisplay'
import { extractTrackLyrics } from '@/utils/trackLyrics'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'ExplorePage' })

const router = useRouter()
const player = usePlayerStore()
const { isAuthenticated } = storeToRefs(useAuthStore())

const loading = ref(false)
const rows = ref<ExploreAssetItem[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 12 })
const filters = reactive({ keyword: '', sort: 'NEWEST' as ExploreSortMode })

const EXPLORE_PAGE_SIZE_MAX = 48

const likeBusyId = ref<number | null>(null)
const downloadBusyId = ref<number | null>(null)

let keywordDebounce: ReturnType<typeof setTimeout> | null = null

async function fetchList() {
  loading.value = true
  try {
    const page = unwrapResult(
      await pageExploreAssets(
        Math.max(1, pager.current),
        Math.min(EXPLORE_PAGE_SIZE_MAX, Math.max(1, pager.size)),
        { keyword: filters.keyword.trim() || undefined, sort: filters.sort },
      ),
    )
    rows.value = page.records.map((r) => ({
      ...r,
      liked: Boolean(r.liked),
      likeCount: Number(r.likeCount) || 0,
    }))
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

watch(
  () => filters.sort,
  () => {
    pager.current = 1
    void fetchList()
  },
)

watch(
  () => filters.keyword,
  () => {
    if (keywordDebounce) clearTimeout(keywordDebounce)
    keywordDebounce = setTimeout(() => {
      keywordDebounce = null
      pager.current = 1
      void fetchList()
    }, 420)
  },
)

watch(isAuthenticated, () => {
  void fetchList()
})

const applySearch = () => {
  if (keywordDebounce) {
    clearTimeout(keywordDebounce)
    keywordDebounce = null
  }
  pager.current = 1
  void fetchList()
}

const toggleLike = async (item: ExploreAssetItem) => {
  if (!isAuthenticated.value) {
    void router.push({ path: '/login', query: { redirect: '/explore' } })
    return
  }
  const wasLiked = Boolean(item.liked)
  likeBusyId.value = item.id
  try {
    if (wasLiked) {
      unwrapResult(await unlikeMusicAsset(item.id))
      item.liked = false
      item.likeCount = Math.max(0, item.likeCount - 1)
    } else {
      unwrapResult(await likeMusicAsset(item.id))
      item.liked = true
      item.likeCount = item.likeCount + 1
    }
  } catch (e) {
    showSubmitError(e, wasLiked ? '取消点赞失败' : '点赞失败')
  } finally {
    likeBusyId.value = null
  }
}

const onPageSizeChange = () => {
  pager.current = 1
  void fetchList()
}

const clearKeywordAndSearch = () => {
  filters.keyword = ''
  applySearch()
}

const onDownload = async (item: ExploreAssetItem) => {
  if (!item.fileUrl?.trim()) {
    ElMessage.warning('暂无可下载地址')
    return
  }
  const name = item.title?.trim() || item.prompt?.trim() || '未命名作品'
  downloadBusyId.value = item.id
  try {
    await downloadAudioByFileUrl(item.fileUrl, name)
  } catch (e) {
    showSubmitError(e, '下载失败，请稍后重试')
  } finally {
    downloadBusyId.value = null
  }
}

onMounted(() => void fetchList())
</script>

<template>
  <div class="page-stack explore-page">
    <section class="explore-intro">
      <div class="explore-intro__head">
        <div>
          <p class="page-eyebrow">作品广场</p>
          <h1 class="page-title page-title--lg">浏览公开作品</h1>
          <p class="page-desc page-desc--wide">任意公开作品可免费试听；登录后可点赞，支持关键词与最热排序。</p>
        </div>
      </div>
      <div class="explore-toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="标题或描述关键词"
          clearable
          class="explore-search"
          :prefix-icon="Search"
          @keyup.enter="applySearch"
          @clear="applySearch"
        />
        <el-select v-model="filters.sort" class="explore-sort" aria-label="排序方式">
          <el-option label="最新发布" value="NEWEST" />
          <el-option label="最热（点赞）" value="LIKES" />
        </el-select>
        <el-select v-model="pager.size" class="explore-pagesize" @change="onPageSizeChange">
          <el-option :value="12" label="每页 12 条" />
          <el-option :value="24" label="每页 24 条" />
          <el-option :value="36" label="每页 36 条" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="applySearch">搜索</el-button>
        <el-button :loading="loading" @click="fetchList">刷新</el-button>
      </div>
    </section>

    <section v-loading="loading" class="explore-grid">
      <el-empty v-if="!rows.length && !loading" description="暂无符合条件的公开作品">
        <div class="empty-actions">
          <el-button v-if="filters.keyword.trim()" text type="primary" @click="clearKeywordAndSearch">
            清空关键词
          </el-button>
          <RouterLink class="explore-empty-cta" to="/generate">前往创作（需登录）</RouterLink>
        </div>
      </el-empty>

      <article v-for="item in rows" :key="item.id" class="explore-card">
        <div class="explore-card__cover">
          <span class="explore-card__abbr">{{ (item.title || item.prompt || 'AI').slice(0, 2) }}</span>
        </div>
        <div class="explore-card__body">
          <h2>{{ item.title?.trim() || '未命名' }}</h2>
          <p class="explore-card__prompt">{{ item.prompt?.trim() || '—' }}</p>
          <div class="explore-card__meta">
            <span>{{ item.durationSec ?? 0 }} 秒</span>
            <span class="meta-likes">
              <span class="like-dot" :class="{ 'like-dot--on': item.liked }" aria-hidden="true" />
              {{ item.likeCount }} 赞
            </span>
          </div>
          <div class="explore-card__actions">
            <el-button type="primary" size="small" @click="onPlay(item)">播放</el-button>
            <el-button size="small" :loading="downloadBusyId === item.id" @click="onDownload(item)">
              下载
            </el-button>
            <template v-if="isAuthenticated">
              <el-button
                size="small"
                link
                :type="item.liked ? 'primary' : 'default'"
                :loading="likeBusyId === item.id"
                @click="toggleLike(item)"
              >
                {{ item.liked ? '已赞' : '点赞' }}
              </el-button>
            </template>
            <RouterLink v-else class="like-login-hint" to="/login?redirect=/explore">登录后点赞</RouterLink>
          </div>
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
.explore-intro {
  padding-bottom: 1.25rem;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--melodify-divider-strong, var(--melodify-border));
}

.explore-intro__head {
  margin-bottom: 1rem;
}

.explore-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(17.5rem, 1fr));
  gap: 1rem;
}

.explore-card {
  display: grid;
  grid-template-columns: 4rem minmax(0, 1fr);
  gap: 0.875rem;
  padding: 1rem;
  border-radius: var(--melodify-radius-lg, 12px);
  border: 1px solid var(--melodify-divider-strong, rgba(58, 48, 40, 0.12));
  background: transparent;
  box-shadow: none;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background-color 0.18s ease;
}

.explore-card:hover {
  background: var(--melodify-surface-muted, #f5f5f5);
  border-color: rgba(var(--melodify-primary-rgb), 0.22);
  box-shadow: var(--melodify-shadow-hover);
  transform: translateY(-2px);
}

.explore-card__cover {
  width: 4rem;
  height: 4rem;
  border-radius: var(--melodify-radius-sm, 8px);
  display: grid;
  place-items: center;
  background: #fafafa;
  color: var(--el-color-primary);
  font-weight: 700;
  font-size: 0.95rem;
  border: 1px solid var(--melodify-divider, rgba(0, 0, 0, 0.06));
  transition: background-color 0.18s ease;
}

.explore-card:hover .explore-card__cover {
  background: var(--el-color-primary-light-9);
}

.explore-card__abbr {
  letter-spacing: -0.02em;
}

.explore-card__body h2 {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--melodify-strong);
  line-height: 1.3;
}

.explore-card__prompt {
  margin: 0.35rem 0 0;
  color: var(--melodify-muted);
  font-size: 0.8125rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.explore-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  color: var(--melodify-muted);
  font-size: 0.75rem;
  margin: 0.5rem 0 0;
}

.meta-likes {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.like-dot {
  width: 0.35rem;
  height: 0.35rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--melodify-muted) 35%, #ccc4b8);
}

.like-dot--on {
  background: var(--el-color-primary);
}

.explore-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem 0.75rem;
  align-items: center;
  margin-top: 0.65rem;
}

.like-login-hint {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--el-color-primary);
  text-decoration: none;
}

.like-login-hint:hover {
  text-decoration: underline;
}

.empty-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: center;
}

.explore-empty-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.48rem 0.95rem;
  margin-top: 0.35rem;
  border-radius: var(--melodify-radius-sm);
  font-weight: 700;
  font-size: 0.875rem;
  color: var(--el-color-primary);
  text-decoration: none;
}

.explore-empty-cta:hover {
  text-decoration: underline;
}

.explore-empty-cta:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

.explore-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.explore-search {
  flex: 1 1 200px;
  min-width: min(100%, 220px);
}

.explore-sort,
.explore-pagesize {
  width: 132px;
}
</style>
