<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getMusicAssetDetail, likeMusicAsset, patchMusicAssetPublic, unlikeMusicAsset } from '@/api/musicAssets'
import { usePlayerStore } from '@/stores/player'
import { resolvePlayableUrl } from '@/utils/audioUrl'
import { extractTrackLyrics } from '@/utils/trackLyrics'
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

/** 后端存入的独立歌词（优先展示） */
const lyricsFromParams = computed(() => {
  const raw = params.value.lyrics
  return typeof raw === 'string' && raw.trim() ? raw.trim() : ''
})

const creativeDescription = computed(() => task.value?.prompt?.trim() || '')

/** 展示用歌词块：独立 lyrics 优先；从 prompt 推断时若与创作描述相同则不再重复展示 */
const lyricsBlockText = computed(() => {
  if (lyricsFromParams.value) return lyricsFromParams.value
  const inferred = extractTrackLyrics(task.value?.prompt, task.value?.params ?? null) || ''
  if (!inferred) return ''
  if (inferred === creativeDescription.value) return ''
  return inferred
})

const heroSummary = computed(() => {
  const bits: string[] = []
  if (task.value?.modelCode) bits.push(String(task.value.modelCode))
  const st = params.value.style
  if (typeof st === 'string' && st.trim()) bits.push(st.trim())
  const pt = params.value.title
  if (typeof pt === 'string' && pt.trim()) bits.push(pt.trim())
  return bits.length ? bits.join(' · ') : '作品详情'
})

const PARAM_LABELS: Record<string, string> = {
  customMode: '自定义模式',
  instrumental: '纯器乐',
  style: '风格',
  title: '标题',
  lyrics: '歌词',
  model: '模型',
  negativeTags: '负面标签',
  vocalGender: '人声性别',
  styleWeight: '风格权重',
  weirdnessConstraint: '怪异度',
  audioWeight: '音频权重',
  personaId: 'Persona',
  personaModel: 'Persona 模型',
  callBackUrl: '回调地址',
}

function formatParamValue(v: unknown): string {
  if (v === true) return '是'
  if (v === false) return '否'
  const s = String(v)
  return s.length > 120 ? `${s.slice(0, 118)}…` : s
}

const paramEntries = computed(() => {
  const p = params.value
  return Object.keys(p)
    .filter((k) => k !== 'lyrics')
    .map((k) => ({
      key: k,
      label: PARAM_LABELS[k] ?? k,
      value: formatParamValue(p[k]),
    }))
})

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
  const ly = lyricsFromParams.value
  await router.push({
    path: '/generate',
    query: {
      model: task.value?.modelCode ?? 'V4_5',
      prompt: task.value?.prompt ?? '',
      ...(ly ? { lyrics: ly } : {}),
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
    subtitle: heroSummary.value,
    lyrics: extractTrackLyrics(task.value?.prompt, task.value?.params ?? null),
    durationSec: asset.value.durationSec ?? undefined,
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
        <p class="page-desc page-desc--wide">{{ heroSummary }}</p>
      </div>
      <div class="detail-actions">
        <RouterLink to="/works" class="ghost-link">返回作品库</RouterLink>
        <el-button round @click="recreate">复用参数再创作</el-button>
        <el-button type="primary" round :loading="liking" @click="toggleLike">
          {{ detail?.liked ? '已点赞' : '点赞' }} · {{ detail?.likeCount ?? 0 }}
        </el-button>
      </div>
    </section>

    <section v-if="asset && (creativeDescription || lyricsBlockText)" class="copy-stack">
      <article v-if="creativeDescription" class="soft-card copy-card">
        <h2 class="copy-card__title">创作描述</h2>
        <p class="copy-card__body">{{ creativeDescription }}</p>
      </article>
      <article v-if="lyricsBlockText" class="soft-card copy-card">
        <h2 class="copy-card__title">歌词</h2>
        <pre class="copy-card__pre">{{ lyricsBlockText }}</pre>
      </article>
    </section>

    <section v-if="asset" class="work-player-card melodify-glass-card">
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
          <el-button type="primary" round plain @click="playInGlobalBar">用底部播放器播放</el-button>
        </div>
        <p class="player-hint">使用站内播放器可查看歌词同步（若本作品有识别到的歌词）。</p>
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
        <span v-for="row in paramEntries" :key="row.key">
          <strong>{{ row.label }}</strong>：{{ row.value }}
        </span>
        <span v-if="!paramEntries.length">暂无结构化参数</span>
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

.copy-stack {
  display: grid;
  gap: 1rem;
}

.copy-card {
  padding: 1.15rem 1.25rem;
}

.copy-card__title {
  margin: 0 0 0.65rem;
  font-size: 0.95rem;
  font-weight: 900;
  color: var(--melodify-strong);
}

.copy-card__body {
  margin: 0;
  color: var(--melodify-muted);
  line-height: 1.75;
  white-space: pre-wrap;
}

.copy-card__pre {
  margin: 0;
  font-family: inherit;
  font-size: 0.9rem;
  line-height: 1.65;
  color: var(--melodify-strong);
  white-space: pre-wrap;
  word-break: break-word;
}

.work-player-card {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr);
  gap: 1.25rem;
  padding: 1.25rem;
}

.player-hint {
  margin: 0 0 0.5rem;
  font-size: 0.82rem;
  color: var(--melodify-muted);
}

.cover-art {
  width: 8rem;
  height: 8rem;
  display: grid;
  place-items: center;
  border-radius: 1.5rem;
  background: linear-gradient(145deg, #f5f3ff, #eef2ff);
  color: #6d5dfc;
  font-size: 2rem;
  font-weight: 900;
  border: 1px solid rgba(99, 102, 241, 0.12);
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

.param-list span strong {
  color: var(--melodify-strong);
  font-weight: 800;
}

@media (max-width: 720px) {
  .work-player-card {
    grid-template-columns: 1fr;
  }

  .cover-art {
    width: 100%;
    height: 6rem;
  }
}
</style>
