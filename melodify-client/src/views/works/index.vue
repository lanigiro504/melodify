<script setup lang="ts">
/**
 * 我的作品：分页展示当前用户的 music_task；已完成项可按业务编号拉取成品试听。
 */
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getMusicAssetByBusinessTask } from '@/api/musicAssets'
import { pageMusicTasks } from '@/api/musicTasks'
import type { MusicTask } from '@/types/musicTask'
import { MUSIC_TASK_STATUS, musicTaskStatusText } from '@/types/musicTask'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'WorksPage' })

const loading = ref(false)
const rows = ref<MusicTask[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 10 })
const statusFilter = ref<number | 'all'>('all')
const router = useRouter()

/** 试听地址缓存（业务 task_id -> fileUrl） */
const previewUrls = reactive<Record<string, string>>({})
const detailIds = reactive<Record<string, number>>({})
const fetchingAudio = reactive<Record<string, boolean>>({})

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

function copyBizId(taskId: string) {
  void navigator.clipboard.writeText(taskId).then(
    () => ElMessage.success('任务号已复制'),
    () => ElMessage.warning('复制失败，请手动选择复制'),
  )
}

async function loadPreview(task: MusicTask) {
  const biz = task.taskId
  if (previewUrls[biz]) return
  fetchingAudio[biz] = true
  try {
    const asset = unwrapResult(await getMusicAssetByBusinessTask(biz))
    previewUrls[biz] = asset.fileUrl
    detailIds[biz] = asset.id
  } catch (e) {
    showSubmitError(e, '成品暂不可用，请稍后重试')
  } finally {
    fetchingAudio[biz] = false
  }
}

async function openDetail(task: MusicTask) {
  if (!detailIds[task.taskId]) {
    await loadPreview(task)
  }
  const id = detailIds[task.taskId]
  if (id) {
    await router.push(`/works/${id}`)
  }
}
</script>

<template>
  <div class="page-stack">
    <header class="works-head page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Library</p>
        <h1 class="page-title page-title--lg">我的作品</h1>
        <p class="page-desc page-desc--wide">按时间倒序列出你的生成任务；完成后可在此处试听、复制任务号和查看失败原因。</p>
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

      <el-empty v-if="!visibleRows.length && !loading" description="暂无匹配记录，先去创作一首歌吧">
        <RouterLink to="/generate" class="empty-link">开始创作</RouterLink>
      </el-empty>

      <div v-else class="work-list">
        <article v-for="row in visibleRows" :key="row.id" class="work-item">
          <div class="cover" :class="`cover--${statusClass(row.status)}`">
            <span>{{ row.modelCode?.slice(0, 2) || 'AI' }}</span>
          </div>

          <div class="work-main">
            <div class="work-title-row">
              <div>
                <h2>{{ shorten(row.prompt || '未命名作品', 30) }}</h2>
                <p>{{ row.createTime || '—' }}</p>
              </div>
              <el-tag round :type="statusClass(row.status)">
                {{ musicTaskStatusText(row.status ?? undefined) }}
              </el-tag>
            </div>

            <p class="prompt-text">{{ shorten(row.prompt ?? '暂无提示词', 120) }}</p>

            <div class="meta-row">
              <span>模型 {{ row.modelCode || '—' }}</span>
              <span>消耗 {{ row.costPoints ?? '—' }} 积分</span>
              <button type="button" @click="copyBizId(row.taskId)">复制任务号</button>
              <button
                v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED"
                type="button"
                @click="openDetail(row)"
              >
                查看详情
              </button>
            </div>

            <div v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED" class="audio-row">
              <audio
                v-if="previewUrls[row.taskId]"
                controls
                class="preview-audio"
                preload="none"
                :src="previewUrls[row.taskId]"
              />
              <el-button
                v-else
                type="primary"
                round
                :loading="!!fetchingAudio[row.taskId]"
                @click="loadPreview(row)"
              >
                加载试听
              </el-button>
            </div>

            <p v-if="row.status === MUSIC_TASK_STATUS.FAILED" class="err-cell">
              {{ [row.errorCode, row.errorMessage].filter(Boolean).join(': ') || '未知失败原因' }}
            </p>
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
  color: var(--el-color-primary);
  font-weight: 800;
}

.work-list {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.work-item {
  display: grid;
  grid-template-columns: 5rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 1.35rem;
  background: #ffffff;
}

.cover {
  width: 5rem;
  height: 5rem;
  display: grid;
  place-items: center;
  border-radius: 1.25rem;
  color: #6d5dfc;
  background: #f5f3ff;
  box-shadow: inset 0 0 0 1px rgba(109, 93, 252, 0.12);
}

.cover span {
  font-weight: 900;
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

.work-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.work-title-row h2 {
  margin: 0;
  color: var(--melodify-strong);
  font-size: 1.08rem;
  font-weight: 900;
}

.work-title-row p {
  margin-top: 0.2rem;
  color: var(--melodify-muted);
  font-size: 0.85rem;
}

.prompt-text {
  margin: 0.75rem 0;
  color: var(--melodify-muted);
  line-height: 1.65;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.6rem 0.9rem;
  color: var(--melodify-muted);
  font-size: 0.86rem;
}

.meta-row button {
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  padding: 0;
}

.audio-row {
  margin-top: 0.85rem;
}

.preview-audio {
  width: 100%;
  max-width: 32rem;
  vertical-align: middle;
}

.err-cell {
  color: var(--el-color-danger);
  font-size: 0.875rem;
  margin: 0.85rem 0 0;
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
