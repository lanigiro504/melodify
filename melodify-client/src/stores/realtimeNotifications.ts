/**
 * C 端站内通知：通过 WebSocket 接收 {@code GENERATION_FINISHED} 等消息，供顶栏铃铛与 Element 通知展示。
 * 连接形如 {@code ws(s)://宿主/ws/notifications?token=JWT}；JWT 与 REST 共用 sessionStorage 令牌。
 *
 * @see melodify-backend MelodifyClientWebSocketHandler
 */
import { ElNotification } from 'element-plus'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getStoredToken } from '@/utils/sessionCredentials'

const MAX_INBOX = 50

export type GenerationFinishedPayload = {
  type: 'GENERATION_FINISHED'
  taskBizId: string
  success: boolean
  titlePreview: string
  errorMessage: string
}

export type InboxItem = {
  id: string
  at: number
  payload: GenerationFinishedPayload
  read: boolean
}

function buildWsUrl(token: string): string {
  const proto = typeof location !== 'undefined' && location.protocol === 'https:' ? 'wss' : 'ws'
  const host = typeof location !== 'undefined' ? location.host : 'localhost:5173'
  return `${proto}://${host}/ws/notifications?token=${encodeURIComponent(token)}`
}

export const useRealtimeNotificationStore = defineStore('realtimeNotifications', () => {
  let socket: WebSocket | null = null
  const connected = ref(false)
  const items = ref<InboxItem[]>([])

  const unreadCount = computed(() => items.value.filter((x) => !x.read).length)

  function formatNotifyMessage(p: GenerationFinishedPayload): string {
    if (!p.success) {
      const err = p.errorMessage?.trim() || '生成未成功，请稍后在作品库查看任务状态'
      return err.length > 160 ? `${err.slice(0, 157)}…` : err
    }
    const name = p.titlePreview?.trim()
    if (name) {
      const short = name.length > 36 ? `${name.slice(0, 34)}…` : name
      return `「${short}」已就绪，可在作品库试听`
    }
    return '新作品已生成，可在作品库试听'
  }

  function pushInbox(parsed: GenerationFinishedPayload) {
    const id =
      `${parsed.taskBizId || 'unknown'}-${parsed.success ? 'ok' : 'fail'}-${Date.now().toString(36)}`
    items.value = [
      { id, at: Date.now(), payload: parsed, read: false },
      ...items.value.filter((x) => x.id !== id),
    ].slice(0, MAX_INBOX)

    ElNotification({
      title: parsed.success ? '生成完成' : '生成失败',
      message: formatNotifyMessage(parsed),
      type: parsed.success ? 'success' : 'error',
      duration: parsed.success ? 4200 : 7000,
      offset: 68,
      showClose: true,
      customClass: 'melodify-notification',
    })
  }

  function handleRawMessage(raw: string) {
    let data: Record<string, unknown>
    try {
      data = JSON.parse(raw) as Record<string, unknown>
    } catch {
      return
    }
    if (data.type === 'GENERATION_FINISHED') {
      pushInbox({
        type: 'GENERATION_FINISHED',
        taskBizId: String(data.taskBizId ?? ''),
        success: Boolean(data.success),
        titlePreview: String(data.titlePreview ?? ''),
        errorMessage: String(data.errorMessage ?? ''),
      })
    }
  }

  function disconnect() {
    if (socket != null) {
      try {
        socket.close()
      } catch {
        /* ignore */
      }
      socket = null
    }
    connected.value = false
  }

  function connect(): void {
    const token = getStoredToken()
    if (!token) {
      disconnect()
      return
    }
    disconnect()
    try {
      socket = new WebSocket(buildWsUrl(token))
    } catch {
      connected.value = false
      return
    }
    socket.onopen = () => {
      connected.value = true
    }
    socket.onclose = () => {
      connected.value = false
    }
    socket.onerror = () => {
      connected.value = false
    }
    socket.onmessage = (ev) => {
      if (typeof ev.data === 'string') handleRawMessage(ev.data)
    }
  }

  function markAllRead() {
    items.value = items.value.map((x) => ({ ...x, read: true }))
  }

  return {
    connected,
    items,
    unreadCount,
    connect,
    disconnect,
    markAllRead,
  }
})
