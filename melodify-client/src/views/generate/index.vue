<script setup lang="ts">
/**
 * AI 创作：提交生成请求 → 轮询任务 → 成功后拉取成品并由底部全局播放器试听。
 */
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getMusicAssetByBusinessTask } from '@/api/musicAssets'
import { getMusicTaskByBusinessId, submitMusicGenerate } from '@/api/musicTasks'
import { usePlayerStore, type PlayerTrack } from '@/stores/player'
import { MUSIC_TASK_STATUS, musicTaskStatusText } from '@/types/musicTask'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { extractTrackLyrics } from '@/utils/trackLyrics'
import { validateFormRef } from '@/utils/validateFormRef'
import {
  appendPromptFragment,
  FULL_PROMPT_PRESETS,
  LYRIC_SKELETONS,
  PROMPT_COMPOSITION_RULES,
  PROMPT_DIMENSIONS,
  STYLE_QUICK_PRESETS,
  TITLE_IDEA_PRESETS,
} from '@/constants/promptPresets'

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

const formRef = ref<FormInstance>()
/** 灵感库：自定义模式下默认收起，缩短页面 */
const inspireLibraryNames = ref<string[]>(['library'])

watch(
  () => form.customMode,
  (custom) => {
    inspireLibraryNames.value = custom ? [] : ['library']
  },
  { immediate: true },
)

/** 灵感库：分页签（要点 / 整段 / 标签），避免单页折叠后过长滚动 */
const inspirePresetTab = ref<'rules' | 'full' | 'tags'>('full')
const submitting = ref(false)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const busyTaskBizId = ref<string | null>(null)
const statusLabel = ref('')
const completedPlayback = ref<PlayerTrack | null>(null)
const errorDetail = ref('')

const form = reactive({
  modelCode: 'V4_5',
  /** 简单模式：整段创作描述。自定义人声：可选的补充描述（不写进 Suno prompt）。 */
  prompt: '',
  /** 仅自定义 + 人声：写入 params.lyrics，并由后端作为 Suno 的 prompt 提交。 */
  lyrics: '',
  style: '',
  title: '',
  /** 对齐 Suno 文档：自定义模式需提供 style/title；关闭时仅 prompt（建议 ≤500 字） */
  customMode: false,
  instrumental: false,
})

/** 文档：非自定义 prompt ≤500；自定义下创作描述取宽松上限；歌词单独计量 */
const promptMaxLen = computed(() => (form.customMode ? 3000 : 500))
const lyricsMaxLen = 5000

const statusType = computed(() => {
  if (errorDetail.value) return 'danger'
  if (completedPlayback.value) return 'success'
  if (busyTaskBizId.value) return 'warning'
  return 'info'
})

function replayCompletedPlayback() {
  const t = completedPlayback.value
  if (t) player.playTrack({ ...t })
}

const applyFullPresetText = (text: string) => {
  form.prompt = text
}

const appendDimensionTag = (tag: string) => {
  form.prompt = appendPromptFragment(form.prompt, tag)
}

const applyStyleQuick = (text: string) => {
  form.style = text
}

const applyTitleIdea = (text: string) => {
  form.title = text
}

const applyLyricSkeleton = (text: string) => {
  form.lyrics = text
}

