<script setup lang="ts">
import { CaretLeft, CaretRight, Close, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { computed, nextTick, ref, watch } from 'vue'
import { usePlayerStore } from '@/stores/player'
import { splitLyricLines } from '@/utils/trackLyrics'

defineOptions({ name: 'MelodifyPlayer' })

const player = usePlayerStore()
const { current, resolvedSrc, paused } = storeToRefs(player)

const audioRef = ref<HTMLAudioElement | null>(null)
const lyricsScrollRef = ref<HTMLElement | null>(null)
const lyricsExpanded = ref(false)
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

const fmtTime = (s: number) => {
  if (!Number.isFinite(s) || s < 0) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

const elapsedLabel = computed(() => fmtTime(currentTime.value))
const durationLabel = computed(() => fmtTime(mediaDuration.value))

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

watch(current, () => {
  lyricsExpanded.value = false
})

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
  if (!lyricsExpanded.value || idx < 0 || !lyricsScrollRef.value) return
  await nextTick()
  const row = lyricsScrollRef.value.querySelector(`[data-line="${idx}"]`)
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

function skipBy(deltaSec: number) {
  const el = audioRef.value
  if (!el) return
  let dur = mediaDuration.value
  if (!dur || dur <= 0) {
    const d = el.duration
    dur = Number.isFinite(d) && d > 0 ? d : 0
  }
  if (!dur) return
  const next = Math.min(dur, Math.max(0, el.currentTime + deltaSec))
  el.currentTime = next
  currentTime.value = next
}

</script>

<template>
  <Teleport to="body">
    <div v-if="current" class="player-root">
      <audio
        ref="audioRef"
        preload="metadata"
        @ended="player.setPaused(true)"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMetadata"
      />

      <!-- 歌词层叠在底栏之上，独立于底栏裁剪 -->
      <div
        v-if="lyricLines.length && lyricsExpanded"
        class="dock-lyrics melodify-glass-card"
      >
        <p class="dock-lyrics__hint">歌词预览 · 无时间轴时按进度粗略同步</p>
        <div ref="lyricsScrollRef" class="dock-lyrics__scroll">
          <p
            v-for="(line, i) in lyricLines"
            :key="i"
            :data-line="i"
            class="dock-lyrics__line"
            :class="{ 'dock-lyrics__line--on': i === activeLineIndex }"
          >
            {{ line }}
          </p>
        </div>
      </div>

      <section class="dock-bar" aria-label="全局播放器">
        <!-- 顶部：时间与进度在同一窄带内，避免与封面行挤压 -->
        <div v-if="mediaDuration > 0" class="dock-progress">
          <span class="dock-time">{{ elapsedLabel }}</span>
          <div class="dock-track-hit">
            <div class="dock-track">
              <div class="dock-track__fill" :style="{ transform: `scaleX(${progressPercent / 100})` }" />
            </div>
            <input
              class="dock-range"
              type="range"
              min="0"
              max="1000"
              step="1"
              aria-label="播放进度"
              :value="Math.round((currentTime / mediaDuration) * 1000)"
              @input="seekRatio(Number(($event.target as HTMLInputElement).value) / 1000)"
            />
          </div>
          <span class="dock-time">{{ durationLabel }}</span>
        </div>

        <div class="dock-main">
          <div class="dock-cover-wrap">
            <div class="dock-cover" :class="{ 'dock-cover--live': !paused }">
              <span>{{ (current.title || 'M').slice(0, 1) }}</span>
            </div>
          </div>

          <div class="dock-info">
            <p class="dock-title" :title="current.title">{{ current.title || '未命名' }}</p>
            <p class="dock-sub">
              {{ current.subtitle || 'Melodify' }}
            </p>
          </div>

          <div class="dock-transport" aria-label="播放控制">
            <button type="button" class="dock-icon-btn" aria-label="后退10秒" @click="skipBy(-10)">
              <el-icon><CaretLeft /></el-icon>
              <span class="dock-icon-btn__cap">10s</span>
            </button>

            <button type="button" class="dock-play" @click="player.setPaused(!paused)">
              <el-icon v-if="paused" :size="28"><VideoPlay /></el-icon>
              <el-icon v-else :size="28"><VideoPause /></el-icon>
            </button>

            <button type="button" class="dock-icon-btn" aria-label="前进10秒" @click="skipBy(10)">
              <span class="dock-icon-btn__cap">10s</span>
              <el-icon><CaretRight /></el-icon>
            </button>
          </div>

          <div class="dock-tools">
            <button
              v-if="lyricLines.length"
              type="button"
              class="dock-tool-text"
              @click="lyricsExpanded = !lyricsExpanded"
            >
              {{ lyricsExpanded ? '收起' : '歌词' }}
            </button>
            <button type="button" class="dock-close" aria-label="关闭播放器" @click="player.clear()">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.player-root {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 5000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 0.65rem;
  padding-bottom: max(14px, env(safe-area-inset-bottom));
  pointer-events: none;
}

.player-root > * {
  pointer-events: auto;
}

.player-root audio {
  display: none;
}

/* ——歌词面板（在底栏外，不参与底栏 overflow 裁剪） */
.dock-lyrics {
  width: min(640px, calc(100vw - 28px));
  padding: 0.75rem 1rem 0.95rem;
  max-height: min(38vh, 15.5rem);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  border-radius: var(--melodify-radius-lg);
  box-sizing: border-box;
}

.dock-lyrics__hint {
  margin: 0;
  font-size: 0.68rem;
  color: var(--melodify-subtle);
}

.dock-lyrics__scroll {
  overflow-y: auto;
  margin: 0 -0.15rem;
  padding: 0 0.15rem;
  max-height: calc(min(38vh, 15.5rem) - 2rem);
  scrollbar-width: thin;
}

.dock-lyrics__line {
  margin: 0;
  padding: 0.42rem 0.55rem;
  border-radius: var(--melodify-radius-sm);
  font-size: 0.86rem;
  line-height: 1.52;
  color: var(--melodify-muted);
}

.dock-lyrics__line--on {
  color: var(--el-color-primary);
  font-weight: 700;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 88%, transparent);
}

/* ——底栏 Spotify 式实心块（圆角 capsule） */
.dock-bar {
  width: min(640px, calc(100vw - 28px));
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(0, 0, 0, 0.09);
  border-radius: var(--melodify-radius-lg);
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.1),
    0 4px 12px rgba(0, 0, 0, 0.04);
  backdrop-filter: saturate(140%) blur(18px);
  overflow: hidden;
  isolation: isolate;
}

