<script setup lang="ts">
/**
 * 顶栏铃铛：展示 WebSocket 推送后的收件箱快照；展开时清空未读角标。
 */
import { BellFilled } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useRealtimeNotificationStore } from '@/stores/realtimeNotifications'

defineOptions({ name: 'NotificationBell' })

const store = useRealtimeNotificationStore()
const { unreadCount, items } = storeToRefs(store)
const popVisible = ref(false)

const formatTime = (at: number) => {
  const d = new Date(at)
  return d.toLocaleString()
}

const onShow = () => {
  store.markAllRead()
}
</script>

<template>
  <el-popover
    v-model:visible="popVisible"
    placement="bottom-end"
    width="316"
    trigger="click"
    @show="onShow"
  >
    <template #reference>
      <span class="notify-trigger">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-button class="notify-btn" circle aria-label="打开站内通知列表">
            <el-icon><BellFilled /></el-icon>
          </el-button>
        </el-badge>
      </span>
    </template>
    <div class="notify-pop">
      <div class="notify-head">
        <span class="notify-title">站内通知</span>
        <small v-if="!items.length">生成任务完成时将在此提示（WebSocket）</small>
      </div>
      <template v-if="items.length">
        <div v-for="it in items" :key="it.id" class="notify-row">
          <div class="notify-row-title">
            {{ it.payload.success ? '生成完成' : '生成失败' }}
          </div>
          <div class="notify-row-body">
            <template v-if="it.payload.success">
              {{ it.payload.titlePreview?.trim() || '作品已就绪' }}
            </template>
            <template v-else>
              {{ it.payload.errorMessage?.trim() || '—' }}
            </template>
          </div>
          <div class="notify-row-meta">
            <span>{{ formatTime(it.at) }}</span>
            <RouterLink to="/works" class="notify-link" @click="popVisible = false">作品库</RouterLink>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无记录" :image-size="72" />
    </div>
  </el-popover>
</template>

<style scoped>
.notify-trigger {
  display: inline-flex;
  vertical-align: middle;
}

.notify-btn {
  --el-button-bg-color: rgba(var(--melodify-primary-rgb), 0.08);
  --el-button-border-color: rgba(var(--melodify-primary-rgb), 0.16);
  --el-button-hover-bg-color: rgba(var(--melodify-primary-rgb), 0.12);
  --el-button-hover-border-color: rgba(var(--melodify-primary-rgb), 0.24);
  --el-button-hover-text-color: var(--el-color-primary);
  --el-button-text-color: var(--el-color-primary);
}

.notify-btn:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 2px;
}

.notify-head {
  margin-bottom: 0.65rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(100, 90, 82, 0.12);
}

.notify-title {
  font-weight: 600;
  color: var(--melodify-strong);
  display: block;
}

.notify-row {
  padding: 0.5rem 0;
  border-bottom: 1px solid rgba(100, 90, 82, 0.08);
}

.notify-row:last-child {
  border-bottom: none;
}

.notify-row-title {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--melodify-strong);
}

.notify-row-body {
  margin-top: 0.2rem;
  font-size: 0.82rem;
  color: var(--melodify-muted);
  line-height: 1.45;
}

.notify-row-meta {
  margin-top: 0.35rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.72rem;
  color: var(--melodify-muted);
}

.notify-link {
  font-weight: 600;
  color: var(--el-color-primary);
  text-decoration: none;
}

.notify-link:hover {
  text-decoration: underline;
}

.notify-pop {
  max-height: 320px;
  overflow-y: auto;
}
</style>