const rules = computed<FormRules>(() => ({
  prompt: [
    {
      validator: (_r, val, cb) => {
        if (form.customMode) {
          cb()
          return
        }
        if (!String(val ?? '').trim()) {
          cb(new Error('请输入创作描述'))
          return
        }
        cb()
      },
      trigger: 'blur',
    },
  ],
  lyrics: [
    {
      validator: (_r, val, cb) => {
        if (!form.customMode || form.instrumental) {
          cb()
          return
        }
        if (!String(val ?? '').trim()) {
          cb(new Error('请填写精确歌词'))
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
  if (!form.instrumental && !form.lyrics.trim()) {
    ElMessage.warning('带人声时请填写「精确歌词」')
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
  if (typeof q.lyrics === 'string') form.lyrics = q.lyrics
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
        const track: PlayerTrack = {
          title:
            form.title.trim() ||
            (form.customMode && !form.instrumental ?
              form.lyrics.trim().slice(0, 48)
            : form.prompt.trim().slice(0, 48)) ||
            '新作品',
          fileUrl: asset.fileUrl,
          subtitle: form.modelCode,
          lyrics: extractTrackLyrics(form.prompt, {
            customMode: form.customMode,
            instrumental: form.instrumental,
            lyrics: form.lyrics,
          }),
          durationSec: asset.durationSec ?? undefined,
        }
        completedPlayback.value = track
        player.playTrack(track)
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
  completedPlayback.value = null
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

  const trimmedLyrics = form.customMode && !form.instrumental ? form.lyrics.trim() : ''
  try {
    const res = unwrapResult(
      await submitMusicGenerate({
        modelCode: form.modelCode.trim(),
        prompt: form.prompt.trim(),
        ...(trimmedLyrics ? { lyrics: trimmedLyrics } : {}),
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

</script>

<template>
  <div class="page-stack">
    <section class="page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Create</p>
        <h1 class="page-title page-title--lg">创作音乐</h1>
        <p class="page-desc page-desc--wide">
          从一句灵感开始，选择模型和模式后提交生成；完成后将由底部播放条试听，成品也会进入作品库。
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

          <el-form-item v-if="!form.customMode" label="创作描述" prop="prompt">
            <el-input
              v-model="form.prompt"
              type="textarea"
              :rows="8"
              resize="none"
              placeholder="用一段话描述情绪、风格、场景或乐器，无需写逐行歌词。"
              :maxlength="promptMaxLen"
              show-word-limit
            />
          </el-form-item>

          <el-form-item v-else-if="form.instrumental" label="器乐 / 氛围描述" prop="prompt">
            <el-input
              v-model="form.prompt"
              type="textarea"
              :rows="5"
              resize="none"
              placeholder="描述氛围、乐器与情绪（可不填）；若留空则主要依赖标题与风格。"
              :maxlength="promptMaxLen"
              show-word-limit
            />
          </el-form-item>

          <template v-else>
            <el-form-item label="创作描述（可选）" prop="prompt">
              <el-input
                v-model="form.prompt"
                type="textarea"
                :rows="3"
                resize="none"
                placeholder="补充场景、情绪或演唱提示（仅保存在任务中，不直接作为 Suno 歌词提交）。"
                :maxlength="promptMaxLen"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="精确歌词" prop="lyrics">
              <div class="lyric-toolbar">
                <p class="lyric-toolbar__hint">可选用骨架起步，再改成你的词</p>
                <div class="lyric-toolbar__chips">
                  <button
                    v-for="sk in LYRIC_SKELETONS"
                    :key="sk.id"
                    type="button"
                    class="lyric-skel-btn"
                    @click="applyLyricSkeleton(sk.text)"
                  >
                    {{ sk.label }}
                  </button>
                </div>
              </div>
              <el-input
                v-model="form.lyrics"
                type="textarea"
                :rows="7"
                resize="none"
                placeholder="写入可演唱的歌词（多行、结构清晰）；此栏内容将作为 Suno 人声生成的歌词。"
                :maxlength="lyricsMaxLen"
                show-word-limit
              />
            </el-form-item>
          </template>

          <div v-if="form.customMode" class="custom-panel">
            <div class="custom-panel__head">
              <span class="custom-panel__head-title">自定义参数</span>
              <span class="custom-panel__head-hint">标题与风格为必填；下方快选可一键填入</span>
            </div>
            <div class="form-row custom-panel__fields">
              <el-form-item label="风格">
                <el-input v-model="form.style" placeholder="如：爵士、民谣、电子" maxlength="1000" />
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="form.title" placeholder="成品标题" maxlength="100" />
              </el-form-item>
            </div>
            <el-checkbox v-model="form.instrumental" class="custom-panel__instrumental"
              >纯器乐（无人声）</el-checkbox
            >

            <div class="custom-presets-block">
              <div class="preset-subsection">
                <span class="preset-subsection__label">风格快选</span>
                <div class="style-preset-grid">
                  <button
                    v-for="(item, i) in STYLE_QUICK_PRESETS"
                    :key="`st-${i}`"
                    type="button"
                    class="style-preset-tile"
                    :title="item.value"
                    @click="applyStyleQuick(item.value)"
                  >
                    <span class="style-preset-tile__text">{{ item.label }}</span>
                  </button>
                </div>
              </div>
              <div class="preset-subsection">
                <span class="preset-subsection__label">标题灵感</span>
                <div class="title-idea-row">
                  <button
                    v-for="t in TITLE_IDEA_PRESETS"
                    :key="t"
                    type="button"
                    class="title-idea-chip"
                    @click="applyTitleIdea(t)"
                  >
                    {{ t }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="inspire-panel">
            <el-collapse v-model="inspireLibraryNames" class="inspire-library-collapse">
              <el-collapse-item name="library">
                <template #title>
                  <div class="inspire-library-title-wrap">
                    <div class="inspire-library-title">
                      <span class="inspire-panel__title">灵感预设库</span>
                      <span class="inspire-panel__badge">本地</span>
                    </div>
                    <span
                      v-if="form.customMode && !inspireLibraryNames.includes('library')"
                      class="inspire-library-title__sub"
                    >
                      已收起以节省版面，点此展开
                    </span>
                  </div>
                </template>
                <el-tabs v-model="inspirePresetTab" type="card" class="inspire-tabs">
              <el-tab-pane label="撰写要点" name="rules" lazy>
                <div class="preset-tab-inner">
                  <ul class="preset-rule-cards">
                    <li v-for="(line, idx) in PROMPT_COMPOSITION_RULES" :key="idx" class="preset-rule-card">
                      <span class="preset-rule-card__idx">{{ idx + 1 }}</span>
                      <span class="preset-rule-card__text">{{ line }}</span>
                    </li>
                  </ul>
                </div>
              </el-tab-pane>

              <el-tab-pane label="整段示例" name="full" lazy>
                <div class="preset-tab-inner">
                  <p class="preset-section__hint preset-section__hint--tab">点击卡片即用完整描述替换当前「创作描述 / 器乐氛围」</p>
                  <div class="full-preset-grid">
                    <button
                      v-for="item in FULL_PROMPT_PRESETS"
                      :key="item.id"
                      type="button"
                      class="full-preset-card"
                      :title="item.text"
                      @click="applyFullPresetText(item.text)"
                    >
                      <span class="full-preset-card__name">{{ item.label }}</span>
                      <span class="full-preset-card__peek">{{ item.text }}</span>
                    </button>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="按维度追加" name="tags" lazy>
                <div class="preset-tab-inner preset-tab-inner--tags">
                  <p class="preset-section__hint preset-section__hint--tab">
                    依次点击标签，片段会追加到上方描述（自动加逗号）
                  </p>
                  <div class="dim-tags-scroll">
                    <div v-for="dim in PROMPT_DIMENSIONS" :key="dim.id" class="dim-row">
                      <span class="dim-row__label">{{ dim.title }}</span>
                      <div class="dim-row__tags">
                        <button
                          v-for="tag in dim.tags"
                          :key="`${dim.id}-${tag}`"
                          type="button"
                          class="tag-pill"
                          @click="appendDimensionTag(tag)"
                        >
                          {{ tag }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
                </el-tabs>
              </el-collapse-item>
            </el-collapse>
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
          <div v-if="completedPlayback" class="play-complete-row">
            <p>已在底部播放器开始试听。</p>
            <el-button type="primary" link @click="replayCompletedPlayback">再播一次</el-button>
          </div>
        </section>

        <section class="tips-card melodify-glass-card">
          <h2>创作建议</h2>
          <ol>
            <li>「灵感预设库」可<strong>折叠</strong>；自定义模式下默认收起以缩短页面，需要时再展开。</li>
            <li>展开后用分栏切换要点、整段与标签。</li>
            <li>先写情绪、风格和场景，再补充乐器与人声。</li>
            <li><strong>简单模式</strong>只有「创作描述」；<strong>自定义 + 人声</strong>请把逐行歌词放在「精确歌词」，与上面的补充描述区分开。</li>
            <li><strong>自定义 + 纯器乐</strong>只需标题与风格，氛围描述选填。</li>
            <li>如果失败，系统会自动退回本次生成积分。</li>
          </ol>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.generate-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 22rem;
  gap: 1.1rem;
  align-items: stretch;
}

.composer-card,
.status-card,
.tips-card {
  padding: 1.35rem;
}

.composer-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
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
  font-weight: 700;
}

.model-option span {
  color: var(--el-text-color-secondary);
  font-size: 0.82rem;
}

/* —— 自定义参数块 —— */
.custom-panel {
  margin-bottom: 0.85rem;
  padding: 0.85rem 1rem 0.95rem;
  border-radius: var(--melodify-radius-lg);
  border: 1px solid var(--melodify-divider-strong);
  background: linear-gradient(165deg, var(--el-color-primary-light-9) 0%, var(--melodify-card-solid) 42%);
  box-shadow: var(--melodify-shadow-soft);
}

.custom-panel__head {
  margin-bottom: 0.75rem;
  padding-bottom: 0.65rem;
  border-bottom: 1px solid var(--melodify-divider);
}

.custom-panel__head-title {
  display: block;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--melodify-strong);
  letter-spacing: 0.02em;
}

.custom-panel__head-hint {
  display: block;
  margin-top: 0.3rem;
  font-size: 0.78rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}

.custom-panel__fields {
  margin-bottom: 0.25rem;
}

.custom-panel__instrumental {
  margin: 0.35rem 0 0;
  font-weight: 600;
}

.custom-presets-block {
  margin-top: 0.75rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--melodify-divider);
}

.preset-subsection + .preset-subsection {
  margin-top: 0.75rem;
}

.preset-subsection__label {
  display: block;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--melodify-muted);
  margin-bottom: 0.55rem;
}

.style-preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 148px), 1fr));
  gap: 0.45rem;
}

.style-preset-tile {
  display: flex;
  align-items: flex-start;
  text-align: left;
  min-height: 2.5rem;
  padding: 0.45rem 0.55rem;
  border-radius: var(--melodify-radius-sm);
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-card-solid);
  cursor: pointer;
  font-family: inherit;
  font-size: 0.78rem;
  font-weight: 600;
  line-height: 1.3;
  color: var(--melodify-strong);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.style-preset-tile:hover {
  border-color: rgba(var(--melodify-primary-rgb), 0.28);
  box-shadow: var(--melodify-shadow-card);
  background: var(--melodify-surface-sunken);
}

.style-preset-tile:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 1px;
}

.style-preset-tile__text {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.title-idea-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.title-idea-chip {
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  border: 1px solid rgba(var(--melodify-primary-rgb), 0.2);
  background: var(--melodify-card-solid);
  color: var(--el-color-primary);
  font-size: 0.82rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease;
}

.title-idea-chip:hover {
  background: var(--el-color-primary-light-9);
  border-color: rgba(var(--melodify-primary-rgb), 0.35);
}

/* —— 歌词骨架工具条 —— */
.lyric-toolbar {
  margin-bottom: 0.65rem;
}

.lyric-toolbar__hint {
  margin: 0 0 0.45rem;
  font-size: 0.8rem;
  color: var(--melodify-muted);
}

.lyric-toolbar__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.lyric-skel-btn {
  padding: 0.38rem 0.7rem;
  border-radius: var(--melodify-radius-sm);
  border: 1px dashed rgba(var(--melodify-primary-rgb), 0.35);
  background: rgba(var(--melodify-primary-rgb), 0.05);
  color: var(--el-color-primary);
  font-size: 0.78rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s ease;
}

.lyric-skel-btn:hover {
  background: var(--el-color-primary-light-9);
  border-style: solid;
}

/* —— 灵感预设：可折叠 + 分栏 —— */
.inspire-panel {
  margin-bottom: 0.85rem;
  border: 1px solid var(--melodify-divider-strong);
  border-radius: var(--melodify-radius-lg);
  overflow: hidden;
  background: var(--melodify-card-solid);
  box-shadow: var(--melodify-shadow-card);
}

.inspire-library-collapse {
  border: none;
}

.inspire-library-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 2.75rem;
  padding: 0.65rem 0.85rem;
  line-height: 1.35;
  font-weight: 600;
  background: linear-gradient(180deg, #fafafa 0%, var(--melodify-card-solid) 100%);
  border-bottom: 1px solid var(--melodify-divider);
}

.inspire-library-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.inspire-library-collapse :deep(.el-collapse-item__content) {
  padding: 0;
}

.inspire-library-collapse :deep(.el-collapse-item__arrow) {
  margin-left: 0.5rem;
  color: var(--melodify-muted);
}

.inspire-library-title-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
  text-align: left;
  width: 100%;
  padding-right: 0.25rem;
}

.inspire-library-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.inspire-library-title__sub {
  font-size: 0.72rem;
  color: var(--melodify-muted);
  font-weight: 500;
  line-height: 1.35;
}

.inspire-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--melodify-strong);
}

.inspire-panel__badge {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0.12rem 0.45rem;
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border: 1px solid rgba(var(--melodify-primary-rgb), 0.15);
}

.inspire-tabs {
  --inspire-tabs-pad: 0.65rem 0.8rem 0.85rem;
}

.inspire-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0.5rem 0.65rem 0;
  background: var(--melodify-card-solid);
  border-bottom: 1px solid var(--melodify-divider);
}

.inspire-tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}