.dock-progress {
  display: flex;
  align-items: center;
  gap: 0.62rem;
  padding: 0.55rem 0.95rem 0.35rem;
  border-bottom: 1px solid var(--melodify-divider, rgba(0, 0, 0, 0.06));
}

.dock-time {
  flex: none;
  width: 2.85rem;
  font-size: 0.688rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--melodify-muted);
}

.dock-time:last-child {
  text-align: right;
}

.dock-track-hit {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 1.65rem;
  display: flex;
  align-items: center;
  cursor: pointer;
  border-radius: 999px;
  outline: none;
}

.dock-track-hit:focus-within {
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
  border-radius: 999px;
}

.dock-track {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 5px;
  margin-top: -2.5px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.09);
  overflow: hidden;
}

.dock-track__fill {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  transform-origin: left center;
  background: linear-gradient(
    90deg,
    var(--el-color-primary-light-7),
    var(--el-color-primary)
  );
  will-change: transform;
}

/* 透明 range 叠在轨道上，拖拽命中区大且不「画出界」 */
.dock-range {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.dock-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.65rem;
  padding: 0.7rem 0.95rem 0.85rem;
  min-height: 3.85rem;
}

.dock-cover-wrap {
  flex: none;
}

.dock-cover {
  width: 3.1rem;
  height: 3.1rem;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 1.2rem;
  font-weight: 800;
  color: #fff;
  background:
    radial-gradient(circle at 35% 30%, rgba(255, 255, 255, 0.3) 0 45%, transparent 46%),
    linear-gradient(145deg, var(--el-color-primary-light-5), var(--el-color-primary));
  box-shadow: 0 4px 14px rgba(var(--melodify-primary-rgb), 0.32);
}

.dock-cover--live {
  box-shadow:
    0 4px 16px rgba(var(--melodify-primary-rgb), 0.35),
    0 0 0 2px color-mix(in srgb, var(--el-color-primary-light-9) 70%, transparent);
}

.dock-info {
  min-width: 0;
  flex: none;
}

.dock-title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--melodify-strong);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-sub {
  margin: 0.1rem 0 0;
  font-size: 0.75rem;
  color: var(--melodify-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-transport {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-shrink: 0;
}

.dock-play {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  display: grid;
  place-items: center;
  border: none;
  cursor: pointer;
  color: #fff;
  background: var(--el-color-primary);
  box-shadow: 0 6px 16px rgba(var(--melodify-primary-rgb), 0.3);
  transition:
    transform 0.12s ease,
    background-color 0.15s ease;
}

.dock-play:hover {
  transform: scale(1.06);
  background-color: color-mix(in srgb, var(--el-color-primary) 88%, black);
}

.dock-play:active {
  transform: scale(0.98);
}

.dock-icon-btn {
  height: 2.65rem;
  min-width: 2.95rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(245, 245, 246, 0.95);
  color: var(--melodify-strong);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 0.625rem;
  font-weight: 800;
}

.dock-icon-btn:hover {
  background: rgba(237, 237, 240, 1);
}

.dock-icon-btn__cap {
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
  letter-spacing: -0.02em;
}

.dock-tools {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}

.dock-tool-text {
  padding: 0.35rem 0.55rem;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.78rem;
  color: var(--el-color-primary);
  background: transparent;
}

.dock-tool-text:hover {
  background: var(--el-color-primary-light-9);
}

.dock-close {
  width: 2.35rem;
  height: 2.35rem;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--melodify-muted);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.dock-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--melodify-strong);
}

@media (max-width: 520px) {
  .dock-progress {
    padding-inline: 0.75rem;
    gap: 0.42rem;
  }

  .dock-main {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 0.5rem 0.6rem;
    padding: 0.65rem 0.82rem 0.75rem;
  }

  .dock-cover-wrap {
    flex: none;
  }

  .dock-info {
    flex: 1;
    min-width: 0;
  }

  .dock-tools {
    flex: none;
    margin-left: auto;
    align-self: center;
    display: flex;
    align-items: center;
  }

  .dock-transport {
    width: 100%;
    justify-content: center;
    order: 9;
    margin-top: 0.08rem;
  }
}

@media (max-width: 400px) {
  .dock-time {
    width: 2.65rem;
    font-size: 0.65rem;
  }

  .dock-icon-btn__cap {
    display: none;
  }

  .dock-icon-btn {
    min-width: 2.55rem;
  }
}
</style>
