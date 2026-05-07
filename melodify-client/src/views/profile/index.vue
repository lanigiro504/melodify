<script setup lang="ts">
/**
 * 个人中心：资料编辑 + 积分概览与近期流水入口。
 */
import type { FormInstance, FormRules } from 'element-plus'
import type { PointLog } from '@/types/api'
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
    <section class="page-hero melodify-glass-card profile-hero">
      <div>
        <p class="page-eyebrow">Account</p>
        <h1 class="page-title page-title--lg">个人中心</h1>
        <p class="page-desc page-desc--wide">
          管理创作者资料、查看积分余额与近期流水；生成完成时还会通过
          <strong>站内通知</strong>与 WebSocket 实时提醒。
        </p>
      </div>
      <div class="hero-side">
        <div class="points-pill">
          <span>可用积分</span>
          <strong>{{ auth.currentUser?.points ?? 0 }}</strong>
        </div>
        <RouterLink to="/recharge">
          <el-button type="primary" round>去充值</el-button>
        </RouterLink>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="profile-tabs" type="border-card">
      <el-tab-pane label="资料设置" name="profile">
        <div class="profile-layout">
          <div class="profile-card soft-card">
            <div class="identity">
              <span
                class="identity-avatar"
                :style="
                  auth.currentUser?.avatar ? { backgroundImage: `url(${auth.currentUser.avatar})` } : {}
                "
              >
                <span v-if="!auth.currentUser?.avatar">{{ auth.avatarText }}</span>
              </span>
              <div>
                <p class="identity-name">{{ auth.displayName || '未命名用户' }}</p>
                <p class="identity-sub">用户名：{{ auth.currentUser?.username }}</p>
              </div>
            </div>
            <el-form
              ref="profileRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
              class="profile-form"
            >
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="创作者昵称" maxlength="50" clearable />
              </el-form-item>
              <el-form-item label="头像地址" prop="avatar">
                <el-input v-model="profileForm.avatar" placeholder="https://..." maxlength="255" clearable />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="选填" maxlength="100" clearable />
              </el-form-item>
              <el-form-item label="手机" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" clearable />
              </el-form-item>
              <el-button type="primary" round :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="积分流水" name="points">
        <div v-loading="logsLoading" class="points-panel soft-card">
          <el-table v-if="logs.length" :data="logs" size="small" stripe style="width: 100%">
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                {{ changeTypeText(row.changeType) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="变动" width="90" />
            <el-table-column prop="balance" label="余额" width="90" />
            <el-table-column prop="bizType" label="业务" width="110" />
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          </el-table>
          <el-empty v-else-if="!logsLoading" description="暂无积分记录" />
          <div v-if="logTotal > logPager.size" class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
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
  </div>
</template>

<style scoped>
.profile-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.points-pill {
  border-radius: 999px;
  padding: 0.65rem 1.1rem;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: rgba(99, 102, 241, 0.06);
}

.points-pill span {
  display: block;
  font-size: 0.72rem;
  color: var(--melodify-muted);
}

.points-pill strong {
  font-size: 1.35rem;
  color: var(--melodify-strong);
}

.profile-tabs {
  margin-top: 1.25rem;
  border-radius: 1.1rem;
  overflow: hidden;
}

.profile-layout {
  padding: 0.5rem 0 0.25rem;
}

.profile-card {
  padding: 1.25rem 1.35rem;
}

.identity {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.identity-avatar {
  width: 3.6rem;
  height: 3.6rem;
  border-radius: 1.2rem;
  background: #6366f1;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 900;
  font-size: 1.2rem;
  background-size: cover;
  background-position: center;
}

.identity-name {
  margin: 0;
  font-weight: 900;
  font-size: 1.08rem;
  color: var(--melodify-strong);
}

.identity-sub {
  margin: 0.2rem 0 0;
  font-size: 0.85rem;
  color: var(--melodify-muted);
}

.profile-form {
  max-width: 520px;
}

.points-panel {
  padding: 1rem;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.75rem;
}
</style>