.inspire-tabs :deep(.el-tabs__item) {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0 1rem;
  height: 2.25rem;
}

.inspire-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}

.inspire-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.preset-tab-inner {
  padding: var(--inspire-tabs-pad);
}

.preset-tab-inner--tags {
  padding-bottom: 0.65rem;
}

.preset-section__hint--tab {
  margin-top: 0;
  margin-bottom: 0.65rem;
}

.dim-tags-scroll {
  max-height: min(280px, 44vh);
  overflow-y: auto;
  padding-right: 0.25rem;
  margin-right: -0.15rem;
  scroll-behavior: smooth;
  scrollbar-gutter: stable;
}

.dim-tags-scroll::-webkit-scrollbar {
  width: 6px;
}

.dim-tags-scroll::-webkit-scrollbar-thumb {
  background: rgba(var(--melodify-primary-rgb), 0.2);
  border-radius: 999px;
}

.dim-tags-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--melodify-primary-rgb), 0.35);
}

.preset-section__hint {
  margin: 0 0 0.75rem;
  font-size: 0.76rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}

.preset-rule-cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.preset-rule-card {
  display: flex;
  gap: 0.65rem;
  align-items: flex-start;
  padding: 0.6rem 0.7rem;
  border-radius: var(--melodify-radius-sm);
  border: 1px solid var(--melodify-divider);
  background: var(--melodify-surface-muted);
}

