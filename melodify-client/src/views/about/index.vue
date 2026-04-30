<script setup lang="ts">
/**
 * 关于页：产品与客户端能力说明（静态文案）。
 * 路由：/about ，目录约定同 views/login、views/register。
 */
defineOptions({ name: 'AboutPage' })
</script>

<template>
  <div class="about">
    <h1>关于 Melodify</h1>
    <p class="about__lead">
      Melodify 面向音乐创作者与爱好者，结合 AIGC 能力辅助完成灵感落地与作品迭代。
    </p>
    <p class="about__body">
      当前客户端提供账号注册与登录；后续将接入生成任务、素材管理与更多个性化功能。
    </p>
  </div>
</template>

<style scoped>
.about {
  padding-top: 0.5rem;
}

.about h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-heading);
  margin-bottom: 1rem;
}

.about__lead {
  font-size: 1.0625rem;
  line-height: 1.65;
  color: var(--color-text);
  margin-bottom: 1rem;
}

.about__body {
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}
</style>
