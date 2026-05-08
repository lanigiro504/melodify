<script setup lang="ts">
/**
 * 我的作品：分页展示当前用户的 music_task；已完成项可按业务编号拉取成品试听。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getMusicAssetByBusinessTask } from '@/api/musicAssets'
import { pageMusicTasks } from '@/api/musicTasks'
import type { MusicTask } from '@/types/musicTask'
import { MUSIC_TASK_STATUS, musicTaskStatusText } from '@/types/musicTask'
import { usePlayerStore } from '@/stores/player'
import { unwrapResult } from '@/utils/apiResult'
import { formatDateTimeZh } from '@/utils/formatDateTime'
import { downloadAudioByFileUrl } from '@/utils/downloadAudio'
import { showSubmitError } from '@/utils/showSubmitError'
import { extractTrackLyrics } from '@/utils/trackLyrics'

defineOptions({ name: 'WorksPage' })

const loading = ref(false)
const rows = ref<MusicTask[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 10 })
const statusFilter = ref<number | 'all'>('all')
const router = useRouter()
const player = usePlayerStore()

/** 业务 task_id → 成品缓存（详情页跳转、底部播放器共用） */
const taskPreviewCache = reactive<
  Record<string, { assetId: number; fileUrl: string; durationSec?: number | null }>
>({})
const fetchingPreview = reactive<Record<string, boolean>>({})
const downloadingBiz = reactive<Record<string, boolean>>({})

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '生成中', value: MUSIC_TASK_STATUS.GENERATING },
  { label: '已完成', value: MUSIC_TASK_STATUS.SUCCEEDED },
  { label: '失败', value: MUSIC_TASK_STATUS.FAILED },
]

const visibleRows = computed(() => {
  if (statusFilter.value === 'all') return rows.value
  return rows.value.filter((row) => row.status === statusFilter.value)
})

const succeededCount = computed(
  () => rows.value.filter((row) => row.status === MUSIC_TASK_STATUS.SUCCEEDED).length,
)
const generatingCount = computed(
  () => rows.value.filter((row) => row.status === MUSIC_TASK_STATUS.GENERATING).length,
)

const emptyDescription = computed(() =>
  rows.value.length && statusFilter.value !== 'all'
    ? '当前筛选下没有符合条件的任务'
    : '暂无作品，先去创作一首歌吧',
)

