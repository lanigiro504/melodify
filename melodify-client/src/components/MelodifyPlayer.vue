<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, nextTick, ref, watch } from 'vue'
import { usePlayerStore } from '@/stores/player'
import { splitLyricLines } from '@/utils/trackLyrics'

defineOptions({ name: 'MelodifyPlayer' })

const player = usePlayerStore()
const { current, resolvedSrc, paused } = storeToRefs(player)

const audioRef = ref<HTMLAudioElement | null>(null)
const lyricsBoxRef = ref<HTMLElement | null>(null)
const currentTime = ref(0)
const mediaDuration = ref(0)
const lastResolvedUrl = ref('')
const loadedSrc = ref('')

const lyricLines = computed(() => {
  const raw = current.value?.lyrics?.trim()
  if (!raw) return []
  return splitLyricLines(raw)
})

const activeLineIndex = computed(() => {
  const lines = lyricLines.value
  const n = lines.length
  if (!n) return -1
  let d = mediaDuration.value
  if (!d || !Number.isFinite(d) || d <= 0) {
    const hint = current.value?.durationSec
    if (hint != null && hint > 0) d = hint
  }
  if (!d || d <= 0) return 0
  const ratio = Math.min(1, Math.max(0, currentTime.value / d))
  return Math.min(n - 1, Math.floor(ratio * n))
})

const timeLabel = computed(() => {
  const fmt = (s: number) => {
    if (!Number.isFinite(s) || s < 0) return '0:00'
    const m = Math.floor(s / 60)
    const sec = Math.floor(s % 60)
    return `${m}:${sec.toString().padStart(2, '0')}`
  }
  return `${fmt(currentTime.value)} / ${fmt(mediaDuration.value)}`
})

const progressPercent = computed(() => {
  if (!mediaDuration.value) return 0
  return Math.min(100, Math.max(0, (currentTime.value / mediaDuration.value) * 100))
})

const syncPlayback = async () => {
  const el = audioRef.value
  if (!el) return
  if (!resolvedSrc.value) {
    el.pause()
    loadedSrc.value = ''
    return
  }
  if (loadedSrc.value !== resolvedSrc.value) {
    el.src = resolvedSrc.value
    el.load()
    loadedSrc.value = resolvedSrc.value
  }
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
    const url = resolvedSrc.value
    if (url !== lastResolvedUrl.value) {
      lastResolvedUrl.value = url
      currentTime.value = 0
      const hint = current.value?.durationSec
      mediaDuration.value = hint != null && hint > 0 ? hint : 0
    }
    if (!current.value) {
      lastResolvedUrl.value = ''
      loadedSrc.value = ''
      currentTime.value = 0
      mediaDuration.value = 0
    }
    void syncPlayback()
  },
  { flush: 'post' },
)

watch(activeLineIndex, async (idx) => {
  if (idx < 0 || !lyricsBoxRef.value) return
  await nextTick()
  const row = lyricsBoxRef.value.querySelector(`[data-line="${idx}"]`)
  row?.scrollIntoView({ block: 'center', behavior: 'smooth' })
})

function onTimeUpdate(e: Event) {
  const a = e.target as HTMLAudioElement
  currentTime.value = a.currentTime
  if (Number.isFinite(a.duration) && a.duration > 0) {
    mediaDuration.value = a.duration
  }
}

function onLoadedMetadata(e: Event) {
  const a = e.target as HTMLAudioElement
  if (Number.isFinite(a.duration) && a.duration > 0) {
    mediaDuration.value = a.duration
  }
}

function seekRatio(ratio: number) {
  const el = audioRef.value
  if (!el || !mediaDuration.value) return
  const r = Math.min(1, Math.max(0, ratio))
  el.currentTime = r * mediaDuration.value
  currentTime.value = el.currentTime
}
</script>

