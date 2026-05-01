<script setup lang="ts">
/**
 * AI 创作：提交生成请求 → 轮询任务 → 成功后拉取成品 URL 并用 audio 试听。
 */
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getMusicAssetByBusinessTask } from '@/api/musicAssets'
import { getMusicTaskByBusinessId, submitMusicGenerate } from '@/api/musicTasks'
import { usePlayerStore } from '@/stores/player'
import { MUSIC_TASK_STATUS, musicTaskStatusText } from '@/types/musicTask'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { extractTrackLyrics } from '@/utils/trackLyrics'
import { validateFormRef } from '@/utils/validateFormRef'

defineOptions({ name: 'GenerateMusicPage' })

const POLL_MS = 4000
const route = useRoute()
const player = usePlayerStore()

const modelOptions = [
  { label: 'V5_5', value: 'V5_5', desc: '最新模型，适合高质量成曲' },
  { label: 'V5', value: 'V5', desc: '综合能力稳定' },
  { label: 'V4_5PLUS', value: 'V4_5PLUS', desc: '细节增强' },
  { label: 'V4_5ALL', value: 'V4_5ALL', desc: '覆盖更多风格' },
  { label: 'V4_5（推荐）', value: 'V4_5', desc: '当前推荐，成本稳定' },
  { label: 'V4', value: 'V4', desc: '兼容旧任务' },
]

const promptExamples = [
  '夏夜城市里的梦幻流行，女声，带一点电子氛围，副歌有记忆点',
  '适合咖啡馆播放的轻爵士，温暖、松弛、带钢琴与贝斯',
  '国风电子融合，描写远山与月光，节奏逐渐推进',
]

const formRef = ref<FormInstance>()
const submitting = ref(false)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const busyTaskBizId = ref<string | null>(null)
const statusLabel = ref('')
const audioUrl = ref<string | null>(null)
const errorDetail = ref('')

const form = reactive({
  modelCode: 'V4_5',
  prompt: '',
  style: '',
  title: '',
  /** 对齐 Suno 文档：自定义模式需提供 style/title；关闭时仅 prompt（建议 ≤500 字） */
  customMode: false,
  instrumental: false,
})

/** 文档：非自定义 prompt ≤500；自定义 V4 系最高 3000，其它更高；前端取宽松上限避免误判 */
const promptMaxLen = computed(() => (form.customMode ? 5000 : 500))

const statusType = computed(() => {
  if (errorDetail.value) return 'danger'
  if (audioUrl.value) return 'success'
  if (busyTaskBizId.value) return 'warning'
  return 'info'
})

const applyExample = (text: string) => {
  form.prompt = text
}

/** 自定义 + 纯器乐时 Suno 可不填 prompt */
const rules = computed<FormRules>(() => ({
  prompt: [
    {
      validator: (_r, val, cb) => {
        if (form.customMode && form.instrumental) {
          cb()
          return
        }
        if (!String(val ?? '').trim()) {
          cb(new Error('请输入提示词'))
          return
        }
        cb()
      },
      trigger: 'blur',
    },
  ],
}))

/** Suno OpenAPI：自定义模式下 instrumental=false 时需 style + title；true 时需 style + title */
const validateBusinessRules = (): boolean => {
  if (!form.customMode) return true
  if (!form.style.trim()) {
    ElMessage.warning('自定义模式下请填写音乐风格')
    return false
  }
  if (!form.title.trim()) {
    ElMessage.warning('自定义模式下请填写成品标题')
    return false
  }
  if (!form.instrumental && !form.prompt.trim()) {
    ElMessage.warning('带人声时请将歌词写入提示词（将作为精确歌词使用）')
    return false
  }
  return true
}

