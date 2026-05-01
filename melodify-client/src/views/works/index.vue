<script setup lang="ts">
/**
 * 我的作品：分页展示当前用户的 music_task；已完成项可按业务编号拉取成品试听。
 */
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
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

/** 试听地址缓存（业务 task_id -> fileUrl） */
const previewUrls = reactive<Record<string, string>>({})
const fetchingAudio = reactive<Record<string, boolean>>({})

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
  } catch (e) {
    showSubmitError(e, '成品暂不可用，请稍后重试')
  } finally {
    fetchingAudio[biz] = false
  }
}
</script>

<template>
  <div class="works-page">
    <header class="works-head">
      <div>
        <h1 class="page-title">我的作品</h1>
        <p class="page-desc">按时间倒序列出你的生成任务；完成后可在此处试听最近一次落库的音频。</p>
      </div>
      <RouterLink to="/generate" class="to-generate">去创作</RouterLink>
    </header>

    <el-card shadow="never" class="works-card">
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无记录，先到创作页提交一次任务吧" row-key="id">
        <el-table-column prop="taskId" label="任务号" min-width="200">
          <template #default="{ row }">
            <code class="mono">{{ shorten(row.taskId, 28) }}</code>
            <el-button link type="primary" class="copy-btn" @click="copyBizId(row.taskId)">
              复制
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <span>{{ musicTaskStatusText(row.status ?? undefined) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="modelCode" label="模型" width="100" />
        <el-table-column label="积分" width="72" align="right">
          <template #default="{ row }">{{ row.costPoints ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="提示词摘要" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ shorten(row.prompt ?? '', 56) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="168" />
        <el-table-column label="试听" width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === MUSIC_TASK_STATUS.SUCCEEDED">
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
                link
                :loading="!!fetchingAudio[row.taskId]"
                @click="loadPreview(row)"
              >
                加载音频
              </el-button>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.status === MUSIC_TASK_STATUS.FAILED" class="err-cell">
              {{ [row.errorCode, row.errorMessage].filter(Boolean).join(': ') || '—' }}
            </span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 0.35rem;
}

.page-desc {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--el-text-color-secondary);
  max-width: 40rem;
}

.works-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.to-generate {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--el-color-primary);
  white-space: nowrap;
}

.to-generate:hover {
  text-decoration: underline;
}

.works-card {
  border-radius: var(--melodify-radius-lg, 12px);
}

.mono {
  font-size: 0.8125rem;
  word-break: break-all;
}

.copy-btn {
  margin-left: 0.25rem;
  vertical-align: baseline;
}

.preview-audio {
  width: 100%;
  max-width: 220px;
  height: 32px;
  vertical-align: middle;
}

.err-cell {
  color: var(--el-color-danger);
  font-size: 0.875rem;
}

.muted {
  color: var(--el-text-color-secondary);
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}
</style>