<template>
  <div
    v-if="current"
    class="melodify-player"
    :class="{ 'melodify-player--with-lyrics': lyricLines.length > 0 }"
    role="region"
    aria-label="全局播放器"
  >
    <audio
      ref="audioRef"
      preload="metadata"
      @ended="player.setPaused(true)"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
    />
    <div class="player-shell">
      <div class="player-top">
        <div class="disc" :class="{ 'disc--playing': !paused }">
          <span>{{ (current.title || 'M').slice(0, 1) }}</span>
        </div>
        <div class="track-meta">
          <span class="t">{{ current.title || '未命名' }}</span>
          <span v-if="current.subtitle" class="s">{{ current.subtitle }}</span>
          <span class="time">{{ timeLabel }}</span>
        </div>
        <div class="player-actions">
          <button type="button" class="icon-btn" @click="player.setPaused(!paused)">
            {{ paused ? '播放' : '暂停' }}
          </button>
          <button type="button" class="icon-btn ghost" @click="player.clear()">关闭</button>
        </div>
      </div>

      <div v-if="mediaDuration > 0" class="seek-wrap">
        <div class="seek-bg" aria-hidden="true">
          <span :style="{ width: `${progressPercent}%` }" />
        </div>
        <input
          class="seek"
          type="range"
          min="0"
          max="1000"
          step="1"
          :value="Math.round((currentTime / mediaDuration) * 1000)"
          @input="seekRatio(Number(($event.target as HTMLInputElement).value) / 1000)"
        />
      </div>

      <div v-if="lyricLines.length" class="lyrics-block">
        <p class="lyrics-hint">歌词预览 · 无时间轴时按播放进度大致同步</p>
        <div ref="lyricsBoxRef" class="lyrics-scroll">
          <p
            v-for="(line, i) in lyricLines"
            :key="i"
            :data-line="i"
            class="lyrics-line"
            :class="{ 'lyrics-line--active': i === activeLineIndex }"
          >
            {{ line }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.melodify-player {
  position: fixed;
  left: 50%;
  bottom: 1.05rem;
  transform: translateX(-50%);
  z-index: 3000;
  width: min(34rem, calc(100vw - 1.5rem));
}

.melodify-player--with-lyrics {
  width: min(40rem, calc(100vw - 1.5rem));
}

.player-shell {
  border-radius: 1.5rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    0 18px 48px rgba(15, 23, 42, 0.14),
    0 1px 0 rgba(255, 255, 255, 0.7) inset;
  backdrop-filter: blur(18px);
  padding: 0.8rem;
  max-height: min(70vh, 23rem);
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.player-top {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.disc {
  width: 2.75rem;
  height: 2.75rem;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 50%;
  color: #fff;
  font-weight: 900;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.96) 0 12%, transparent 13% 100%),
    linear-gradient(135deg, #6d5dfc, #22c55e);
  box-shadow: 0 10px 24px rgba(109, 93, 252, 0.24);
}

.disc--playing {
  animation: spin 9s linear infinite;
}

.track-meta {
  min-width: 0;
  flex: 1;
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

.track-meta .time {
  font-size: 0.72rem;
  color: var(--melodify-muted, #94a3b8);
  font-variant-numeric: tabular-nums;
}

.player-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}

.seek-wrap {
  position: relative;
  padding: 0.25rem 0;
}

.seek-bg {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  transform: translateY(-50%);
  background: #e5e7eb;
}

.seek-bg span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3b82f6, #6d5dfc);
}

.seek {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 1rem;
  margin: 0;
  opacity: 0;
  accent-color: var(--el-color-primary);
  cursor: pointer;
}

.lyrics-block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-height: 0;
  flex: 1;
}

.lyrics-hint {
  margin: 0;
  font-size: 0.7rem;
  color: #94a3b8;
  line-height: 1.4;
}

.lyrics-scroll {
  overflow-y: auto;
  max-height: 11.5rem;
  padding: 0.25rem;
  border-radius: 1rem;
  background: #f8fafc;
  scrollbar-width: thin;
}

.lyrics-line {
  margin: 0;
  padding: 0.45rem 0.65rem;
  border-radius: 0.75rem;
  font-size: 0.88rem;
  line-height: 1.55;
  color: var(--melodify-muted, #64748b);
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.lyrics-line--active {
  color: #2563eb;
  font-weight: 800;
  background: #eef2ff;
}

.icon-btn {
  border: none;
  border-radius: 999px;
  padding: 0.42rem 0.9rem;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(135deg, #3b82f6, #6d5dfc);
  color: #fff;
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.2);
}

.icon-btn.ghost {
  background: #f1f5f9;
  color: var(--melodify-strong, #0f172a);
  box-shadow: none;
}

audio {
  display: none;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 520px) {
  .player-top {
    align-items: flex-start;
  }

  .player-actions {
    flex-direction: column;
  }

  .icon-btn {
    padding-inline: 0.75rem;
  }
}
</style>
