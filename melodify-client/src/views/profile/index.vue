<script setup lang="ts">
/**
 * 个人中心：侧栏身份与快捷入口 + 资料 / 积分分区。
 */
import type { FormInstance, FormRules, UploadRawFile, UploadRequestOptions } from 'element-plus'
import type { PointLog } from '@/types/api'
import { Camera, Cpu, Headset, Trophy, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { pageMyPointLogs } from '@/api/pointLogs'
import { useAuthStore } from '@/stores/auth'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'
import { avatarDisplayUrl } from '@/utils/avatarDisplayUrl'

defineOptions({ name: 'ProfilePage' })

const auth = useAuthStore()
const profileRef = ref<FormInstance>()
const savingProfile = ref(false)
const logsLoading = ref(false)
const logs = ref<PointLog[]>([])
const logTotal = ref(0)
const logPager = reactive({ current: 1, size: 8 })

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  qq: '',
})

const profileRules: FormRules = {
  nickname: [{ max: 50, message: '昵称最多 50 个字符', trigger: 'blur' }],
  email: [{ max: 100, message: '邮箱最多 100 个字符', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号最多 20 个字符', trigger: 'blur' }],
  qq: [{ max: 20, message: 'QQ 号最多 20 个字符', trigger: 'blur' }],
}

watch(
  () => auth.currentUser,
  (user) => {
    profileForm.nickname = user?.nickname ?? ''
    profileForm.email = user?.email ?? ''
    profileForm.phone = user?.phone ?? ''
    profileForm.qq = user?.qq ?? ''
  },
  { immediate: true },
)

const changeTypeText = (t: number | null | undefined) => {
  switch (t) {
    case 1:
      return '充值'
    case 2:
      return '消费'
    case 3:
      return '退款'
    case 4:
      return '活动'
    case 5:
      return '管理员调整'
    default:
      return '变动'
  }
}

const saveProfile = async () => {
  if (!(await validateFormRef(profileRef))) return
  savingProfile.value = true
  try {
    await auth.updateProfile({
      nickname: profileForm.nickname.trim(),
      email: profileForm.email.trim(),
      phone: profileForm.phone.trim(),
      qq: profileForm.qq.trim(),
    })
  } catch (e) {
    showSubmitError(e, '保存资料失败')
  } finally {
    savingProfile.value = false
  }
}

const fetchLogs = async () => {
  logsLoading.value = true
  try {
    const page = unwrapResult(await pageMyPointLogs(logPager.current, logPager.size))
    logs.value = page.records
    logTotal.value = page.total
  } catch (e) {
    showSubmitError(e, '加载积分流水失败')
  } finally {
    logsLoading.value = false
  }
}

const activeTab = ref<'profile' | 'points'>('profile')

watch(
  () => activeTab.value,
  (t) => {
    if (t === 'points') void fetchLogs()
  },
)

const avatarUploading = ref(false)

const asideAvatarSrc = computed(() => avatarDisplayUrl(auth.currentUser?.avatar))

const beforeAvatarUpload = (file: UploadRawFile) => {
  const okType =
    file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  if (!okType) {
    ElMessage.warning('仅支持 JPG、PNG、WebP')
    return false
  }
  const max = 2 * 1024 * 1024
  if (file.size > max) {
    ElMessage.warning('图片请小于 2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: UploadRequestOptions) => {
  avatarUploading.value = true
  try {
    await auth.uploadAvatar(options.file as File)
    ElMessage.success('头像已更新')
    options.onSuccess?.({} as never)
  } catch (e) {
    showSubmitError(e, '上传头像失败')
    options.onError?.(e as never)
  } finally {
    avatarUploading.value = false
  }
}

const onAvatarTriggerKeydown = (ev: KeyboardEvent) => {
  if (ev.key !== 'Enter' && ev.key !== ' ') return
  ev.preventDefault()
  ;(ev.currentTarget as HTMLElement | null)?.click()
}

void auth.refreshMe().catch(() => {})
</script>

<template>
  <div class="page-stack profile-page">
    <header class="profile-page-head">
      <p class="page-eyebrow">账户与资料</p>
      <h1 class="profile-page-title">个人中心</h1>
      <p class="profile-page-sub">编辑昵称与联系方式；头像仅支持本地上传，点击侧栏圆形头像即可更换。</p>
    </header>

    <div class="profile-shell">
      <aside class="profile-aside soft-card">
        <div class="aside-inner">
          <el-upload
            class="aside-avatar-upload"
            :show-file-list="false"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            :disabled="avatarUploading"
          >
            <div
              class="aside-avatar"
              tabindex="0"
              role="button"
              aria-label="上传或更换头像"
              v-loading="avatarUploading"
              element-loading-text="上传中…"
              element-loading-background="rgba(255,255,255,0.6)"
              :style="asideAvatarSrc ? { backgroundImage: `url(${asideAvatarSrc})` } : {}"
              @keydown="onAvatarTriggerKeydown"
            >
              <span v-if="!asideAvatarSrc" class="aside-avatar-letter">{{ auth.avatarText }}</span>
              <span v-if="!avatarUploading" class="aside-avatar-overlay" aria-hidden="true">
                <span class="aside-avatar-chip">
                  <el-icon class="aside-avatar-cam"><Camera /></el-icon>
                  <span class="aside-avatar-overlay-text">更换</span>
                </span>
              </span>
            </div>
          </el-upload>
          <h2 class="aside-name">{{ auth.displayName || '创作者' }}</h2>
          <p class="aside-username">@{{ auth.currentUser?.username }}</p>

          <div class="aside-stat">
            <span class="aside-stat-label">可用积分</span>
            <strong class="aside-stat-num">{{ auth.currentUser?.points ?? 0 }}</strong>
          </div>

          <div class="aside-actions">
            <RouterLink class="aside-link" to="/recharge">
              <el-icon><Wallet /></el-icon>
              积分充值
            </RouterLink>
            <RouterLink class="aside-link" to="/generate">
              <el-icon><Cpu /></el-icon>
              去创作
            </RouterLink>
            <RouterLink class="aside-link" to="/works">
              <el-icon><Headset /></el-icon>
              我的作品
            </RouterLink>
            <RouterLink class="aside-link" to="/explore">
              <el-icon><Trophy /></el-icon>
              广场逛逛
            </RouterLink>
          </div>

          <p class="aside-tip">点击头像上传或更换本地图片（JPG / PNG / WebP，≤2MB）。</p>
        </div>
      </aside>

      <main class="profile-main soft-card">
        <el-tabs v-model="activeTab" class="profile-tabs" stretch>
          <el-tab-pane label="资料设置" name="profile">
            <div class="pane-body">
              <p class="pane-lead">
                修改昵称与联系方式。头像请在侧栏点击
                <strong class="pane-lead-strong">圆形预览</strong>
                上传，保存按钮仅作用于本页文字信息。
              </p>
              <el-form
                ref="profileRef"
                :model="profileForm"
                :rules="profileRules"
                label-position="top"
                class="profile-form-grid"
              >
                <el-form-item label="昵称" prop="nickname">
                  <el-input
                    v-model="profileForm.nickname"
                    placeholder="对外展示的创作者昵称"
                    maxlength="50"
                    clearable
                  />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="选填" maxlength="100" clearable />
                </el-form-item>
                <el-form-item label="手机" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" clearable />
                </el-form-item>
                <el-form-item label="QQ" prop="qq">
                  <el-input v-model="profileForm.qq" placeholder="选填" maxlength="20" clearable />
                </el-form-item>
                <div class="full-span form-actions">
                  <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>
          <el-tab-pane label="积分流水" name="points">
            <div v-loading="logsLoading" class="pane-body points-pane">
              <p class="pane-lead">最近积分变动明细；充值与生成扣费均可在此核对。</p>
              <el-table v-if="logs.length" :data="logs" size="small" stripe class="log-table">
                <el-table-column prop="createTime" label="时间" width="172" />
                <el-table-column label="类型" width="104">
                  <template #default="{ row }">
                    {{ changeTypeText(row.changeType) }}
                  </template>
                </el-table-column>
                <el-table-column prop="amount" label="变动" width="88" align="right" />
                <el-table-column prop="balance" label="余额" width="88" align="right" />
                <el-table-column prop="bizType" label="业务" min-width="100" />
                <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
              </el-table>
              <el-empty v-else-if="!logsLoading" description="暂无积分记录" />
              <div v-if="logTotal > logPager.size" class="pager">
                <el-pagination
                  background
                  layout="prev, pager, next, total"
                  :total="logTotal"
                  :page-size="logPager.size"
                  :current-page="logPager.current"
                  @current-change="
                    (c: number) => {
                      logPager.current = c
                      void fetchLogs()
                    }
                  "
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  gap: 1.5rem;
}

.profile-page-head {
  padding: 0.2rem 0 0;
}

.profile-page-title {
  margin: 0 0 0.45rem;
  font-size: clamp(1.55rem, 3.2vw, 2.05rem);
  font-weight: 700;
  letter-spacing: -0.045em;
  color: var(--melodify-strong);
  line-height: 1.18;
}

.profile-page-sub {
  margin: 0;
  max-width: 40rem;
  font-size: 0.9375rem;
  color: var(--melodify-muted);
  line-height: 1.65;
}

.profile-shell {
  display: grid;
  grid-template-columns: minmax(0, 17rem) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.profile-aside.soft-card {
  background: transparent;
  box-shadow: none;
}

.profile-main.soft-card {
  background: transparent;
  box-shadow: none;
}

.profile-aside {
  position: relative;
  border-radius: var(--melodify-radius-lg);
  overflow: hidden;
  transition:
    background-color 0.2s ease,
    box-shadow 0.22s ease,
    transform 0.22s ease;
}

.profile-aside:hover {
  background: var(--melodify-surface-muted, #f5f5f5);
  box-shadow: var(--melodify-shadow-hover);
}

.profile-main {
  border-radius: var(--melodify-radius-lg);
  min-height: 22rem;
  overflow: hidden;
  transition:
    background-color 0.2s ease,
    box-shadow 0.22s ease;
}

.profile-main:hover {
  background: var(--melodify-surface-muted, #f5f5f5);
  box-shadow: var(--melodify-shadow-hover);
}

.profile-aside::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(
    90deg,
    var(--el-color-primary-light-5),
    var(--el-color-primary) 42%,
    var(--el-color-primary-light-7)
  );
  z-index: 1;
}

.aside-inner {
  position: relative;
  z-index: 0;
  padding: 1.65rem 1.35rem 1.4rem;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 1.05rem;
}

.aside-avatar-upload {
  display: flex;
  justify-content: center;
  margin-top: 0.25rem;
}

.aside-avatar-upload :deep(.el-upload) {
  justify-content: center;
  border: none;
  cursor: pointer;
}

.aside-avatar-upload :deep(.el-upload:focus-visible) {
  outline: none;
}

.aside-avatar {
  position: relative;
  margin: 0 auto;
  width: 5.25rem;
  height: 5.25rem;
  border-radius: 50%;
  background: var(--melodify-surface-muted, #f5f5f5);
  color: var(--melodify-muted);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 1.35rem;
  background-size: cover;
  background-position: center;
  outline: 3px solid rgba(255, 253, 251, 0.98);
  box-shadow:
    0 0 0 1px rgba(110, 100, 92, 0.2),
    0 12px 30px rgba(var(--melodify-primary-rgb), 0.12);
  cursor: pointer;
  transition:
    transform 0.22s cubic-bezier(0.34, 1.2, 0.64, 1),
    box-shadow 0.22s ease;
}

.aside-avatar::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.22s ease;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.55);
  pointer-events: none;
}

.aside-avatar:hover,
.aside-avatar:focus-visible {
  transform: scale(1.02);
}

.aside-avatar:hover {
  box-shadow:
    0 0 0 1px rgba(var(--melodify-primary-rgb), 0.28),
    0 0 0 4px rgba(var(--melodify-primary-rgb), 0.07),
    0 16px 38px rgba(var(--melodify-primary-rgb), 0.14);
}

.aside-avatar:hover::after {
  opacity: 1;
}

.aside-avatar:focus-visible::after {
  opacity: 1;
}

.aside-avatar:focus-visible {
  outline: 3px solid rgba(255, 253, 251, 0.98);
  box-shadow:
    0 0 0 2px var(--el-color-primary),
    0 0 0 7px rgba(var(--melodify-primary-rgb), 0.1),
    0 14px 34px rgba(var(--melodify-primary-rgb), 0.12);
}

.aside-avatar-letter {
  position: relative;
  z-index: 0;
}

.aside-avatar-overlay {
  position: absolute;
  right: 0.2rem;
  bottom: 0.2rem;
  left: auto;
  z-index: 2;
  max-width: calc(100% - 0.5rem);
  transform: translateY(4px);
  opacity: 0;
  pointer-events: none;
  transition:
    opacity 0.22s ease,
    transform 0.24s cubic-bezier(0.34, 1.2, 0.64, 1);
}

/* 深色磨砂小条：在浅色占位图与实拍照上对比都更清晰，避免大白块贴底割裂感 */
.aside-avatar-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.22rem;
  padding: 0.3rem 0.5rem 0.3rem 0.42rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.08),
    0 6px 16px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.aside-avatar-cam {
  flex-shrink: 0;
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.96);
}

.aside-avatar:hover .aside-avatar-overlay,
.aside-avatar:focus-visible .aside-avatar-overlay {
  opacity: 1;
  transform: translateY(0);
}

.aside-avatar-overlay-text {
  flex-shrink: 0;
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.98);
  line-height: 1;
  white-space: nowrap;
}

@media (max-width: 380px) {
  .aside-avatar-overlay-text {
    display: none;
  }

  .aside-avatar-chip {
    padding: 0.38rem;
    border-radius: 50%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .aside-avatar,
  .aside-avatar::after,
  .aside-avatar-overlay {
    transition-duration: 0.01ms;
  }

  .aside-avatar:hover,
  .aside-avatar:focus-visible {
    transform: none;
  }

  .aside-avatar-overlay {
    transform: none;
  }
}

.aside-name {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 600;
  text-align: center;
  color: var(--melodify-strong);
  letter-spacing: -0.035em;
  line-height: 1.25;
}

.aside-username {
  margin: -0.35rem 0 0;
  font-size: 0.8125rem;
  text-align: center;
  color: var(--melodify-subtle, #94a3b8);
  font-variant-numeric: tabular-nums;
}

.aside-stat {
  padding: 0.9rem 1rem;
  border-radius: var(--melodify-radius-md);
  background: transparent;
  border: 1px solid var(--melodify-divider-strong);
  transition: background-color 0.18s ease;
}

.aside-stat:hover {
  background: var(--el-color-primary-light-9);
}

.aside-stat-label {
  display: block;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--melodify-muted);
}

.aside-stat-num {
  display: block;
  margin-top: 0.15rem;
  font-size: 1.65rem;
  font-weight: 700;
  color: var(--melodify-strong);
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}

.aside-actions {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.aside-link {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.55rem 0.65rem;
  border-radius: var(--melodify-radius-sm);
  font-weight: 600;
  font-size: 0.84375rem;
  color: var(--melodify-strong);
  text-decoration: none;
  transition:
    background 0.18s cubic-bezier(0.25, 0.8, 0.25, 1),
    color 0.18s ease,
    transform 0.18s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.aside-link :deep(.el-icon) {
  font-size: 1.05rem;
  color: var(--melodify-muted);
  transition: color 0.16s ease;
}

.aside-link:hover {
  background: var(--melodify-surface-muted);
  color: var(--el-color-primary);
  transform: translateX(2px);
}

.aside-link:hover :deep(.el-icon) {
  color: var(--el-color-primary);
}

.aside-tip {
  margin: 0;
  padding: 0.65rem 0.5rem 0;
  border-top: 1px dashed var(--melodify-divider-strong, rgba(58, 48, 40, 0.16));
  font-size: 0.75rem;
  line-height: 1.55;
  color: var(--melodify-muted);
  text-align: center;
}

.profile-tabs {
  --el-tabs-header-height: 48px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 1.5rem;
  background: transparent;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--melodify-border), transparent);
}

.profile-tabs :deep(.el-tabs__item) {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--melodify-muted);
  transition: color 0.15s ease;
}

.profile-tabs :deep(.el-tabs__item:hover) {
  color: var(--melodify-strong);
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: var(--melodify-strong);
  font-weight: 700;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--el-color-primary);
}

