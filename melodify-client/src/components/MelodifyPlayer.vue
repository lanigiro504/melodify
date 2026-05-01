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
    const url = resolvedSrc.value
    if (url !== lastResolvedUrl.value) {
      lastResolvedUrl.value = url
      currentTime.value = 0
      const hint = current.value?.durationSec
      mediaDuration.value = hint != null && hint > 0 ? hint : 0
    }
    if (!current.value) {
      lastResolvedUrl.value = ''
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
    <div class="player-shell melodify-glass-card">
      <div class="player-top">
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
        <p class="lyrics-hint">歌词随进度大致高亮（按整首时长均分行；非 LRC 精确时间轴）</p>
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
  bottom: 1.25rem;
  transform: translateX(-50%);
  z-index: 3000;
  width: min(32rem, calc(100vw - 2rem));
}

.melodify-player--with-lyrics {
  width: min(38rem, calc(100vw - 2rem));
}

.player-shell {
  border-radius: 1.25rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.97);
  padding: 0.75rem 1rem 0.85rem;
  max-height: min(70vh, 22rem);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.player-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
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
  padding: 0 0.15rem;
}

.seek {
  width: 100%;
  height: 0.35rem;
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
  color: var(--melodify-muted, #94a3b8);
  line-height: 1.4;
}

.lyrics-scroll {
  overflow-y: auto;
  max-height: 11rem;
  padding-right: 0.25rem;
  scrollbar-width: thin;
}

.lyrics-line {
  margin: 0;
  padding: 0.35rem 0.5rem;
  border-radius: 0.5rem;
  font-size: 0.88rem;
  line-height: 1.55;
  color: var(--melodify-muted, #64748b);
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.lyrics-line--active {
  color: var(--el-color-primary);
  font-weight: 800;
  background: rgba(109, 93, 252, 0.08);
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
