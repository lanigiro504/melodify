<script setup lang="ts">
import { CaretLeft, CaretRight, Close } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { computed, nextTick, ref, watch } from 'vue'
import { usePlayerStore } from '@/stores/player'

defineOptions({ name: 'MelodifyPlayer' })

const player = usePlayerStore()
const { current, resolvedSrc, paused } = storeToRefs(player)

const audioRef = ref<HTMLAudioElement | null>(null)
const currentTime = ref(0)
const mediaDuration = ref(0)
const lastResolvedUrl = ref('')
const loadedSrc = ref('')

const fmtTime = (s: number) => {
  if (!Number.isFinite(s) || s < 0) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

/** 有效总时长（与元数据同步的 mediaDuration → audio → hint） */
const totalDurationSec = computed(() => {
  if (mediaDuration.value > 0 && Number.isFinite(mediaDuration.value)) {
    return mediaDuration.value
  }
  const el = audioRef.value
  if (el && Number.isFinite(el.duration) && el.duration > 0) {
    return el.duration
  }
  const hint = current.value?.durationSec
  if (hint != null && Number(hint) > 0) {
    return Number(hint)
  }
  return 0
})

const elapsedLabel = computed(() => fmtTime(currentTime.value))
const durationLabel = computed(() => {
  const d = totalDurationSec.value
  if (!d || d <= 0) return '--:--'
  return fmtTime(d)
})

const progressPercent = computed(() => {
  const d = totalDurationSec.value
  if (!d) return 0
  return Math.min(100, Math.max(0, (currentTime.value / d) * 100))
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
  const dur = totalDurationSec.value
  if (!el || !dur) return
  const r = Math.min(1, Math.max(0, ratio))
  el.currentTime = r * dur
  currentTime.value = el.currentTime
}

function skipBy(deltaSec: number) {
  const el = audioRef.value
  if (!el) return
  let dur = totalDurationSec.value
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

      <section class="dock-bar" aria-label="全局播放器">
        <!-- 顶部：时间与进度在同一窄带内，避免与封面行挤压 -->
        <div v-if="totalDurationSec > 0" class="dock-progress">
          <span class="dock-time dock-time--elapsed">{{ elapsedLabel }}</span>
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
              :value="Math.round((currentTime / totalDurationSec) * 1000)"
              @input="seekRatio(Number(($event.target as HTMLInputElement).value) / 1000)"
            />
          </div>
          <span class="dock-time dock-time--total">{{ durationLabel }}</span>
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
            <button type="button" class="dock-skip" title="后退 10 秒" aria-label="后退10秒" @click="skipBy(-10)">
              <el-icon :size="20"><CaretLeft /></el-icon>
            </button>

            <button
              type="button"
              class="dock-play"
              :aria-label="paused ? '播放' : '暂停'"
              @click="player.setPaused(!paused)"
            >
              <span v-if="paused" class="dock-play__glyph dock-play__glyph--play" aria-hidden="true" />
              <span v-else class="dock-play__glyph dock-play__glyph--pause" aria-hidden="true" />
            </button>

            <button type="button" class="dock-skip" title="前进 10 秒" aria-label="前进10秒" @click="skipBy(10)">
              <el-icon :size="20"><CaretRight /></el-icon>
            </button>
          </div>

          <div class="dock-tools">
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
  gap: 0;
  padding-bottom: max(14px, env(safe-area-inset-bottom));
  pointer-events: none;
}

.player-root > * {
  pointer-events: auto;
}

.player-root audio {
  display: none;
}

/* ——底栏（浅色 + 主色） */
.dock-bar {
  width: min(640px, calc(100vw - 28px));
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(0, 0, 0, 0.09);
  border-radius: var(--melodify-radius-lg);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.95) inset,
    0 10px 36px rgba(0, 0, 0, 0.09),
    0 2px 8px rgba(0, 0, 0, 0.04);
  backdrop-filter: saturate(140%) blur(18px);
  overflow: hidden;
  isolation: isolate;
}

.dock-progress {
  display: flex;
  align-items: center;
  gap: 0.52rem;
  padding: 0.4rem 0.95rem 0.32rem;
  border-bottom: 1px solid var(--melodify-divider, rgba(0, 0, 0, 0.06));
}

.dock-time {
  flex: none;
  width: 2.72rem;
  font-size: 0.7rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.dock-time--elapsed {
  color: var(--melodify-strong);
  text-align: left;
}

.dock-time--total {
  color: var(--melodify-muted);
  text-align: right;
}

.dock-track-hit {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 1.35rem;
  display: flex;
  align-items: center;
  cursor: pointer;
  border-radius: 999px;
  outline: none;
}

.dock-track-hit:focus-visible {
  box-shadow: 0 0 0 2px rgba(var(--melodify-primary-rgb), 0.22);
}

.dock-track {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 4px;
  margin-top: -2px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.dock-track__fill {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  transform-origin: left center;
  background: var(--el-color-primary);
  will-change: transform;
}

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
  gap: 0.7rem;
  padding: 0.65rem 0.95rem 0.78rem;
  min-height: 3.65rem;
}

.dock-cover-wrap {
  flex: none;
}

.dock-cover {
  width: 3rem;
  height: 3rem;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 1.12rem;
  font-weight: 800;
  color: #fff;
  background:
    radial-gradient(ellipse 80% 65% at 30% 18%, rgba(255, 255, 255, 0.28) 0%, transparent 52%),
    linear-gradient(150deg, var(--el-color-primary-light-5), var(--el-color-primary));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 2px 8px rgba(var(--melodify-primary-rgb), 0.22);
}

.dock-cover--live {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 3px 12px rgba(var(--melodify-primary-rgb), 0.26),
    0 0 0 1px rgba(var(--melodify-primary-rgb), 0.16);
}

.dock-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--melodify-strong);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-sub {
  margin: 0.1rem 0 0;
  font-size: 0.73rem;
  color: var(--melodify-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-info {
  min-width: 0;
}

.dock-transport {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  flex-shrink: 0;
}

/* 主键：扁平实心圆 + 纯形状图标（无 Element「带圈」视频图标） */
.dock-play {
  width: 2.65rem;
  height: 2.65rem;
  border-radius: 50%;
  border: none;
  margin: 0 0.15rem;
  padding: 0;
  display: grid;
  place-items: center;
  cursor: pointer;
  background: var(--el-color-primary);
  transition: opacity 0.12s ease, transform 0.1s ease;
}

.dock-play__glyph {
  display: block;
  flex-shrink: 0;
}

/* 三角播放：clip-path 比 border 三角更易对齐 */
.dock-play__glyph--play {
  width: 0.55rem;
  height: 0.7rem;
  margin-left: 0.12rem;
  background: #fff;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
}

/* 暂停：双竖条（整像素 + flex 间距，避免 rem 亚像素导致左右粗细不一） */
.dock-play__glyph--pause {
  display: inline-flex;
  align-items: stretch;
  justify-content: center;
  gap: 5px;
  width: 11px;
  height: 12px;
  box-sizing: border-box;
}

.dock-play__glyph--pause::before,
.dock-play__glyph--pause::after {
  content: '';
  flex: 0 0 3px;
  width: 3px;
  min-width: 3px;
  border-radius: 0;
  background: #fff;
}

.dock-play:hover {
  opacity: 0.92;
}

.dock-play:active {
  opacity: 0.88;
  transform: scale(0.97);
}

/* 次键：仅图标，浅灰 → 主色 */
.dock-skip {
  width: 2.35rem;
  height: 2.35rem;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--melodify-muted);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    color 0.12s ease,
    background 0.12s ease;
}

.dock-skip:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.dock-skip:active {
  background: color-mix(in srgb, var(--el-color-primary-light-9) 85%, var(--el-color-primary));
}

.dock-tools {
  display: inline-flex;
  align-items: center;
  gap: 0.15rem;
}

.dock-close {
  width: 2.15rem;
  height: 2.15rem;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--melodify-subtle);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    background 0.14s ease,
    color 0.14s ease;
}

.dock-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--melodify-strong);
}

@media (max-width: 520px) {
  .dock-progress {
    padding-inline: 0.85rem;
    gap: 0.42rem;
  }

  .dock-main {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 0.5rem 0.6rem;
    padding: 0.6rem 0.82rem 0.72rem;
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
    margin-top: 0.06rem;
    gap: 0.4rem;
  }
}

@media (max-width: 400px) {
  .dock-time {
    width: 2.6rem;
    font-size: 0.65rem;
  }

  .dock-play {
    width: 2.5rem;
    height: 2.5rem;
  }

  .dock-skip {
    width: 2.2rem;
    height: 2.2rem;
  }
}
</style>
