<script setup lang="ts">
/**
 * 关于页：产品与客户端能力说明（静态文案）。
 * 路由：/about ，目录约定同 views/login、views/register。
 */
defineOptions({ name: 'AboutPage' })
</script>

<template>
  <div class="about melodify-glass-card">
    <p class="page-eyebrow">About</p>
    <h1 class="page-title page-title--xl">面向灵感捕捉的 AI 音乐工作台</h1>
    <p class="page-desc about__lead">
      Melodify 面向音乐创作者与爱好者，结合 AIGC 能力辅助完成灵感落地、任务追踪与作品管理。
    </p>
    <div class="section-grid section-grid--3">
      <section class="soft-card">
        <h2>当前能力</h2>
        <p>账号注册登录、积分扣费、AI 生成任务、作品库试听、失败自动退费。</p>
      </section>
      <section class="soft-card">
        <h2>适合场景</h2>
        <p>歌曲 demo、短视频配乐、风格草稿、歌词旋律灵感探索。</p>
      </section>
      <section class="soft-card">
        <h2>后续计划</h2>
        <p>收藏夹、标签、批量生成、作品发布、Webhook 回调和自有存储。</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.about {
  padding: clamp(1.5rem, 4vw, 2.2rem);
}

.about__lead {
  max-width: 48rem;
  font-size: 1.08rem;
  margin-bottom: 1.4rem;
}

.about section {
  padding: 1.25rem;
}

.about h2 {
  color: var(--melodify-strong);
  font-size: 1.05rem;
  font-weight: 900;
  margin-bottom: 0.45rem;
}

.about section p {
  color: var(--melodify-muted);
  line-height: 1.7;
}
</style>
