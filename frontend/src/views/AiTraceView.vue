<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { aiTraceApi } from '../api/aiTraceApi';
import type { AiTrace } from '../types';

const traces = ref<AiTrace[]>([]);
const selectedTrace = ref<AiTrace | null>(null);
const loading = ref(false);
const errorMessage = ref('');

function traceKey(trace: AiTrace) {
  // 兼容会话详情里的 traceId 和 Trace 列表里的 id，避免长整型字段映射差异导致行选择失效。
  return trace.traceId ?? trace.id;
}

async function load() {
  loading.value = true;
  errorMessage.value = '';
  try {
    traces.value = await aiTraceApi.list();
    selectedTrace.value = traces.value[0] ?? null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载Trace失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function select(trace: AiTrace) {
  selectedTrace.value = trace;
}

function statusClass(status: string) {
  if (status === 'success') return 'green';
  if (status === 'failed') return 'red';
  return 'orange';
}

function formatJson(value: unknown) {
  return value == null ? '{}' : JSON.stringify(value, null, 2);
}

function promptLabel(trace: AiTrace) {
  // 列表优先展示模板编码，方便快速确认是否命中了 default_intent_router 等关键 Prompt。
  const name = trace.promptTemplateCode || trace.promptTemplateName || '未绑定Prompt';
  return `${name}${trace.promptVersion ? ` · ${trace.promptVersion}` : ''}`;
}

onMounted(load);
</script>

<template>
  <section class="trace-page">
    <header class="page-head">
      <div class="page-title">
        <h1>AI Trace</h1>
        <p>查看 Router、Prompt 调用、模型、输出 JSON、耗时、Token 与错误信息</p>
      </div>
      <button class="btn" type="button" :disabled="loading" @click="load">刷新</button>
    </header>

    <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

    <section class="page-body trace-layout">
      <section class="panel trace-list-panel">
        <div class="panel-head">
          <div>
            <h2>最近调用</h2>
            <p>最多显示 50 条 Trace</p>
          </div>
        </div>
        <div class="trace-table-wrap">
          <table>
            <thead>
              <tr>
                <th>状态</th>
                <th>能力</th>
                <th>输入摘要</th>
                <th>模型 / Prompt</th>
                <th>耗时</th>
                <th>Token</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="trace in traces"
                :key="traceKey(trace)"
                class="trace-row"
                :class="{ active: selectedTrace && traceKey(trace) === traceKey(selectedTrace) }"
                @click="select(trace)"
              >
                <td><span class="tag" :class="statusClass(trace.status)">{{ trace.status }}</span></td>
                <td>
                  <strong>{{ trace.abilityType }}</strong>
                  <span v-if="trace.intent" class="muted-line">intent: {{ trace.intent }}</span>
                  <span v-if="trace.nextActions?.length" class="muted-line">next: {{ trace.nextActions.join(', ') }}</span>
                </td>
                <td class="summary-cell">{{ trace.inputSummary || '-' }}</td>
                <td>
                  <strong>{{ trace.modelName || '-' }}</strong>
                  <span class="muted-line">{{ promptLabel(trace) }}</span>
                </td>
                <td>{{ trace.durationMs ?? 0 }} ms</td>
                <td>{{ trace.tokenInput ?? '' }} / {{ trace.tokenOutput ?? '' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!loading && traces.length === 0" class="empty">暂无 AI Trace。发送一条对话消息后会生成 Router 调用记录。</div>
        </div>
      </section>

      <aside class="panel trace-detail-panel">
        <div class="panel-head">
          <div>
            <h2>Trace详情</h2>
            <p>{{ selectedTrace?.traceNo || '未选择' }}</p>
          </div>
        </div>
        <div v-if="selectedTrace" class="panel-body detail-body">
          <div class="detail-grid">
            <div><span>能力</span><strong>{{ selectedTrace.abilityType }}</strong></div>
            <div><span>模型</span><strong>{{ selectedTrace.modelName || '-' }}</strong></div>
            <div><span>Prompt模板</span><strong>{{ selectedTrace.promptTemplateCode || '-' }}</strong></div>
            <div><span>版本</span><strong>{{ selectedTrace.promptVersion || '-' }}</strong></div>
            <div><span>状态</span><strong>{{ selectedTrace.status }}</strong></div>
            <div><span>耗时</span><strong>{{ selectedTrace.durationMs ?? 0 }} ms</strong></div>
            <div><span>Input Token</span><strong>{{ selectedTrace.tokenInput ?? '' }}</strong></div>
            <div><span>Output Token</span><strong>{{ selectedTrace.tokenOutput ?? '' }}</strong></div>
          </div>

          <div v-if="selectedTrace.errorCode || selectedTrace.errorMessage" class="error-box">
            <strong>{{ selectedTrace.errorCode }}</strong>
            <p>{{ selectedTrace.errorMessage }}</p>
          </div>

          <section class="json-section">
            <h3>Router / 能力输出 JSON</h3>
            <pre>{{ formatJson(selectedTrace.outputJson) }}</pre>
          </section>

          <section class="json-section">
            <h3>输入 JSON</h3>
            <pre>{{ formatJson(selectedTrace.inputJson) }}</pre>
          </section>
        </div>
        <div v-else class="empty">请选择一条 Trace。</div>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.trace-page { display: grid; gap: 16px; }
.trace-layout { display: grid; grid-template-columns: minmax(0, 1fr) 420px; gap: 16px; align-items: start; }
.trace-list-panel, .trace-detail-panel { min-height: calc(100vh - 104px); }
.trace-table-wrap { overflow: auto; }
.trace-row { cursor: pointer; }
.trace-row.active td { background: var(--soft-blue); }
.trace-row strong, .detail-grid strong { display: block; font-size: 13px; }
.muted-line { display: block; color: var(--muted); font-size: 12px; margin-top: 4px; }
.summary-cell { min-width: 220px; max-width: 340px; color: #344054; line-height: 1.5; }
.detail-body { display: grid; gap: 14px; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.detail-grid div { border: 1px solid var(--line); border-radius: 8px; padding: 10px; min-width: 0; }
.detail-grid span { display: block; color: var(--muted); font-size: 12px; margin-bottom: 5px; }
.error-box { border: 1px solid #fecaca; border-radius: 8px; background: var(--soft-red); color: #b42318; padding: 12px; }
.error-box p { margin: 6px 0 0; line-height: 1.5; }
.json-section h3 { margin: 0 0 8px; font-size: 13px; }
.json-section pre { margin: 0; max-height: 340px; overflow: auto; border: 1px solid var(--line); border-radius: 8px; background: #101828; color: #e4e7ec; padding: 12px; font-size: 12px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
@media (max-width: 1200px) {
  .trace-layout { grid-template-columns: 1fr; }
  .trace-list-panel, .trace-detail-panel { min-height: auto; }
}
</style>