const stopPoll = () => {
  if (pollTimer.value != null) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

onUnmounted(() => stopPoll())

function applyRouteQuery() {
  const q = route.query
  if (typeof q.model === 'string' && q.model.trim()) form.modelCode = q.model.trim()
  if (typeof q.prompt === 'string') form.prompt = q.prompt
  if (typeof q.style === 'string') form.style = q.style
  if (typeof q.title === 'string') form.title = q.title
  if (q.customMode === 'true') form.customMode = true
  if (q.customMode === 'false') form.customMode = false
  if (q.instrumental === 'true') form.instrumental = true
  if (q.instrumental === 'false') form.instrumental = false
}

watch(() => route.query, applyRouteQuery, { immediate: true })

const pollOnce = async (taskBizId: string): Promise<boolean> => {
  try {
    const task = unwrapResult(await getMusicTaskByBusinessId(taskBizId))
    statusLabel.value = musicTaskStatusText(task.status ?? undefined)

    const st = task.status
    if (st === MUSIC_TASK_STATUS.SUCCEEDED) {
      stopPoll()
      try {
        const asset = unwrapResult(await getMusicAssetByBusinessTask(taskBizId))
        audioUrl.value = asset.fileUrl
        player.playTrack({
          title: form.title.trim() || form.prompt.trim().slice(0, 48) || '新作品',
          fileUrl: asset.fileUrl,
          subtitle: form.modelCode,
          lyrics: extractTrackLyrics(form.prompt, null),
          durationSec: asset.durationSec ?? undefined,
        })
        ElMessage.success('生成完成，可以试听')
      } catch (e) {
        showSubmitError(e, '已完成但暂无法加载音频，请稍后在作品列表中查看')
      }
      submitting.value = false
      busyTaskBizId.value = null
      return true
    }

    if (st === MUSIC_TASK_STATUS.FAILED) {
      stopPoll()
      errorDetail.value =
        [task.errorCode, task.errorMessage].filter(Boolean).join(': ') || '未知错误'
      ElMessage.error('生成失败')
      submitting.value = false
      busyTaskBizId.value = null
      return true
    }

    return false
  } catch {
    statusLabel.value = '轮询暂时失败，将自动重试…'
    return false
  }
}

const startPoll = (taskBizId: string) => {
  busyTaskBizId.value = taskBizId
  statusLabel.value = musicTaskStatusText(MUSIC_TASK_STATUS.GENERATING)
  stopPoll()
  pollTimer.value = setInterval(() => void pollOnce(taskBizId), POLL_MS)
  void pollOnce(taskBizId)
}

const onSubmit = async () => {
  if (!(await validateFormRef(formRef))) return
  if (!validateBusinessRules()) return
  submitting.value = true
  audioUrl.value = null
  errorDetail.value = ''
  stopPoll()

  /** 后端会按文档推断 Suno JSON；此处显式传 customMode，避免只靠 style/title 猜测 */
  const params: Record<string, unknown> = {
    customMode: form.customMode,
    instrumental: form.customMode && form.instrumental,
  }
  if (form.customMode) {
    if (form.style.trim()) params.style = form.style.trim()
    if (form.title.trim()) params.title = form.title.trim()
  }

  try {
    const res = unwrapResult(
      await submitMusicGenerate({
        modelCode: form.modelCode.trim(),
        prompt: form.prompt.trim(),
        params,
      }),
    )
    ElMessage.success(`任务已提交，本次消耗 ${res.costPoints} 积分`)
    startPoll(res.businessTaskId)
  } catch (e) {
    submitting.value = false
    showSubmitError(e, '提交失败')
  }
}

const hint =
  import.meta.env.DEV ?
    [
      '后端默认 melodify.music-generation.provider=auto：已配置 Suno api-key（如 application-local.yml）则试听为远端真曲。',
      '若仍听到 SoundHelix 占位音频，多半是未载入密钥或未重启后端。',
    ].join('')
  : ''
</script>

<template>
  <div class="page-stack">
    <section class="page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Create</p>
        <h1 class="page-title page-title--lg">创作音乐</h1>
        <p class="page-desc page-desc--wide">
          从一句灵感开始，选择模型和模式后提交生成；完成后可在这里试听，也会进入作品库。
        </p>
      </div>
      <RouterLink class="primary-pill-link" to="/works">查看作品库</RouterLink>
    </section>

    <div class="generate-grid">
      <section class="composer-card melodify-glass-card">
        <el-form
          ref="formRef"
          class="generate-form"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent
        >
          <div class="form-row">
            <el-form-item label="生成模型" prop="modelCode">
              <el-select v-model="form.modelCode" placeholder="请选择">
                <el-option
                  v-for="item in modelOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="model-option">
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.desc }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="创作模式">
              <el-segmented
                v-model="form.customMode"
                :options="[
                  { label: '简单模式', value: false },
                  { label: '自定义', value: true },
                ]"
              />
            </el-form-item>
          </div>

          <el-form-item label="提示词 / 歌词" prop="prompt">
            <el-input
              v-model="form.prompt"
              type="textarea"
              :rows="8"
              resize="none"
              :placeholder="
                form.customMode ?
                  form.instrumental ?
                    '纯器乐可不填，也可以描述氛围、乐器与情绪'
                  : '将作为精确歌词写入 Suno（自定义模式）'
                : '简单模式：一段话描述你想要的音乐即可（≤500 字）'
              "
              :maxlength="promptMaxLen"
              show-word-limit
            />
          </el-form-item>

          <div class="prompt-chips">
            <button
              v-for="text in promptExamples"
              :key="text"
              type="button"
              class="prompt-chip"
              @click="applyExample(text)"
            >
              {{ text }}
            </button>
          </div>

          <div v-if="form.customMode" class="custom-box">
            <div class="form-row">
              <el-form-item label="风格">
                <el-input v-model="form.style" placeholder="如：爵士、民谣、电子" maxlength="1000" />
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="form.title" placeholder="成品标题" maxlength="100" />
              </el-form-item>
            </div>
            <el-checkbox v-model="form.instrumental">纯器乐（无人声）</el-checkbox>
          </div>

          <div class="submit-row">
            <el-button type="primary" size="large" :loading="submitting" @click="onSubmit">
              生成音乐
            </el-button>
            <span class="cost-note">本次预计消耗 10 积分</span>
          </div>
        </el-form>
      </section>

      <aside class="side-panel">
        <section class="status-card melodify-glass-card">
          <div class="status-head">
            <span class="status-disc" :class="`status-disc--${statusType}`" />
            <div>
              <h2>任务状态</h2>
              <p>{{ statusLabel || '等待提交新的音乐任务' }}</p>
            </div>
          </div>
          <div v-if="busyTaskBizId" class="task-id">
            <span>任务号</span>
            <code>{{ busyTaskBizId }}</code>
          </div>
          <p v-if="errorDetail" class="status-error">{{ errorDetail }}</p>
          <div v-if="audioUrl" class="audio-card">
            <p>生成完成，可以试听</p>
            <audio controls class="preview-audio" :src="audioUrl" preload="none" />
          </div>
        </section>

        <section class="tips-card melodify-glass-card">
          <h2>创作建议</h2>
          <ol>
            <li>先写情绪、风格和场景，再补充乐器与人声。</li>
            <li>简单模式适合灵感草稿，自定义模式适合明确歌词。</li>
            <li>如果失败，系统会自动退回本次生成积分。</li>
          </ol>
        </section>

        <p v-if="hint" class="page-hint">{{ hint }}</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.generate-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 22rem;
  gap: 1.1rem;
  align-items: start;
}

