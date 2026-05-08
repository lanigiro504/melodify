<script setup lang="ts">
import {
  CircleCloseFilled,
  DataLine,
  Histogram,
  Monitor,
  PieChart,
  Refresh,
  SuccessFilled,
  User,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  fetchDashboardSummary,
  fetchDashboardTrends,
  type AdminDashboardSummary,
  type AdminDashboardTrends,
} from '@/api/adminDashboard'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'AdminDashboardPage' })

const PRIMARY = '#584d9e'
const ACCENT_BLUE = '#2563eb'
const ACCENT_GREEN = '#16a34a'
const ACCENT_ORANGE = '#ea580c'
const ACCENT_VIOLET = '#7c3aed'

const TASK_STATUS_LABEL: Record<number, string> = {
  0: '排队',
  1: '生成中',
  2: '成功',
  3: '失败',
  4: '已取消',
}

const trendDayOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 14 天', value: 14 },
  { label: '近 30 天', value: 30 },
]

const loading = ref(false)
const data = ref<AdminDashboardSummary | null>(null)
const trends = ref<AdminDashboardTrends | null>(null)
const trendDays = ref(14)

const chartTasksEl = ref<HTMLDivElement | null>(null)
const chartRechargeEl = ref<HTMLDivElement | null>(null)
const chartStatusEl = ref<HTMLDivElement | null>(null)

type ChartKey = 'tasks' | 'recharge' | 'status'

const chartInstances: Partial<Record<ChartKey, echarts.ECharts>> = {}

const getOrInitChart = (key: ChartKey, el: HTMLDivElement | null | undefined): echarts.ECharts | null => {
  if (!el) return null
  if (chartInstances[key]) {
    return chartInstances[key]!
  }
  const inst = echarts.init(el)
  chartInstances[key] = inst
  return inst
}

const disposeCharts = () => {
  ;(['tasks', 'recharge', 'status'] as const).forEach((k) => {
    chartInstances[k]?.dispose()
    delete chartInstances[k]
  })
}

const onWinResize = () => {
  ;(Object.values(chartInstances) as echarts.ECharts[]).forEach((c) => c.resize())
}

const buildTasksOption = (t: AdminDashboardTrends): echarts.EChartsOption => ({
  color: [ACCENT_BLUE, ACCENT_GREEN, ACCENT_ORANGE, PRIMARY],
  tooltip: { trigger: 'axis' },
  legend: {
    top: 0,
    icon: 'roundRect',
    textStyle: { fontSize: 12 },
    data: ['新建任务', '成功完结', '失败完结', '新注册用户'],
  },
  grid: { left: 48, right: 24, top: 40, bottom: 28 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: t.dayLabels,
    axisTick: { show: false },
    axisLabel: { color: '#71717a' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { type: 'dashed', opacity: 0.45 } },
    axisLabel: { color: '#71717a' },
  },
  series: [
    {
      name: '新建任务',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      emphasis: { focus: 'series' },
      data: t.musicTasksCreatedPerDay,
      areaStyle: { opacity: 0.08 },
    },
    {
      name: '成功完结',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      emphasis: { focus: 'series' },
      data: t.musicTasksSucceededPerDay,
      areaStyle: { opacity: 0.08 },
    },
    {
      name: '失败完结',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      emphasis: { focus: 'series' },
      data: t.musicTasksFailedPerDay,
      areaStyle: { opacity: 0.08 },
    },
    {
      name: '新注册用户',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      emphasis: { focus: 'series' },
      data: t.usersRegisteredPerDay,
      areaStyle: { opacity: 0.06 },
    },
  ],
})

const buildRechargeOption = (t: AdminDashboardTrends): echarts.EChartsOption => {
  const yuan = t.rechargePaidCentSumPerDay.map((c) => Math.round((c / 100) * 100) / 100)
  return {
    color: [PRIMARY, ACCENT_VIOLET],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      top: 0,
      data: ['已支付订单数', '支付金额(元)'],
    },
    grid: { left: 52, right: 52, top: 40, bottom: 28 },
    xAxis: {
      type: 'category',
      data: t.dayLabels,
      axisTick: { show: false },
      axisLabel: { color: '#71717a' },
    },
    yAxis: [
      {
        type: 'value',
        name: '笔数',
        minInterval: 1,
        splitLine: { lineStyle: { type: 'dashed', opacity: 0.45 } },
        axisLabel: { color: '#71717a' },
      },
      {
        type: 'value',
        name: '元',
        splitLine: { show: false },
        axisLabel: { color: '#71717a' },
      },
    ],
    series: [
      {
        name: '已支付订单数',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { borderRadius: [6, 6, 0, 0] },
        data: t.rechargePaidOrdersPerDay,
      },
      {
        name: '支付金额(元)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        data: yuan,
      },
    ],
  }
}

