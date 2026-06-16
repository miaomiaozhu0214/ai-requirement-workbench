<script setup lang="ts">
import { Activity, FileText, FolderTree, MessageSquareText, PanelsTopLeft, Settings, SlidersHorizontal, Tags } from '@lucide/vue';
import ToastHost from './components/common/ToastHost.vue';
</script>

<template>
  <div class="shell">
    <aside class="side-nav">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div>
          <strong>AI需求工作台</strong>
          <span>MVP 第一阶段</span>
        </div>
      </div>
      <nav>
        <RouterLink to="/conversation">
          <MessageSquareText :size="18" />
          需求对话
        </RouterLink>
        <RouterLink to="/requirements">
          <PanelsTopLeft :size="18" />
          需求池
        </RouterLink>
        <RouterLink to="/feature-library">
          <FolderTree :size="18" />
          全量功能库
        </RouterLink>
        <RouterLink to="/product-lines">
          <Tags :size="18" />
          产品线配置
        </RouterLink>
        <RouterLink to="/ai/models">
          <Settings :size="18" />
          模型配置
        </RouterLink>
        <RouterLink to="/ai/prompts">
          <FileText :size="18" />
          Prompt模板
        </RouterLink>
        <RouterLink to="/ai/abilities">
          <SlidersHorizontal :size="18" />
          AI能力
        </RouterLink>
        <RouterLink to="/ai/traces">
          <Activity :size="18" />
          AI Trace
        </RouterLink>
      </nav>
      <div class="side-note">已覆盖：网页对话、候选需求卡片、正式需求入库、AI配置与Trace。</div>
    </aside>
    <main class="main">
      <RouterView />
    </main>
    <ToastHost />
  </div>
</template>