.preset-rule-card__idx {
  flex: none;
  width: 1.35rem;
  height: 1.35rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 0.72rem;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.preset-rule-card__text {
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--melodify-strong);
}

.full-preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 160px), 1fr));
  gap: 0.55rem;
}

.full-preset-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.35rem;
  padding: 0.65rem 0.75rem;
  min-height: 5rem;
  text-align: left;
  border-radius: var(--melodify-radius-sm);
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-card-solid);
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.12s ease;
}

.full-preset-card:hover {
  border-color: rgba(var(--melodify-primary-rgb), 0.3);
  box-shadow: var(--melodify-shadow-hover);
  transform: translateY(-1px);
}

.full-preset-card:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 2px;
}

.full-preset-card__name {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--melodify-strong);
  line-height: 1.3;
}

.full-preset-card__peek {
  font-size: 0.68rem;
  line-height: 1.4;
  color: var(--melodify-muted);
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dim-row {
  padding: 0.65rem 0 0.75rem;
  border-bottom: 1px solid var(--melodify-divider);
}

.dim-row:first-child {
  padding-top: 0;
}

.dim-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.dim-row__label {
  display: block;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--melodify-muted);
  margin-bottom: 0.45rem;
}

.dim-row__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.38rem;
}

.tag-pill {
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  border: 1px solid var(--melodify-divider-strong);
  background: var(--melodify-surface-sunken);
  color: var(--melodify-strong);
  font-size: 0.76rem;
  font-family: inherit;
  cursor: pointer;
  transition:
    background 0.12s ease,
    border-color 0.12s ease;
}

