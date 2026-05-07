<script setup lang="ts">
import { CircleCloseFilled, Monitor, SuccessFilled, User } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { fetchDashboardSummary, type AdminDashboardSummary } from '@/api/adminDashboard'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'AdminDashboardPage' })

const loading = ref(false)
const data = ref<AdminDashboardSummary | null>(null)

const load = async () => {
  loading.value = true
  try {
    data.value = unwrapResult(await fetchDashboardSummary())
  } catch (e) {
    showSubmitError(e, '加载仪表盘失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <div v-loading="loading" class="dash-root">
    <div v-if="data" class="dash-grid">
      <el-card class="metric" shadow="hover">
        <div class="metric-icon users">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <p class="metric-label">注册用户总数</p>
          <strong class="metric-value">{{ data.usersTotal }}</strong>
        </div>
      </el-card>
      <el-card class="metric" shadow="hover">
        <div class="metric-icon gen">
          <el-icon><Monitor /></el-icon>
        </div>
        <div>
          <p class="metric-label">生成中任务</p>
          <strong class="metric-value">{{ data.musicTasksGenerating }}</strong>
        </div>
      </el-card>
      <el-card class="metric" shadow="hover">
        <div class="metric-icon ok">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div>
          <p class="metric-label">今日成功完成</p>
          <strong class="metric-value">{{ data.musicTasksTodaySucceeded }}</strong>
          <span class="metric-sub">今日新建 {{ data.musicTasksTodayCreated }}</span>
        </div>
      </el-card>
      <el-card class="metric" shadow="hover">
        <div class="metric-icon bad">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div>
          <p class="metric-label">今日失败</p>
          <strong class="metric-value">{{ data.musicTasksTodayFailed }}</strong>
        </div>
      </el-card>
      <el-card class="metric wide" shadow="hover">
        <div>
          <p class="metric-label">今日模拟支付入账</p>
          <strong class="metric-value">{{ data.rechargePaidTodayYuanApprox }} 元</strong>
          <span class="metric-sub">
            {{ data.rechargePaidTodayCentTotal }} 分 · 订单 {{ data.rechargeOrdersPaidTodayCount }} 笔
          </span>
        </div>
      </el-card>
      <el-card class="metric wide" shadow="hover">
        <div>
          <p class="metric-label">公开作品总数</p>
          <strong class="metric-value">{{ data.publicAssetsTotal }}</strong>
        </div>
      </el-card>
    </div>
    <div v-if="data" class="dash-footnote">
      统计范围为服务器本地日：<strong>自然日零点至当前时刻</strong>；与运营列表页数据源一致。
    </div>
    <div v-if="!data && !loading" class="empty-hint">
      <el-button type="primary" @click="load">重新加载</el-button>
    </div>
  </div>
</template>

<style scoped>
.dash-root {
  min-height: 220px;
}

.dash-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
}

.metric {
  display: flex;
  align-items: center;
  gap: 1rem;
  border-radius: 12px;
}

.metric.wide {
  grid-column: span 1;
}

.metric-label {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.metric-value {
  font-size: 26px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  letter-spacing: -0.03em;
}

.metric-sub {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  flex: none;
  font-size: 22px;
}

.metric-icon.users {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.metric-icon.gen {
  background: linear-gradient(135deg, #0ea5e9, #22d3ee);
}
.metric-icon.ok {
  background: linear-gradient(135deg, #10b981, #34d399);
}
.metric-icon.bad {
  background: linear-gradient(135deg, #f97316, #fb923c);
}

.dash-footnote {
  margin-top: 1rem;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  padding: 1rem 0;
}
</style>
