import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { resolvePlayableUrl } from '@/utils/audioUrl'

export interface PlayerTrack {
  title: string
  fileUrl: string
  subtitle?: string
}

/**
 * 全局迷你播放器：任意页面 push 曲目后，底部条统一控制同一 audio 实例。
 */
export const usePlayerStore = defineStore('player', () => {
  const current = ref<PlayerTrack | null>(null)
  const resolvedSrc = computed(() =>
    current.value ? resolvePlayableUrl(current.value.fileUrl) : '',
  )
  const paused = ref(true)

  const playTrack = (track: PlayerTrack) => {
    current.value = track
    paused.value = false
  }

  const setPaused = (p: boolean) => {
    paused.value = p
  }

  const clear = () => {
    current.value = null
    paused.value = true
  }

  return {
    current,
    resolvedSrc,
    paused,
    playTrack,
    setPaused,
    clear,
  }
})