async function fetchList() {
  loading.value = true
  try {
    const page = unwrapResult(await pageMusicTasks(pager.current, pager.size))
    rows.value = page.records
    total.value = page.total
  } catch (e) {
    showSubmitError(e, '加载生成记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => void fetchList())

function onPagerChange(cur: number) {
  pager.current = cur
  void fetchList()
}

function shorten(s: string | null | undefined, max: number) {
  const t = (s ?? '').trim()
  return t.length <= max ? t : `${t.slice(0, max)}…`
}

function statusClass(status: number | null | undefined) {
  if (status === MUSIC_TASK_STATUS.SUCCEEDED) return 'success'
  if (status === MUSIC_TASK_STATUS.FAILED) return 'danger'
  if (status === MUSIC_TASK_STATUS.GENERATING) return 'warning'
  return 'info'
}

async function ensureTaskPreview(task: MusicTask) {
  const biz = task.taskId
  const cached = taskPreviewCache[biz]
  if (cached) return cached
  fetchingPreview[biz] = true
  try {
    const asset = unwrapResult(await getMusicAssetByBusinessTask(biz))
    const entry = {
      assetId: asset.id,
      fileUrl: asset.fileUrl,
      durationSec: asset.durationSec,
    }
    taskPreviewCache[biz] = entry
    return entry
  } finally {
    fetchingPreview[biz] = false
  }
}

async function playPreview(task: MusicTask) {
  try {
    const p = await ensureTaskPreview(task)
    player.playTrack({
      title: shorten(task.prompt ?? '未命名作品', 160) || '未命名作品',
      fileUrl: p.fileUrl,
      subtitle: task.modelCode ?? '',
      lyrics: extractTrackLyrics(task.prompt ?? '', task.params ?? null) || undefined,
      durationSec: p.durationSec ?? undefined,
    })
  } catch (e) {
    showSubmitError(e, '成品暂不可用，请稍后重试')
  }
}

async function openDetail(task: MusicTask) {
  try {
    const p = await ensureTaskPreview(task)
    await router.push(`/works/${p.assetId}`)
  } catch (e) {
    showSubmitError(e, '成品暂不可用，请稍后重试')
  }
}

async function downloadPreview(task: MusicTask) {
  const biz = task.taskId
  try {
    const p = await ensureTaskPreview(task)
    downloadingBiz[biz] = true
    try {
      await downloadAudioByFileUrl(
        p.fileUrl,
        shorten(task.prompt ?? '未命名作品', 160) || '未命名作品',
      )
    } finally {
      downloadingBiz[biz] = false
    }
  } catch (e) {
    downloadingBiz[biz] = false
    showSubmitError(e, '下载失败，请稍后重试')
  }
}
</script>

<template>
  <div class="page-stack">
    <header class="works-head page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Library</p>
        <h1 class="page-title page-title--lg">我的作品</h1>
        <p class="page-desc page-desc--wide">
          按时间倒序列出你的生成任务；完成后可试听（底部播放条）、下载成片或查看详情。成片音频以链接形式存库，文件可能在
          Suno 侧或由后台镜像到本服务的 <code>/api/media/audio/</code> 目录。
        </p>
      </div>
      <div class="head-actions">
        <el-button round :loading="loading" @click="fetchList">刷新</el-button>
        <RouterLink to="/generate" class="primary-pill-link">去创作</RouterLink>
      </div>
    </header>

    <section class="section-grid section-grid--3">
      <div class="summary-card soft-card">
        <span>当前页任务</span>
        <strong>{{ rows.length }}</strong>
      </div>
      <div class="summary-card soft-card">
        <span>已完成</span>
        <strong>{{ succeededCount }}</strong>
      </div>
      <div class="summary-card soft-card">
        <span>生成中</span>
        <strong>{{ generatingCount }}</strong>
      </div>
    </section>

    <section class="works-card melodify-glass-card" v-loading="loading">
      <div class="toolbar">
        <el-segmented v-model="statusFilter" :options="statusOptions" />
        <span class="toolbar-count">共 {{ total }} 条记录</span>
      </div>

      <el-empty v-if="!visibleRows.length && !loading" :description="emptyDescription">
        <template v-if="rows.length && statusFilter !== 'all'">
          <el-button type="primary" link @click="statusFilter = 'all'">显示全部状态</el-button>
        </template>
        <RouterLink v-else class="empty-link" to="/generate">开始创作</RouterLink>
      </el-empty>

      <div v-else class="work-list">
        <article v-for="row in visibleRows" :key="row.id" class="work-item">
          <div class="cover" :class="`cover--${statusClass(row.status)}`">
            <span>{{ row.modelCode?.slice(0, 2) || 'AI' }}</span>
          </div>

          <div class="work-main">
            <div class="work-body">
              <div class="work-title-row">
                <div class="title-stack">
                  <h2>{{ shorten(row.prompt || '未命名作品', 36) }}</h2>
                  <div class="time-row">
                    <span class="time-label">创建于</span>
                    <time :datetime="row.createTime ?? undefined" class="time-value">
                      {{ formatDateTimeZh(row.createTime) }}
                    </time>
                  </div>
                </div>
                <el-tag round size="small" effect="plain" class="status-tag" :type="statusClass(row.status)">
                  {{ musicTaskStatusText(row.status ?? undefined) }}
                </el-tag>
              </div>

              <p class="prompt-text">{{ shorten(row.prompt ?? '暂无提示词', 120) }}</p>

              <div class="meta-row">
                <div class="meta-facts">
                  <span class="meta-pair"><span class="meta-label">模型</span>{{ row.modelCode || '—' }}</span>
                  <span aria-hidden="true" class="meta-dot">·</span>
                  <span class="meta-pair"><span class="meta-label">消耗</span>{{ row.costPoints ?? '—' }} 积分</span>
                </div>
                <div class="meta-actions">
                  <button
                    v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED"
                    type="button"
                    :disabled="!!fetchingPreview[row.taskId]"
                    @click.stop="playPreview(row)"
                  >
                    {{ fetchingPreview[row.taskId] ? '加载中…' : '收听' }}
                  </button>
                  <button
                    v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED"
                    type="button"
                    :disabled="!!fetchingPreview[row.taskId] || !!downloadingBiz[row.taskId]"
                    @click.stop="downloadPreview(row)"
                  >
                    {{ downloadingBiz[row.taskId] ? '下载中…' : '下载' }}
                  </button>
                  <button
                    v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED"
                    type="button"
                    @click="openDetail(row)"
                  >
                    查看详情
                  </button>
                </div>
              </div>
            </div>

            <div
              v-if="
                row.status === MUSIC_TASK_STATUS.FAILED ||
                row.status === MUSIC_TASK_STATUS.QUEUED ||
                row.status === MUSIC_TASK_STATUS.GENERATING
              "
              class="work-footer"
            >
              <p v-if="row.status === MUSIC_TASK_STATUS.FAILED" class="err-cell">
                {{ [row.errorCode, row.errorMessage].filter(Boolean).join(': ') || '未知失败原因' }}
              </p>

              <p
                v-else-if="
                  row.status === MUSIC_TASK_STATUS.QUEUED || row.status === MUSIC_TASK_STATUS.GENERATING
                "
                class="work-footer-hint"
              >
                生成完成后可「收听」「下载」或前往详情。
              </p>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > pager.size" class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pager.size"
          :current-page="pager.current"
          @current-change="onPagerChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.works-head {
  flex-wrap: wrap;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.summary-card {
  padding: 1.1rem 1.25rem;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--melodify-shadow-hover);
}

.summary-card span {
  display: block;
  color: var(--melodify-muted);
}

.summary-card strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--melodify-strong);
  font-size: 1.8rem;
  font-weight: 900;
}