.tag-pill:hover {
  border-color: rgba(var(--melodify-primary-rgb), 0.28);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.tag-pill:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 1px;
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
  height: 100%;
  min-height: 0;
}

.status-head {
  display: flex;
  gap: 0.8rem;
  align-items: flex-start;
}

.status-head h2,
.tips-card h2 {
  margin: 0;
  color: var(--melodify-classical-ink, var(--melodify-strong));
  font-size: 1.05rem;
  font-weight: 700;
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
  box-shadow: 0 0 0 0.35rem rgba(122, 95, 71, 0.1);
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
  background: var(--melodify-surface-sunken, #f3efe6);
  border: 1px solid var(--melodify-divider, rgba(58, 48, 40, 0.1));
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

.play-complete-row {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--melodify-divider-strong, rgba(58, 48, 40, 0.12));
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.35rem 1rem;
}

.play-complete-row p {
  margin: 0;
  color: var(--melodify-strong);
  font-weight: 600;
  font-size: 0.875rem;
}

.tips-card ol {
  margin: 0.8rem 0 0;
  padding-left: 1.2rem;
  color: var(--melodify-muted);
}

.tips-card li + li {
  margin-top: 0.55rem;
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

  .inspire-tabs :deep(.el-tabs__item) {
    padding: 0 0.55rem;
    font-size: 0.76rem;
  }

  .inspire-tabs :deep(.el-tabs__nav) {
    width: 100%;
  }

  .inspire-tabs :deep(.el-tabs__nav-scroll) {
    overflow-x: auto;
  }
}
</style>
