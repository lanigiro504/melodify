<script setup lang="ts">
/**
 * 个人中心：侧栏身份与快捷入口 + 资料 / 积分分区。
 */
import type { FormInstance, FormRules } from 'element-plus'
import type { PointLog } from '@/types/api'
import { Cpu, Headset, Trophy, Wallet } from '@element-plus/icons-vue'
import { reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { pageMyPointLogs } from '@/api/pointLogs'
import { useAuthStore } from '@/stores/auth'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'
import { validateFormRef } from '@/utils/validateFormRef'

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
  avatar: '',
  email: '',
  phone: '',
})

const profileRules: FormRules = {
  nickname: [{ max: 50, message: '昵称最多 50 个字符', trigger: 'blur' }],
  avatar: [{ max: 255, message: '头像地址最多 255 个字符', trigger: 'blur' }],
  email: [{ max: 100, message: '邮箱最多 100 个字符', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号最多 20 个字符', trigger: 'blur' }],
}

watch(
  () => auth.currentUser,
  (user) => {
    profileForm.nickname = user?.nickname ?? ''
    profileForm.avatar = user?.avatar ?? ''
    profileForm.email = user?.email ?? ''
    profileForm.phone = user?.phone ?? ''
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
      avatar: profileForm.avatar.trim(),
      email: profileForm.email.trim(),
      phone: profileForm.phone.trim(),
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

void auth.refreshMe().catch(() => {})
</script>

<template>
  <div class="page-stack profile-page">
    <div class="profile-shell">
      <aside class="profile-aside melodify-glass-card">
        <div class="aside-gradient" aria-hidden="true" />
        <div class="aside-inner">
          <div
            class="aside-avatar"
            :style="auth.currentUser?.avatar ? { backgroundImage: `url(${auth.currentUser.avatar})` } : {}"
          >
            <span v-if="!auth.currentUser?.avatar">{{ auth.avatarText }}</span>
          </div>
          <h1 class="aside-name">{{ auth.displayName || '创作者' }}</h1>
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

          <p class="aside-tip">资料与流水在右侧管理；生成完成会通过站内通知提醒您。</p>
        </div>
      </aside>

      <main class="profile-main soft-card">
        <el-tabs v-model="activeTab" class="profile-tabs" stretch>
          <el-tab-pane label="资料设置" name="profile">
            <div class="pane-body">
              <p class="pane-lead">更新对外展示信息与联系方式（头像支持填写图片 URL）。</p>
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
                <el-form-item label="头像地址" prop="avatar" class="full-span">
                  <el-input v-model="profileForm.avatar" placeholder="https://…（外链图片地址）" maxlength="255" clearable />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="选填" maxlength="100" clearable />
                </el-form-item>
                <el-form-item label="手机" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" clearable />
                </el-form-item>
                <div class="full-span form-actions">
                  <el-button type="primary" round size="large" :loading="savingProfile" @click="saveProfile">
                    保存资料
                  </el-button>
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
.profile-shell {
  display: grid;
  grid-template-columns: minmax(0, 17.5rem) minmax(0, 1fr);
  gap: 1.35rem;
  align-items: start;
}

.profile-aside {
  position: relative;
  overflow: hidden;
  border-radius: 1.35rem;
  padding: 0;
}

.aside-gradient {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 90% at 10% -10%, rgba(109, 93, 252, 0.35), transparent 52%),
    radial-gradient(80% 60% at 100% 0%, rgba(14, 165, 233, 0.18), transparent 45%);
  pointer-events: none;
}

.aside-inner {
  position: relative;
  padding: 1.65rem 1.35rem 1.45rem;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.85rem;
}

.aside-avatar {
  margin: 0 auto;
  width: 5.5rem;
  height: 5.5rem;
  border-radius: 1.65rem;
  background: linear-gradient(145deg, #6366f1, #8b5cf6);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 900;
  font-size: 1.85rem;
  background-size: cover;
  background-position: center;
  box-shadow:
    0 12px 32px rgba(99, 102, 241, 0.25),
    0 0 0 4px rgba(255, 255, 255, 0.75);
}

.aside-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 900;
  text-align: center;
  color: var(--melodify-strong);
  letter-spacing: -0.03em;
}

.aside-username {
  margin: -0.35rem 0 0;
  font-size: 0.82rem;
  text-align: center;
  color: var(--melodify-muted);
}

.aside-stat {
  margin-top: 0.35rem;
  padding: 0.95rem 1rem;
  border-radius: 1.15rem;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(99, 102, 241, 0.15);
}

.aside-stat-label {
  display: block;
  font-size: 0.72rem;
  color: var(--melodify-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.aside-stat-num {
  display: block;
  margin-top: 0.15rem;
  font-size: 1.85rem;
  font-weight: 900;
  color: #4f46e5;
  letter-spacing: -0.04em;
}

.aside-actions {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.aside-link {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.65rem 0.85rem;
  border-radius: 0.95rem;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--melodify-strong);
  text-decoration: none;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(248, 250, 252, 0.85);
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.aside-link:hover {
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.07);
  color: #4f46e5;
}

.aside-tip {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  line-height: 1.45;
  color: var(--melodify-muted);
  text-align: center;
}

.profile-main {
  border-radius: 1.35rem;
  padding: 0.25rem 0 0.5rem;
  min-height: 22rem;
}

.profile-tabs {
  --el-tabs-header-height: 48px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin: 0 1rem;
  padding-top: 0.35rem;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: rgba(148, 163, 184, 0.22);
}

.profile-tabs :deep(.el-tabs__item) {
  font-weight: 800;
  font-size: 0.95rem;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #4f46e5;
}

.pane-body {
  padding: 1rem 1.35rem 1.5rem;
}

.pane-lead {
  margin: 0 0 1.1rem;
  font-size: 0.88rem;
  color: var(--melodify-muted);
  line-height: 1.55;
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.25rem 1.15rem;
}

.full-span {
  grid-column: 1 / -1;
}

.form-actions {
  margin-top: 0.35rem;
}

.log-table {
  width: 100%;
  border-radius: 0.85rem;
  overflow: hidden;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 900px) {
  .profile-shell {
    grid-template-columns: 1fr;
  }

  .profile-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
