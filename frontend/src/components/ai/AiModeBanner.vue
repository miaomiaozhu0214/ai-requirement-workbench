<script setup lang="ts">
import { AlertTriangle, CheckCircle2, FlaskConical } from '@lucide/vue';
import type { AiConfigStatus } from '../../types';

defineProps<{
  status: AiConfigStatus | null;
  loading?: boolean;
}>();
</script>

<template>
  <section
    class="mode-banner"
    :class="{
      mock: status?.provider === 'mock',
      ok: status?.provider !== 'mock' && status?.llmConfigured,
      warn: status?.provider !== 'mock' && status && !status.llmConfigured,
    }"
  >
    <component
      :is="status?.provider === 'mock' ? FlaskConical : status?.llmConfigured ? CheckCircle2 : AlertTriangle"
      :size="18"
    />
    <div>
      <strong>
        当前AI模式：{{ loading ? '检查中' : status?.modeLabel || '未知' }}
      </strong>
      <p v-if="status?.provider === 'mock'">当前服务使用 Mock AI，仅适合本地流程测试。</p>
      <p v-else-if="status?.llmConfigured">真实 LLM 配置完整，当前模型：{{ status.modelName || '未返回模型名' }}。</p>
      <p v-else-if="status">真实 LLM 配置不完整：{{ status.missingItems.join('；') }}</p>
      <p v-else>暂未获取到 AI 配置状态。</p>
    </div>
  </section>
</template>

<style scoped>
.mode-banner { border: 1px solid var(--line); border-radius: 8px; padding: 12px 14px; display: flex; align-items: flex-start; gap: 10px; background: white; }
.mode-banner strong { display: block; font-size: 14px; }
.mode-banner p { margin: 4px 0 0; color: var(--muted); font-size: 12px; line-height: 1.5; }
.mode-banner.ok { border-color: #bbf7d0; background: var(--soft-green); color: #027a48; }
.mode-banner.warn { border-color: #fed7aa; background: var(--soft-orange); color: #b54708; }
.mode-banner.mock { border-color: #bfdbfe; background: var(--soft-blue); color: #1d4ed8; }
.mode-banner.ok p, .mode-banner.warn p, .mode-banner.mock p { color: inherit; opacity: .88; }
</style>