const buildStatusOption = (t: AdminDashboardTrends): echarts.EChartsOption => {
  const pieData = t.musicTaskStatusDistribution
    .filter((x) => x.count > 0)
    .map((x) => ({
      name: TASK_STATUS_LABEL[x.status] ?? `状态 ${x.status}`,
      value: x.count,
    }))
  if (!pieData.length) {
    return {
      title: {
        text: '暂无任务数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#a1a1aa', fontSize: 14, fontWeight: 500 },
      },
      series: [],
    }
  }
  return {
    color: [ACCENT_BLUE, PRIMARY, ACCENT_GREEN, ACCENT_ORANGE, '#94a3b8'],
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 条 ({d}%)' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      textStyle: { fontSize: 11 },
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['38%', '68%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { fontSize: 12 },
        data: pieData,
      },
    ],
  }
}

const applyCharts = () => {
  const tr = trends.value
  if (!tr) return
  const c1 = getOrInitChart('tasks', chartTasksEl.value)
  const c2 = getOrInitChart('recharge', chartRechargeEl.value)
  const c3 = getOrInitChart('status', chartStatusEl.value)
  c1?.setOption(buildTasksOption(tr), true)
  c2?.setOption(buildRechargeOption(tr), true)
  c3?.setOption(buildStatusOption(tr), true)
}

const loadAll = async () => {
  loading.value = true
  try {
    const [sum, tr] = await Promise.all([
      fetchDashboardSummary(),
      fetchDashboardTrends(trendDays.value),
    ])
    data.value = unwrapResult(sum)
    trends.value = unwrapResult(tr)
    await nextTick()
    applyCharts()
  } catch (e) {
    showSubmitError(e, '加载仪表盘失败')
  } finally {
    loading.value = false
  }
}

watch(trendDays, () => {
  void loadAll()
})

onMounted(() => {
  window.addEventListener('resize', onWinResize)
  void loadAll()
})

onUnmounted(() => {
  window.removeEventListener('resize', onWinResize)
  disposeCharts()
})
</script>

<template>
  <div v-loading="loading" class="dash-root">
    <div class="dash-head">
      <div class="dash-head__text">
        <p class="dash-head__eyebrow">Analytics</p>
        <h2 class="dash-head__title">数据概览</h2>
        <p class="dash-head__desc">关键指标与按日趋势；图表时间范围与下方「今日」卡片统计相互独立。</p>
      </div>
      <div class="dash-head__actions">
        <el-select v-model="trendDays" class="dash-range" aria-label="趋势图统计天数">
          <el-option v-for="o in trendDayOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <div v-if="data" class="dash-grid">
      <el-card class="metric" shadow="never">
        <div class="metric-icon users">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <p class="metric-label">注册用户总数</p>
          <strong class="metric-value">{{ data.usersTotal }}</strong>
        </div>
      </el-card>
      <el-card class="metric" shadow="never">
        <div class="metric-icon gen">
          <el-icon><Monitor /></el-icon>
        </div>
        <div>
          <p class="metric-label">生成中任务</p>
          <strong class="metric-value">{{ data.musicTasksGenerating }}</strong>
        </div>
      </el-card>
      <el-card class="metric" shadow="never">
        <div class="metric-icon ok">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div>
          <p class="metric-label">今日成功完成</p>
          <strong class="metric-value">{{ data.musicTasksTodaySucceeded }}</strong>
          <span class="metric-sub">今日新建 {{ data.musicTasksTodayCreated }}</span>
        </div>
      </el-card>
      <el-card class="metric" shadow="never">
        <div class="metric-icon bad">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div> 
          <p class="metric-label">今日失败</p>
          <strong class="metric-value">{{ data.musicTasksTodayFailed }}</strong>
        </div>
      </el-card>
      <el-card class="metric wide" shadow="never">
        <div>
          <p class="metric-label">今日模拟支付入账</p>
          <strong class="metric-value">{{ data.rechargePaidTodayYuanApprox }} 元</strong>
          <span class="metric-sub">
            {{ data.rechargePaidTodayCentTotal }} 分 · 订单 {{ data.rechargeOrdersPaidTodayCount }} 笔
          </span>
        </div>
      </el-card>
    </div>

    <template v-if="trends">
      <div class="chart-section-head">
        <el-icon><DataLine /></el-icon>
        <span>趋势分析</span>
        <small>（服务器本地日历日）</small>
      </div>
      <el-row class="chart-row" :gutter="16">
        <el-col :xs="24" :lg="16">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-card__hdr">
                <span>生成与用户</span>
                <small class="muted">新建 / 成功 / 失败 / 注册</small>
              </div>
            </template>
            <div ref="chartTasksEl" class="chart-host" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-card__hdr">
                <span><el-icon class="hdr-ico"><PieChart /></el-icon> 任务状态分布</span>
                <small class="muted">全库快照</small>
              </div>
            </template>
            <div ref="chartStatusEl" class="chart-host chart-host--pie" />
          </el-card>
        </el-col>
      </el-row>
      <el-row class="chart-row chart-row--last" :gutter="16">
        <el-col :span="24">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-card__hdr">
                <span><el-icon class="hdr-ico"><Histogram /></el-icon> 模拟支付（已支付）</span>
                <small class="muted">订单笔数 · 金额按日汇总</small>
              </div>
            </template>
            <div ref="chartRechargeEl" class="chart-host chart-host--wide" />
          </el-card>
        </el-col>
      </el-row>
    </template>

    <div v-if="data" class="dash-footnote">
      「今日」类卡片统计范围为<strong>当日 0 点至今</strong>；趋势图为所选天数内按<strong>自然日</strong>聚合。
    </div>
    <div v-if="!data && !loading" class="empty-hint">
      <el-button type="primary" @click="loadAll">重新加载</el-button>
    </div>
  </div>
