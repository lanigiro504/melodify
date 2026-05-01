<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getMusicAssetDetail, likeMusicAsset, patchMusicAssetPublic, unlikeMusicAsset } from '@/api/musicAssets'
import { usePlayerStore } from '@/stores/player'
import { resolvePlayableUrl } from '@/utils/audioUrl'
import type { MusicAssetDetail } from '@/types/musicAsset'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'WorkDetailPage' })

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const loading = ref(false)
const liking = ref(false)
const publishing = ref(false)
const detail = ref<MusicAssetDetail | null>(null)

const asset = computed(() => detail.value?.asset)
const task = computed(() => detail.value?.task)
const title = computed(() => asset.value?.title || task.value?.prompt || '未命名作品')
const params = computed<Record<string, unknown>>(() => task.value?.params ?? {})

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    ElMessage.error('作品 ID 不正确')
    return
  }
  loading.value = true
  try {
    detail.value = unwrapResult(await getMusicAssetDetail(id))
  } catch (e) {
    showSubmitError(e, '加载作品详情失败')
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!detail.value || !asset.value) return
  liking.value = true
  try {
    if (detail.value.liked) {
      unwrapResult(await unlikeMusicAsset(asset.value.id))
      detail.value.liked = false
      detail.value.likeCount = Math.max(0, detail.value.likeCount - 1)
    } else {
      unwrapResult(await likeMusicAsset(asset.value.id))
      detail.value.liked = true
      detail.value.likeCount += 1
    }
  } catch (e) {
    showSubmitError(e, '操作失败')
  } finally {
    liking.value = false
  }
}

async function recreate() {
  await router.push({
    path: '/generate',
    query: {
      model: task.value?.modelCode ?? 'V4_5',
      prompt: task.value?.prompt ?? '',
      style: String(params.value.style ?? ''),
      title: String(params.value.title ?? ''),
      customMode: String(Boolean(params.value.customMode)),
      instrumental: String(Boolean(params.value.instrumental)),
    },
  })
}

async function onPublicChange(val: string | number | boolean) {
  if (!asset.value || !detail.value) return
  const on = val === true || val === 'true' || val === 1 || val === '1'
  publishing.value = true
  try {
    unwrapResult(await patchMusicAssetPublic(asset.value.id, on ? 1 : 0))
    detail.value.asset.isPublic = on ? 1 : 0
    ElMessage.success(on ? '已公开到广场' : '已从广场下架')
  } catch (e) {
    showSubmitError(e, '更新失败')
  } finally {
    publishing.value = false
  }
}

function playInGlobalBar() {
  if (!asset.value?.fileUrl) return
  player.playTrack({
    title: title.value,
    fileUrl: asset.value.fileUrl,
    subtitle: task.value?.prompt ?? undefined,
  })
}

onMounted(() => void fetchDetail())
</script>

<template>
  <div class="page-stack" v-loading="loading">
    <section class="page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Work Detail</p>
        <h1 class="page-title page-title--lg">{{ title }}</h1>
        <p class="page-desc page-desc--wide">{{ task?.prompt || '暂无提示词' }}</p>
      </div>
      <div class="detail-actions">
        <RouterLink to="/works" class="ghost-link">返回作品库</RouterLink>
        <el-button round @click="recreate">复用参数再创作</el-button>
        <el-button type="primary" round :loading="liking" @click="toggleLike">
          {{ detail?.liked ? '已点赞' : '点赞' }} · {{ detail?.likeCount ?? 0 }}
        </el-button>
      </div>
    </section>

    <section v-if="asset" class="player-card melodify-glass-card">
      <div class="cover-art">
        <span>{{ (asset.title || 'AI').slice(0, 2) }}</span>
      </div>
      <div class="player-main">
        <h2>{{ asset.title || 'AI 生成音乐' }}</h2>
        <p>时长 {{ asset.durationSec || 0 }} 秒 · {{ asset.format || 'mp3' }}</p>
        <div class="player-row">
          <el-switch
            :model-value="(asset.isPublic ?? 0) === 1"
            :loading="publishing"
            active-text="公开到广场"
            inactive-text="仅自己"
            @change="onPublicChange"
          />
          <el-button type="primary" round plain @click="playInGlobalBar">用底部栏播放</el-button>
        </div>
        <audio controls preload="none" :src="resolvePlayableUrl(asset.fileUrl)" />
      </div>
    </section>

    <section class="section-grid section-grid--3">
      <div class="soft-card info-card">
        <span>模型</span>
        <strong>{{ task?.modelCode || '—' }}</strong>
      </div>
      <div class="soft-card info-card">
        <span>消耗积分</span>
        <strong>{{ task?.costPoints ?? '—' }}</strong>
      </div>
      <div class="soft-card info-card">
        <span>创建时间</span>
        <strong>{{ asset?.createTime || '—' }}</strong>
      </div>
    </section>

    <section class="melodify-glass-card params-card">
      <h2>生成参数</h2>
      <div class="param-list">
        <span v-for="(value, key) in params" :key="key">
          {{ key }}：{{ String(value) }}
        </span>
        <span v-if="!Object.keys(params).length">暂无结构化参数</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.ghost-link {
  color: var(--melodify-muted);
  font-weight: 800;
}

.player-card {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr);
  gap: 1.25rem;
  padding: 1.25rem;
}

.cover-art {
  width: 8rem;
  height: 8rem;
  display: grid;
  place-items: center;
  border-radius: 1.5rem;
  background: #f5f3ff;
  color: #6d5dfc;
  font-size: 2rem;
  font-weight: 900;
}

.player-main h2 {
  margin: 0;
  color: var(--melodify-strong);
}

.player-main p {
  color: var(--melodify-muted);
}

.player-main audio {
  width: 100%;
  max-width: 40rem;
}

.player-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin: 0.65rem 0;
}

.info-card {
  padding: 1rem 1.2rem;
}

.info-card span {
  color: var(--melodify-muted);
}

.info-card strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--melodify-strong);
}

.params-card {
  padding: 1.25rem;
}

.params-card h2 {
  margin: 0 0 1rem;
  color: var(--melodify-strong);
}

.param-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.param-list span {
  padding: 0.45rem 0.7rem;
  border-radius: 999px;
  background: #f8fafc;
  color: var(--melodify-muted);
  font-size: 0.9rem;
}

@media (max-width: 720px) {
  .player-card {
    grid-template-columns: 1fr;
  }

  .cover-art {
    width: 100%;
    height: 6rem;
  }
}
</style>
