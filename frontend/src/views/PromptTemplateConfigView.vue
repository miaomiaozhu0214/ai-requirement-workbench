<script setup lang="ts">
import { Plus, RefreshCw, Save } from '@lucide/vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { aiConfigApi } from '../api/aiConfigApi';
import { useUiStore } from '../stores/ui';
import type { PromptTemplate } from '../types';

const abilityOptions = [
  { value: 'intent_router', label: 'AI路由' },
  { value: 'intent_recognition', label: '历史意图识别' },
  { value: 'requirement_extract', label: '需求抽取' },
  { value: 'requirement_split', label: '需求拆分' },
  { value: 'completeness_check', label: '完整度检查' },
  { value: 'reply_generate', label: '回复生成' },
  { value: 'card_generate', label: '需求卡片生成' },
  { value: 'similar_requirement_search', label: '相似需求搜索' },
];

const ui = useUiStore();
const prompts = ref<PromptTemplate[]>([]);
const loading = ref(false);
const saving = ref(false);
const schemaText = ref('');
const errors = reactive<Record<string, string>>({});
const form = reactive<PromptTemplate>(blankPrompt());

const filteredPrompts = computed(() => prompts.value.filter((item) => item.abilityType === form.abilityType));

function blankPrompt(): PromptTemplate {
  return {
    abilityType: 'requirement_extract',
    templateCode: 'default_requirement_extract',
    templateName: '',
    version: 'v1.0',
    systemPrompt: '',
    userPrompt: '',
    jsonSchema: { type: 'object', required: [], properties: {} },
    status: 'enabled',
    isDefault: false,
  };
}

