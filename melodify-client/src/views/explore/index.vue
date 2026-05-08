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

onMounted(() => void fetchList())
</script>

<template>
  <div class="page-stack explore-page">
    <section class="melodify-glass-card explore-hero">
      <div class="explore-hero-head">
        <div>
          <p class="page-eyebrow">Explore</p>
          <h1 class="page-title page-title--lg">作品广场</h1>
          <p class="page-desc page-desc--wide">
            公开作品可<strong>免费试听</strong>；登录后可<strong>点赞</strong>。支持标题/描述搜索与按热度排序。
          </p>
        </div>
      </div>
      <div class="explore-toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索标题或创作描述（自动搜索，也可按回车立即查询）"
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
        <el-button type="primary" round :loading="loading" @click="applySearch">立即搜索</el-button>
        <el-button round :loading="loading" @click="fetchList">刷新</el-button>
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

      <article v-for="item in rows" :key="item.id" class="explore-card soft-card">
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
            <el-button type="primary" round size="small" @click="onPlay(item)">播放</el-button>
            <template v-if="isAuthenticated">
              <el-button
                round
                size="small"
                :type="item.liked ? 'warning' : 'default'"
                :plain="!item.liked"
                :loading="likeBusyId === item.id"
                @click="toggleLike(item)"
              >
                {{ item.liked ? '已点赞' : '点赞' }}
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
.explore-hero {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.explore-hero-head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.explore-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(17.5rem, 1fr));
  gap: 1.15rem;
  margin-top: 1rem;
}

.explore-card {
  display: grid;
  grid-template-columns: 4.75rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1.05rem 1.15rem;
  border-radius: 1.15rem;
  transition:
    box-shadow 0.22s ease,
    border-color 0.22s ease,
    transform 0.18s ease;
}

.explore-card:hover {
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.1);
  border-color: rgba(99, 102, 241, 0.22);
  transform: translateY(-2px);
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
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
  color: var(--melodify-muted);
  font-size: 0.8rem;
  margin-bottom: 0.55rem;
}

.meta-likes {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.like-dot {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.75);
}

.like-dot--on {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.2);
}

.explore-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  align-items: center;
}

.like-login-hint {
  font-size: 0.8rem;
  font-weight: 800;
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
  display: inline-block;
  font-weight: 800;
  color: var(--el-color-primary);
  text-decoration: none;
}

.explore-empty-cta:hover {
  text-decoration: underline;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.explore-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
}

.explore-search {
  flex: 1 1 220px;
  min-width: min(100%, 240px);
}

.explore-sort {
  width: 150px;
}

.explore-pagesize {
  width: 128px;
}
</style>
