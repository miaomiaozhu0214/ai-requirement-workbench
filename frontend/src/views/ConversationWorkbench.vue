<script setup lang="ts">
import { RefreshCw } from '@lucide/vue';
import { onMounted, ref } from 'vue';
import { candidateApi } from '../api/candidateApi';
import BaseModal from '../components/common/BaseModal.vue';
import CandidatePanel from '../components/conversation/CandidatePanel.vue';
import GenerateRequirementDialog from '../components/conversation/GenerateRequirementDialog.vue';
import MessagePanel from '../components/conversation/MessagePanel.vue';
import RouterDebugPanel from '../components/conversation/RouterDebugPanel.vue';
import SessionList from '../components/conversation/SessionList.vue';
import { useUiStore } from '../stores/ui';
import { useWorkbenchStore } from '../stores/workbench';
import type { Candidate } from '../types';

const store = useWorkbenchStore();
const ui = useUiStore();
const selectedCandidate = ref<Candidate | null>(null);
const generating = ref(false);
const closingCandidate = ref<Candidate | null>(null);
const deletingSession = ref<{ id: number; title: string } | null>(null);
const loading = ref(false);

async function initialize() {
  loading.value = true;
  try {
    await store.loadSessions();
    if (store.sessions.length === 0) {
      await store.createSession();
    }
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function createSession() {
  try {
    await store.createSession();
    ui.toast('新会话已创建', '可以开始描述新的需求', 'success');
  } catch (error) {
    ui.toast('创建失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  }
}

async function selectSession(id: number) {
  try {
    await store.selectSession(id);
  } catch (error) {
    ui.toast('切换失败', error instanceof Error ? error.message : '会话不存在', 'error');
  }
}

async function sendMessage(content: string) {
  try {
    await store.sendMessage(content);
  } catch (error) {
    ui.toast('发送失败', error instanceof Error ? error.message : 'AI服务暂时不可用，请稍后重试', 'error');
  }
}

async function closeCandidate() {
  if (!closingCandidate.value) return;
  try {
    await candidateApi.close(closingCandidate.value.id);
    ui.toast('候选需求已关闭', '该候选需求不会生成正式需求卡片', 'success');
    if (store.activeSession) await store.selectSession(store.activeSession.id);
  } catch (error) {
    ui.toast('关闭失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    closingCandidate.value = null;
  }
}

async function deleteSession() {
  if (!deletingSession.value) return;
  try {
    await store.deleteSession(deletingSession.value.id);
    ui.toast('会话已删除', '测试会话已从列表中移除', 'success');
  } catch (error) {
    ui.toast('删除失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    deletingSession.value = null;
  }
}

function openGenerateDialog(candidate: Candidate) {
  selectedCandidate.value = candidate;
  generating.value = true;
}

async function handleGenerated() {
  generating.value = false;
  selectedCandidate.value = null;
  if (store.activeSession) await store.selectSession(store.activeSession.id);
  await store.refreshRequirements();
}

onMounted(initialize);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>需求对话</h1>
      <p>网页对话到候选需求卡片，再到正式需求卡片入库</p>
    </div>
    <button class="btn" type="button" :disabled="loading" @click="initialize">
      <RefreshCw :size="16" />
      刷新
    </button>
  </header>
  <section class="page-body">
    <div class="chat-layout">
      <SessionList
        :sessions="store.sessions"
        :active-id="store.activeSession?.id"
        @create="createSession"
        @select="selectSession"
        @delete="deletingSession = $event"
      />
      <MessagePanel :messages="store.messages" :sending="store.sending" :has-session="!!store.activeSession" @send="sendMessage" />
      <div class="right-column">
        <RouterDebugPanel :actions="store.aiActions" />
        <CandidatePanel :candidates="store.candidates" @generate="openGenerateDialog" @close="closingCandidate = $event" />
      </div>
    </div>
  </section>

  <GenerateRequirementDialog
    :open="generating"
    :candidate="selectedCandidate"
    @close="generating = false"
    @generated="handleGenerated"
  />

  <BaseModal :open="!!closingCandidate" title="关闭候选需求" @close="closingCandidate = null">
    <p class="confirm-text">确认关闭“{{ closingCandidate?.title }}”？关闭后不会生成正式需求卡片。</p>
    <template #footer>
      <button class="btn" type="button" @click="closingCandidate = null">取消</button>
      <button class="btn danger" type="button" @click="closeCandidate">确认关闭</button>
    </template>
  </BaseModal>

  <BaseModal :open="!!deletingSession" title="删除会话" @close="deletingSession = null">
    <p class="confirm-text">确认删除“{{ deletingSession?.title }}”？会话、消息和候选需求将从列表隐藏，正式需求和 AI Trace 会保留。</p>
    <template #footer>
      <button class="btn" type="button" @click="deletingSession = null">取消</button>
      <button class="btn danger" type="button" @click="deleteSession">确认删除</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.chat-layout {
  height: calc(100vh - 104px);
  display: grid;
  grid-template-columns: 270px minmax(420px, 1fr) 390px;
  gap: 14px;
}
.right-column { min-height: 0; display: grid; grid-template-rows: auto minmax(0, 1fr); gap: 14px; }
.confirm-text { margin: 0; line-height: 1.7; }
@media (max-width: 1180px) {
  .chat-layout { height: auto; grid-template-columns: 1fr; }
  .right-column { min-height: auto; }
}
</style>