</template>

<style scoped>
.dash-root {
  min-height: 220px;
}

.dash-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.15rem;
}

.dash-head__eyebrow {
  margin: 0 0 0.25rem;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--el-text-color-secondary);
}

.dash-head__title {
  margin: 0 0 0.35rem;
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--el-text-color-primary);
}

.dash-head__desc {
  margin: 0;
  max-width: 40rem;
  font-size: 13px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.dash-head__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
}

.dash-range {
  width: 124px;
}

.dash-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
}

.metric {
  border-radius: 14px;
  border: 1px solid var(--admin-border-strong, rgba(0, 0, 0, 0.1));
  box-shadow: none;
}

.metric :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
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
  border-radius: var(--el-border-radius-base);
  display: grid;
  place-items: center;
  flex: none;
  font-size: 22px;
  border: 1px solid transparent;
}

.metric-icon.users {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: rgba(var(--admin-primary-rgb), 0.14);
}

.metric-icon.gen {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: rgba(29, 78, 216, 0.12);
}

.metric-icon.ok {
  background: #f0fdf4;
  color: #15803d;
  border-color: rgba(21, 128, 61, 0.12);
}

.metric-icon.bad {
  background: #fff7ed;
  color: #c2410c;
  border-color: rgba(194, 65, 12, 0.14);
}

.chart-section-head {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 1.35rem 0 0.75rem;
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.chart-section-head small {
  font-weight: 500;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chart-row {
  margin-bottom: 0;
}

.chart-row--last {
  margin-top: 1rem;
  margin-bottom: 0.25rem;
}

.chart-card {
  border-radius: 14px;
  border: 1px solid var(--admin-border-strong, rgba(0, 0, 0, 0.1));
  box-shadow: none;
}

.chart-card :deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid var(--admin-border, rgba(0, 0, 0, 0.08));
}

.chart-card :deep(.el-card__body) {
  padding: 12px 16px 16px;
}

.chart-card__hdr {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.35rem 1rem;
  font-size: 14px;
  font-weight: 700;
}

.chart-card__hdr .muted {
  font-weight: 500;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.hdr-ico {
  vertical-align: -2px;
  margin-right: 4px;
}

.chart-host {
  width: 100%;
  height: 300px;
}

.chart-host--pie {
  height: 320px;
}

.chart-host--wide {
  height: 340px;
}

.dash-footnote {
  margin-top: 1rem;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.55;
}

.empty-hint {
  padding: 1rem 0;
}
</style>
