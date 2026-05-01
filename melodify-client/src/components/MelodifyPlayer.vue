<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { nextTick, ref, watch } from 'vue'
import { usePlayerStore } from '@/stores/player'

defineOptions({ name: 'MelodifyPlayer' })

const player = usePlayerStore()
const { current, resolvedSrc, paused } = storeToRefs(player)

const audioRef = ref<HTMLAudioElement | null>(null)

const syncPlayback = async () => {
  const el = audioRef.value
  if (!el) return
  if (!resolvedSrc.value) {
    el.pause()
    return
  }
  el.src = resolvedSrc.value
  el.load()
  await nextTick()
  if (!paused.value) {
    try {
      await el.play()
    } catch {
      player.setPaused(true)
    }
  } else {
    el.pause()
  }
}

watch(
  [current, resolvedSrc, paused],
  async () => {
    await nextTick()
    void syncPlayback()
  },
  { flush: 'post' },
)
</script>

<template>
  <div v-if="current" class="melodify-player" role="region" aria-label="全局播放器">
    <audio ref="audioRef" preload="metadata" @ended="player.setPaused(true)" />
    <div class="player-inner melodify-glass-card">
      <div class="track-meta">
        <span class="t">{{ current.title || '未命名' }}</span>
        <span v-if="current.subtitle" class="s">{{ current.subtitle }}</span>
      </div>
      <div class="player-actions">
        <button type="button" class="icon-btn" @click="player.setPaused(!paused)">
          {{ paused ? '播放' : '暂停' }}
        </button>
        <button type="button" class="icon-btn ghost" @click="player.clear()">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.melodify-player {
  position: fixed;
  left: 50%;
  bottom: 1.25rem;
  transform: translateX(-50%);
  z-index: 3000;
  width: min(32rem, calc(100vw - 2rem));
}

.player-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 1rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.96);
}

.track-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.track-meta .t {
  font-weight: 800;
  color: var(--melodify-strong, #0f172a);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-meta .s {
  font-size: 0.78rem;
  color: var(--melodify-muted, #64748b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.player-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}

.icon-btn {
  border: none;
  border-radius: 999px;
  padding: 0.35rem 0.85rem;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  background: var(--el-color-primary);
  color: #fff;
}

.icon-btn.ghost {
  background: #f1f5f9;
  color: var(--melodify-strong, #0f172a);
}

audio {
  display: none;
}
</style>
