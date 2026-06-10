<script setup lang="ts">
import { Plus, Trash2 } from '@lucide/vue';
import type { ConversationSummary } from '../../types';

defineProps<{
  sessions: ConversationSummary[];
  activeId?: number;
}>();

defineEmits<{
  create: [];
  select: [id: number];
  delete: [session: ConversationSummary];
}>();
</script>

<template>
  <section class="panel sessions-panel">
    <div class="panel-head">
      <div>
        <h2>会话</h2>
        <p>需求来源强绑定</p>
      </div>
      <button class="btn small primary" type="button" @click="$emit('create')">
        <Plus :size="15" />
        新建
      </button>
    </div>
    <div class="session-list">
      <div
        v-for="session in sessions"
        :key="session.id"
        class="session-item"
        :class="{ active: session.id === activeId }"
      >
        <button class="session-main" type="button" @click="$emit('select', session.id)">
          <strong>{{ session.title }}</strong>
          <span>{{ session.candidateCount }} 个候选 · {{ session.currentStage }}</span>
        </button>
        <button class="icon-btn" type="button" title="删除会话" @click.stop="$emit('delete', session)">
          <Trash2 :size="15" />
        </button>
      </div>
      <div v-if="sessions.length === 0" class="empty">暂无会话，点击新建开始。</div>
    </div>
  </section>
</template>

<style scoped>
.sessions-panel { min-height: 0; display: flex; flex-direction: column; }
.session-list { min-height: 0; overflow: auto; padding: 10px; }
.session-item { width: 100%; border: 1px solid transparent; background: white; border-radius: 8px; text-align: left; margin-bottom: 8px; display: grid; grid-template-columns: minmax(0, 1fr) 32px; align-items: center; gap: 4px; }
.session-item.active { border-color: #bfdbfe; background: var(--soft-blue); }
.session-main { min-width: 0; border: 0; background: transparent; text-align: left; padding: 12px; }
.session-item strong, .session-item span { display: block; }
.session-item strong { font-size: 13px; color: var(--text); }
.session-item span { color: var(--muted); font-size: 12px; margin-top: 5px; }
.icon-btn { width: 28px; height: 28px; border: 0; border-radius: 6px; background: transparent; color: var(--muted); display: inline-grid; place-items: center; }
.icon-btn:hover { background: var(--soft-red); color: var(--danger); }
</style>