.works-card {
  padding: 1rem;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.toolbar-count {
  color: var(--melodify-muted);
  font-size: 0.9rem;
}

.empty-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.45rem 0.85rem;
  border-radius: var(--melodify-radius-sm);
  color: var(--el-color-primary);
  font-weight: 600;
  text-decoration: none;
}

.empty-link:hover {
  text-decoration: underline;
}

.empty-link:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

.work-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-radius: 1.1rem;
  border: 1px solid var(--melodify-divider-strong, rgba(58, 48, 40, 0.12));
  background: transparent;
  overflow: hidden;
  box-shadow: none;
}

.work-item {
  display: grid;
  grid-template-columns: 5rem minmax(0, 1fr);
  gap: 1.1rem;
  align-items: start;
  padding: 1.2rem 1.15rem;
  border-radius: 0;
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--melodify-divider, rgba(58, 48, 40, 0.1));
  transition: background-color 0.15s ease;
}

.work-item:last-of-type {
  border-bottom: none;
}

.work-item:hover {
  background-color: var(--melodify-surface-muted, #f4f4f5);
}

.cover {
  width: 5rem;
  height: 5rem;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 1.1rem;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 88%, var(--melodify-surface-sunken));
  box-shadow:
    inset 0 0 0 1px rgba(var(--melodify-primary-rgb), 0.12),
    0 1px 3px rgba(38, 31, 26, 0.04);
}

.cover span {
  font-weight: 700;
}

.cover--success {
  color: #059669;
  background: #ecfdf5;
}

.cover--warning {
  color: #b45309;
  background: #fffbeb;
}

.cover--danger {
  color: #e11d48;
  background: #fff1f2;
}

.work-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
}

.work-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

.work-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.875rem;
}

.title-stack {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.work-title-row h2 {
  margin: 0;
  color: var(--melodify-classical-ink, var(--melodify-strong));
  font-size: 1.06rem;
  font-weight: 700;
  line-height: 1.42;
  letter-spacing: -0.02em;
}

.time-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.time-label {
  flex: none;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--melodify-subtle);
  letter-spacing: 0.06em;
}

.time-value {
  font-size: 0.8325rem;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
  color: var(--melodify-muted);
  word-break: keep-all;
}

.status-tag {
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.prompt-text {
  margin: 0;
  padding-left: 0.65rem;
  border-left: 3px solid rgba(var(--melodify-primary-rgb), 0.22);
  color: var(--melodify-muted);
  font-size: 0.875rem;
  line-height: 1.72;
}

.meta-row {
  margin-top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  color: var(--melodify-muted);
  font-size: 0.8125rem;
}

.meta-facts {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 0.35rem;
  row-gap: 0.25rem;
}

.meta-pair .meta-label {
  margin-right: 0.2rem;
  color: var(--melodify-subtle);
  font-weight: 500;
}

.meta-dot {
  color: color-mix(in srgb, var(--melodify-subtle) 55%, #d4ccc0);
  user-select: none;
  padding: 0 0.08rem;
}

.meta-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.meta-row button {
  border: 1px solid rgba(var(--melodify-primary-rgb), 0.14);
  background: rgba(var(--melodify-primary-rgb), 0.06);
  color: var(--el-color-primary);
  font: inherit;
  font-weight: 600;
  font-size: 0.8125rem;
  cursor: pointer;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    transform 0.12s ease;
}

.meta-row button:hover {
  background: rgba(var(--melodify-primary-rgb), 0.11);
  border-color: rgba(var(--melodify-primary-rgb), 0.22);
}

.meta-row button:active {
  transform: scale(0.98);
}

.meta-row button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.meta-row button:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  transform: none;
}

.work-footer {
  margin-top: auto;
  padding-top: 0.95rem;
  margin-inline: 0;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.35rem;
  min-height: 2.75rem;
}

.work-footer-hint {
  margin: 0;
  color: var(--melodify-muted);
  font-size: 0.8125rem;
  line-height: 1.5;
}

.err-cell {
  margin: 0;
  padding: 0.35rem 0 0;
  color: var(--el-color-danger);
  font-size: 0.8325rem;
  line-height: 1.5;
}
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 720px) {
  .work-item {
    grid-template-columns: 1fr;
  }

  .cover {
    width: 100%;
    height: 4.5rem;
  }
}
</style>
