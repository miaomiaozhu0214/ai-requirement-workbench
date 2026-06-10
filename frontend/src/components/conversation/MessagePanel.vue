<script setup lang="ts">
import { SendHorizonal } from '@lucide/vue';
import { nextTick, ref, watch } from 'vue';
import type { ConversationMessage } from '../../types';

const props = defineProps<{
  messages: ConversationMessage[];
  sending: boolean;
  hasSession: boolean;
}>();

const emit = defineEmits<{
  send: [content: string];
}>();

const content = ref('');
const error = ref('');
const messageBox = ref<HTMLElement | null>(null);

function submit() {
  const value = content.value.trim();
  if (!value) {
    error.value = '请输入需求内容或命令';
    return;
  }
  if (value.length > 10000) {
    error.value = '消息内容最多10000字符';
    return;
  }
  error.value = '';
  emit('send', value);
  content.value = '';
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight;
  },
);
</script>

<template>
  <section class="panel chat-panel">
    <div class="panel-head">
      <div>
        <h2>需求对话</h2>
        <p>用户消息先落库，再由 AI Router 编排需求能力</p>
      </div>
      <div class="toolbar">
        <button class="btn small" type="button" @click="content = '/生成卡片'">/生成卡片</button>
        <button class="btn small" type="button" @click="content = '导出范围是当前筛选结果，仅运营人员可导出，字段与列表一致。'">补充示例</button>
      </div>
    </div>
    <div ref="messageBox" class="messages">
      <div v-if="messages.length === 0" class="empty">
        <strong>{{ hasSession ? '当前会话还没有消息' : '暂无会话' }}</strong>
        <span>{{ hasSession ? '直接描述一个需求，AI 会先路由识别，再生成候选需求卡片。' : '点击左侧新建会话，或直接在下方输入需求。' }}</span>
      </div>
      <div v-for="message in messages" :key="message.id" class="message" :class="message.role">
        <div>{{ message.content }}</div>
        <span>{{ message.role === 'user' ? '你' : 'AI需求助手' }} · {{ new Date(message.createdAt).toLocaleTimeString() }}</span>
      </div>
    </div>
    <form class="chat-input" @submit.prevent="submit">
      <div class="input-area">
        <textarea v-model="content" class="textarea" placeholder="输入需求、补充规则，或使用 /生成卡片 命令" />
        <div class="field-error">{{ error }}</div>
      </div>
      <button class="btn primary send-btn" type="submit" :disabled="sending">
        <SendHorizonal :size="17" />
        {{ sending ? '发送中' : '发送' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.chat-panel { min-height: 0; display: flex; flex-direction: column; }
.messages { flex: 1; min-height: 0; overflow: auto; padding: 16px; background: #fbfdff; }
.message { max-width: 82%; margin-bottom: 14px; padding: 12px 14px; border-radius: 8px; line-height: 1.55; font-size: 14px; }
.message.user { margin-left: auto; background: var(--primary); color: white; }
.message.assistant, .message.system { border: 1px solid var(--line); background: white; }
.message span { display: block; margin-top: 6px; font-size: 11px; opacity: .72; }
.empty strong, .empty span { display: block; }
.empty strong { margin-bottom: 6px; color: var(--text); }
.chat-input { border-top: 1px solid var(--line); padding: 12px; display: grid; grid-template-columns: minmax(0, 1fr) 92px; gap: 10px; background: white; }
.chat-input .textarea { min-height: 48px; max-height: 120px; }
.input-area { min-width: 0; }
.send-btn { align-self: start; }
</style>
