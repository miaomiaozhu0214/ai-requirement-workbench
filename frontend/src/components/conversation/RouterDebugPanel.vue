<script setup lang="ts">
import { AlertTriangle, CheckCircle2, Route } from '@lucide/vue';
import { computed } from 'vue';
import type { AiAction } from '../../types';

const props = defineProps<{
  actions: AiAction[];
}>();

const routerTrace = computed(() => props.actions.find((action) => action.abilityType === 'intent_router') || null);

function statusClass(status?: string) {
  if (status === 'success') return 'green';
  if (status === 'failed') return 'red';
  return 'orange';
}
</script>

<template>
  <section class="panel router-panel">
    <div class="panel-head compact-head">
      <div>
        <h2>AI Router</h2>
        <p>当前会话最近一次路由识别</p>
      </div>
      <Route :size="17" />
    </div>
    <div v-if="routerTrace" class="router-body">
      <div class="router-topline">
        <span class="tag" :class="statusClass(routerTrace.status)">
          <component :is="routerTrace.status === 'success' ? CheckCircle2 : AlertTriangle" :size="13" />
          {{ routerTrace.status }}
        </span>
        <strong>{{ routerTrace.intent || '未返回 intent' }}</strong>
      </div>
      <div class="debug-grid">
        <span>后续动作</span>
        <strong>{{ routerTrace.nextActions?.join(', ') || '-' }}</strong>
        <span>模型</span>
        <strong>{{ routerTrace.modelName || '-' }}</strong>
        <span>Prompt</span>
        <strong>{{ routerTrace.promptTemplateCode || '-' }} {{ routerTrace.promptVersion || '' }}</strong>
        <span>耗时</span>
        <strong>{{ routerTrace.durationMs ?? 0 }} ms</strong>
      </div>
      <p v-if="routerTrace.errorMessage" class="debug-error">{{ routerTrace.errorMessage }}</p>
    </div>
    <div v-else class="router-empty">发送消息后会显示 Router 识别结果。</div>
  </section>
</template>

<style scoped>
.router-panel { overflow: hidden; }
.compact-head { min-height: 50px; padding-top: 10px; padding-bottom: 10px; }
.router-body { padding: 12px; display: grid; gap: 10px; }
.router-topline { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.router-topline strong { font-size: 14px; }
.tag { gap: 4px; }
.debug-grid { display: grid; grid-template-columns: 68px minmax(0, 1fr); gap: 6px 10px; font-size: 12px; }
.debug-grid span { color: var(--muted); }
.debug-grid strong { min-width: 0; color: #344054; word-break: break-word; }
.debug-error { margin: 0; border-radius: 8px; padding: 8px; background: var(--soft-red); color: #b42318; font-size: 12px; line-height: 1.5; }
.router-empty { padding: 16px 12px; color: var(--muted); font-size: 12px; text-align: center; }
</style>