.pane-body {
  padding: 1.35rem 1.5rem 1.55rem;
}

.pane-lead {
  margin: 0 0 1.2rem;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: var(--melodify-muted);
  line-height: 1.62;
  background: transparent;
  border-radius: var(--melodify-radius-md);
  border: 1px solid var(--melodify-divider-strong, rgba(58, 48, 40, 0.12));
}

.pane-lead-strong {
  color: var(--melodify-strong);
  font-weight: 700;
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem 1.25rem;
}

.profile-main :deep(.el-form-item__label) {
  margin-bottom: 0.3rem !important;
  padding: 0;
  font-weight: 600;
  font-size: 0.8125rem;
  color: var(--melodify-strong);
  letter-spacing: 0.02em;
}

.full-span {
  grid-column: 1 / -1;
}

.form-actions {
  margin-top: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--melodify-divider, rgba(58, 48, 40, 0.1));
}

.log-table {
  width: 100%;
  border-radius: var(--melodify-radius-md);
  overflow: hidden;
  --el-table-border-color: var(--melodify-border);
  --el-table-header-bg-color: var(--melodify-table-header-bg);
  --el-table-header-text-color: var(--melodify-strong);
  --el-table-row-hover-bg-color: rgba(0, 0, 0, 0.035);
}

.log-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.log-table :deep(th.el-table__cell) {
  font-weight: 600;
  font-size: 0.8125rem;
}

.points-pane .el-empty {
  padding: 2rem 1rem;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

@media (max-width: 900px) {
  .profile-shell {
    grid-template-columns: 1fr;
  }

  .profile-form-grid {
    grid-template-columns: 1fr;
  }

  .pane-body {
    padding: 1.15rem;
  }

  .profile-tabs :deep(.el-tabs__header) {
    padding: 0 1rem;
  }
}
</style>
