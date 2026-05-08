<script setup lang="ts">
/**
 * 点赞爱心：空心 + 灰描边，已赞实心 + 红色（不依赖 Element 图标库里的 Heart）。
 */
withDefaults(
  defineProps<{
    filled?: boolean
    /** 边长 px */
    size?: number
  }>(),
  {
    filled: false,
    size: 18,
  },
)
</script>

<template>
  <svg
    class="like-heart-icon"
    :class="{ 'like-heart-icon--filled': filled }"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    aria-hidden="true"
    focusable="false"
  >
    <!-- Feather「heart」轮廓；实心时仅填色、取消描边 -->
    <path
      class="like-heart-icon__path"
      d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
    />
  </svg>
</template>

<style scoped>
.like-heart-icon {
  display: block;
  flex-shrink: 0;
  vertical-align: middle;
  color: var(--melodify-muted, #94a3b8);
}

.like-heart-icon__path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.85;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition:
    fill 0.15s ease,
    stroke 0.15s ease;
}

.like-heart-icon--filled {
  color: #ef4444;
}

.like-heart-icon--filled .like-heart-icon__path {
  fill: currentColor;
  stroke: none;
}
</style>