async function load() {
  loading.value = true;
  try {
    prompts.value = await aiConfigApi.prompts();
    if (!form.id && prompts.value.length > 0) {
      edit(preferredPrompt());
    }
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

function preferredPrompt() {
  // 默认打开 AI Router 模板，便于排查“用户消息是否先经过 intent_router”这一核心链路。
  return prompts.value.find((prompt) => prompt.templateCode === 'default_intent_router')
    || prompts.value.find((prompt) => prompt.abilityType === 'intent_router')
    || prompts.value[0];
}

function edit(prompt: PromptTemplate) {
  Object.assign(form, { ...prompt, jsonSchema: { ...prompt.jsonSchema } });
  schemaText.value = JSON.stringify(prompt.jsonSchema, null, 2);
  clearErrors();
}

function createNew() {
  Object.assign(form, blankPrompt());
  schemaText.value = JSON.stringify(form.jsonSchema, null, 2);
  clearErrors();
}

function clearErrors() {
  Object.keys(errors).forEach((key) => delete errors[key]);
}

function validate() {
  clearErrors();
  if (!form.templateName.trim()) errors.templateName = '模板名称不能为空';
  if (!form.templateCode.trim()) errors.templateCode = '模板编码不能为空';
  if (!form.version.trim()) errors.version = '版本不能为空';
  if (!form.systemPrompt.trim()) errors.systemPrompt = 'System Prompt不能为空';
  if (!form.userPrompt.trim()) errors.userPrompt = 'User Prompt不能为空';
  try {
    const parsed = JSON.parse(schemaText.value);
    if (!parsed || parsed.type !== 'object') {
      errors.jsonSchema = 'JSON Schema 根节点必须是 object';
    }
    form.jsonSchema = parsed;
  } catch {
    errors.jsonSchema = 'JSON Schema 必须是合法 JSON';
  }
  return Object.keys(errors).length === 0;
}

async function save() {
  if (!validate()) {
    ui.toast('校验失败', '请修正 Prompt 模板后再保存', 'error');
    return;
  }
  saving.value = true;
  try {
    const saved = await aiConfigApi.savePrompt({ ...form, jsonSchema: form.jsonSchema });
    ui.toast('Prompt模板已保存', `${saved.abilityType} ${saved.version}`, 'success');
    await load();
    edit(saved);
  } catch (error) {
    ui.toast('保存失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>Prompt 模板</h1>
      <p>需求识别、需求抽取、完整度检查、回复生成均使用可配置 Prompt</p>
    </div>
    <div class="toolbar">
      <button class="btn" type="button" :disabled="loading" @click="load">
        <RefreshCw :size="16" />
        刷新
      </button>
      <button class="btn primary" type="button" @click="createNew">
        <Plus :size="16" />
        新增
      </button>
    </div>
  </header>

  <section class="page-body prompt-layout">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>模板列表</h2>
          <p>{{ filteredPrompts.length }} 个当前能力模板</p>
        </div>
      </div>
      <div class="prompt-filter">
        <select v-model="form.abilityType" class="select">
          <option v-for="option in abilityOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </div>
      <div class="prompt-list">
        <button
          v-for="prompt in filteredPrompts"
          :key="prompt.id"
          class="config-row"
          :class="{ active: prompt.id === form.id }"
          type="button"
          @click="edit(prompt)"
        >
          <strong>{{ prompt.templateName }}</strong>
          <span>{{ prompt.abilityType }} · {{ prompt.templateCode }} · {{ prompt.version }}</span>
          <span>
            {{ prompt.status }} · {{ prompt.isDefault ? '默认' : '非默认' }}
            <b v-if="prompt.templateCode === 'default_intent_router'">AI路由入口模板</b>
          </span>
        </button>
        <div v-if="filteredPrompts.length === 0" class="empty">当前能力暂无模板。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>{{ form.id ? '编辑 Prompt' : '新增 Prompt' }}</h2>
          <p>模板变量：<code v-pre>{{latestMessage}}</code>、<code v-pre>{{currentCandidatesJson}}</code>、<code v-pre>{{candidateJson}}</code></p>
        </div>
        <button class="btn primary" type="button" :disabled="saving" @click="save">
          <Save :size="16" />
          {{ saving ? '保存中' : '保存' }}
        </button>
      </div>

      <div class="panel-body prompt-form">
        <div class="grid-form">
          <div class="field">
            <label>能力类型 *</label>
            <select v-model="form.abilityType" class="select">
              <option v-for="option in abilityOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <div class="field-error"></div>
          </div>
          <div class="field">
            <label>模板名称 *</label>
            <input v-model="form.templateName" class="input" maxlength="120" />
            <div class="field-error">{{ errors.templateName }}</div>
          </div>
          <div class="field">
            <label>模板编码 *</label>
            <input v-model="form.templateCode" class="input" maxlength="100" />
            <div class="field-error">{{ errors.templateCode }}</div>
          </div>
          <div class="field">
            <label>版本 *</label>
            <input v-model="form.version" class="input" maxlength="32" />
            <div class="field-error">{{ errors.version }}</div>
          </div>
          <div class="field">
            <label>状态</label>
            <select v-model="form.status" class="select">
              <option value="enabled">enabled</option>
              <option value="disabled">disabled</option>
            </select>
            <div class="field-error"></div>
          </div>
        </div>

        <label class="check-line">
          <input v-model="form.isDefault" type="checkbox" />
          默认模板
        </label>

        <div class="field">
          <label>System Prompt *</label>
          <textarea v-model="form.systemPrompt" class="textarea prompt-text"></textarea>
          <div class="field-error">{{ errors.systemPrompt }}</div>
        </div>
        <div class="field">
          <label>User Prompt *</label>
          <textarea v-model="form.userPrompt" class="textarea prompt-text large"></textarea>
          <div class="field-error">{{ errors.userPrompt }}</div>
        </div>
        <div class="field">
          <label>JSON Schema *</label>
          <textarea v-model="schemaText" class="textarea schema-text" spellcheck="false"></textarea>
          <div class="field-error">{{ errors.jsonSchema }}</div>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.prompt-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; }
.prompt-filter { padding: 12px 12px 0; }
.prompt-list { padding: 10px; display: grid; gap: 8px; }
.config-row { width: 100%; border: 1px solid var(--line); background: white; border-radius: 8px; padding: 12px; text-align: left; display: grid; gap: 5px; }
.config-row strong { font-size: 13px; }
.config-row span { color: var(--muted); font-size: 12px; }
.config-row b { display: inline-flex; margin-left: 6px; border-radius: 999px; padding: 2px 6px; background: var(--soft-blue); color: #1d4ed8; font-weight: 700; }
.config-row.active { border-color: var(--primary); background: var(--soft-blue); }
.prompt-form { display: grid; gap: 12px; }
.prompt-text { min-height: 110px; }
.prompt-text.large { min-height: 170px; }
.schema-text { min-height: 220px; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; }
.check-line { display: flex; align-items: center; gap: 8px; font-weight: 650; }
@media (max-width: 1100px) { .prompt-layout { grid-template-columns: 1fr; } }
</style>
