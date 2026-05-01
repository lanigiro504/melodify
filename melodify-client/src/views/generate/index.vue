<script setup lang="ts">
/**
 * AI 创作：提交生成请求 → 轮询任务 → 成功后拉取成品 URL 并用 audio 试听。
 */
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onUnmounted, reactive, ref } from 'vue'
import { getMusicAssetByBusinessTask } from '@/api/musicAssets'
import { getMusicTaskByBusinessId, submitMusicGenerate } from '@/api/musicTasks'
import { MUSIC_TASK_STATUS, musicTaskStatusText } from '@/types/musicTask'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'

defineOptions({ name: 'GenerateMusicPage' })

const POLL_MS = 4000

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
  instrumental: false,
})

const rules: FormRules = {
  prompt: [{ required: true, message: '请输入提示词或歌词', trigger: 'blur' }],
}

const stopPoll = () => {
  if (pollTimer.value != null) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

onUnmounted(() => stopPoll())

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
  } catch (e) {
    statusLabel.value = '轮询暂时失败，将自动重试…'
    console.error(e)
    return false
  }
}

const startPoll = (taskBizId: string) => {
  busyTaskBizId.value = taskBizId
  statusLabel.value = musicTaskStatusText(MUSIC_TASK_STATUS.GENERATING)
  stopPoll()
  void pollOnce(taskBizId)
  pollTimer.value = setInterval(() => void pollOnce(taskBizId), POLL_MS)
}

const onSubmit = async () => {
  if (!(await validateFormRef(formRef))) return
  submitting.value = true
  audioUrl.value = null
  errorDetail.value = ''
  stopPoll()

  const params: Record<string, unknown> = {
    customMode: true,
    instrumental: form.instrumental,
  }
  if (form.style.trim()) params.style = form.style.trim()
  if (form.title.trim()) params.title = form.title.trim()

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
    `开发环境请将后端 provider 设为 suno 且配置 SUNO_API_KEY（见 application.yml）；未配置时使用本地占位音频。`
  : ''
</script>

<template>
  <div class="generate-page">
    <h1 class="page-title">创作音乐</h1>
    <p class="page-desc">填写提示词与风格，系统将异步生成曲目；完成后可在此试听。</p>
    <p v-if="hint" class="page-hint">{{ hint }}</p>

    <el-form
      ref="formRef"
      class="generate-form"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent
    >
      <el-form-item label="模型" prop="modelCode">
        <el-select v-model="form.modelCode" placeholder="请选择" style="width: 100%">
          <el-option label="V4_5（推荐）" value="V4_5" />
          <el-option label="V4" value="V4" />
        </el-select>
      </el-form-item>
      <el-form-item label="提示词 / 歌词" prop="prompt">
        <el-input
          v-model="form.prompt"
          type="textarea"
          :rows="5"
          placeholder="描述你想要的音乐氛围、主题或直接把歌词粘贴在此"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="风格（可选）">
        <el-input v-model="form.style" placeholder="如：民谣流行、电子舞曲" maxlength="128" />
      </el-form-item>
      <el-form-item label="标题（可选）">
        <el-input v-model="form.title" placeholder="成品标题，展示用" maxlength="120" />
      </el-form-item>
      <el-form-item label="">
        <el-checkbox v-model="form.instrumental">纯器乐（无人声）</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSubmit">生成</el-button>
      </el-form-item>
    </el-form>

    <section v-if="busyTaskBizId || audioUrl || errorDetail" class="generate-status">
      <h2 class="subsection-title">任务状态</h2>
      <p v-if="busyTaskBizId" class="status-line"><strong>任务号：</strong>{{ busyTaskBizId }}</p>
      <p class="status-line"><strong>状态：</strong>{{ statusLabel || '—' }}</p>
      <p v-if="errorDetail" class="status-error">{{ errorDetail }}</p>
      <audio v-if="audioUrl" controls class="preview-audio" :src="audioUrl" preload="none" />
    </section>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
}

.page-desc {
  margin: 0 0 0.75rem;
  color: var(--color-text);
  opacity: 0.9;
}

.page-hint {
  margin: 0 0 1.25rem;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  border-radius: var(--melodify-radius-md, 8px);
  background: var(--color-background-soft, #f6f8fa);
  border: 1px solid var(--color-border);
}

.generate-form {
  max-width: 640px;
}

.generate-status {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
}

.subsection-title {
  font-size: 1.125rem;
  margin: 0 0 0.75rem;
}

.status-line {
  margin: 0.35rem 0;
  word-break: break-all;
}

.status-error {
  color: var(--el-color-danger);
  margin: 0.5rem 0 1rem;
}

.preview-audio {
  width: 100%;
  max-width: 480px;
  margin-top: 0.75rem;
}
</style>