.composer-card,
.status-card,
.tips-card {
  padding: 1.35rem;
}

.generate-form {
  width: 100%;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.model-option {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.model-option strong {
  font-weight: 800;
}

.model-option span {
  color: var(--el-text-color-secondary);
  font-size: 0.82rem;
}

.prompt-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin: -0.3rem 0 1.2rem;
}

.prompt-chip {
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 999px;
  background: #f8fafc;
  color: var(--el-color-primary);
  padding: 0.45rem 0.75rem;
  cursor: pointer;
  font-size: 0.85rem;
  font-family: inherit;
}

.custom-box {
  padding: 1rem;
  border-radius: 1rem;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.submit-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.9rem;
  margin-top: 1.25rem;
}

.cost-note {
  color: var(--melodify-muted);
  font-size: 0.9rem;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.status-head {
  display: flex;
  gap: 0.8rem;
  align-items: flex-start;
}

.status-head h2,
.tips-card h2 {
  margin: 0;
  color: var(--melodify-strong);
  font-size: 1.05rem;
  font-weight: 900;
}

.status-head p {
  margin-top: 0.25rem;
  color: var(--melodify-muted);
}

.status-disc {
  width: 0.85rem;
  height: 0.85rem;
  flex: none;
  margin-top: 0.35rem;
  border-radius: 999px;
  background: var(--el-color-info);
  box-shadow: 0 0 0 0.35rem rgba(148, 163, 184, 0.12);
}

.status-disc--success {
  background: var(--el-color-success);
}

.status-disc--warning {
  background: var(--el-color-warning);
}

.status-disc--danger {
  background: var(--el-color-danger);
}

.task-id {
  margin-top: 1rem;
  padding: 0.85rem;
  border-radius: 0.9rem;
  background: #f8fafc;
}

.task-id span {
  display: block;
  color: var(--melodify-muted);
  font-size: 0.78rem;
}

.task-id code {
  display: block;
  margin-top: 0.35rem;
  word-break: break-all;
  color: var(--melodify-strong);
}

.status-error {
  margin: 1rem 0 0;
  color: var(--el-color-danger);
}

.audio-card {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.audio-card p {
  color: var(--melodify-strong);
  font-weight: 800;
  margin-bottom: 0.6rem;
}

.preview-audio {
  width: 100%;
}

.tips-card ol {
  margin: 0.8rem 0 0;
  padding-left: 1.2rem;
  color: var(--melodify-muted);
}

.tips-card li + li {
  margin-top: 0.55rem;
}

.page-hint {
  margin: 0;
  padding: 0.9rem 1rem;
  font-size: 0.86rem;
  border-radius: var(--melodify-radius-md);
  color: var(--melodify-muted);
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

@media (max-width: 980px) {
  .generate-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
