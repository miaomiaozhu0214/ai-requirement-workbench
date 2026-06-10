<script setup lang="ts">
import { Plus, RefreshCw, Save } from '@lucide/vue';
import { onMounted, reactive, ref } from 'vue';
import { aiConfigApi } from '../api/aiConfigApi';
import AiModeBanner from '../components/ai/AiModeBanner.vue';
import { useUiStore } from '../stores/ui';
import type { AiConfigStatus, AiModelConfig } from '../types';

const ui = useUiStore();
const models = ref<AiModelConfig[]>([]);
const configStatus = ref<AiConfigStatus | null>(null);
const loading = ref(false);
const saving = ref(false);
const errors = reactive<Record<string, string>>({});
const form = reactive<AiModelConfig>(blankModel());

function blankModel(): AiModelConfig {
  return {
    provider: 'openai',
    modelName: 'gpt-4.1-mini',
    displayName: '',
    apiBaseUrl: 'https://api.openai.com/v1',
    apiKeyEnv: 'OPENAI_API_KEY',
    apiKeySecret: '',
    temperature: 0.2,
    maxOutputTokens: 1600,
    timeoutSeconds: 60,
    status: 'enabled',
    isDefault: false,
  };
}

async function load() {
  loading.value = true;
  try {
    const [status, modelList] = await Promise.all([aiConfigApi.status(), aiConfigApi.models()]);
    configStatus.value = status;
    models.value = modelList;
    if (!form.id && models.value.length > 0) {
      edit(models.value[0]);
    }
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

function edit(model: AiModelConfig) {
  Object.assign(form, { ...model });
  clearErrors();
}

function createNew() {
  Object.assign(form, blankModel());
  clearErrors();
}

function clearErrors() {
  Object.keys(errors).forEach((key) => delete errors[key]);
}

function validate() {
  clearErrors();
  if (!form.displayName.trim()) errors.displayName = '显示名称不能为空';
  if (!form.modelName.trim()) errors.modelName = '模型名称不能为空';
  if (!form.apiBaseUrl.trim() || !/^https?:\/\//.test(form.apiBaseUrl)) errors.apiBaseUrl = '请输入合法的 API Base URL';
  // 真实 LLM 默认启用：可以直接保存 Key，也可以填写部署环境中可读取的环境变量名。
  if (!form.apiKeySecret?.trim() && !form.apiKeyEnv.trim()) errors.apiKeySecret = '请填写真实 API Key，或填写环境变量名';
  if (form.temperature < 0 || form.temperature > 2) errors.temperature = '温度范围为 0-2';
  if (form.maxOutputTokens < 128 || form.maxOutputTokens > 16000) errors.maxOutputTokens = '输出 Token 范围为 128-16000';
  if (form.timeoutSeconds < 5 || form.timeoutSeconds > 300) errors.timeoutSeconds = '超时范围为 5-300 秒';
  return Object.keys(errors).length === 0;
}

async function save() {
  if (!validate()) {
    ui.toast('校验失败', '请修正表单字段后再保存', 'error');
    return;
  }
  saving.value = true;
  try {
    const saved = await aiConfigApi.saveModel({ ...form });
    ui.toast('模型配置已保存', saved.displayName, 'success');
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
      <h1>模型配置</h1>
      <p>配置 OpenAI 模型、真实 Key、输出 Token 和超时</p>
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

  <section class="page-body">
    <AiModeBanner :status="configStatus" :loading="loading" class="mode-section" />
    <div class="config-layout">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>模型列表</h2>
          <p>{{ models.length }} 个模型配置</p>
        </div>
      </div>
      <div class="model-list">
        <button
          v-for="model in models"
          :key="model.id"
          class="config-row"
          :class="{ active: model.id === form.id }"
          type="button"
          @click="edit(model)"
        >
          <strong>{{ model.displayName }}</strong>
          <span>{{ model.provider }} · {{ model.modelName }}</span>
          <span>{{ model.status }} · {{ model.isDefault ? '默认' : '非默认' }}</span>
        </button>
        <div v-if="models.length === 0" class="empty">暂无模型配置。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>{{ form.id ? '编辑模型' : '新增模型' }}</h2>
          <p>优先使用直接保存的真实 Key；未填写时使用环境变量名</p>
        </div>
        <button class="btn primary" type="button" :disabled="saving" @click="save">
          <Save :size="16" />
          {{ saving ? '保存中' : '保存' }}
        </button>
      </div>
      <div class="panel-body grid-form">
        <div class="field">
          <label>显示名称 *</label>
          <input v-model="form.displayName" class="input" maxlength="120" />
          <div class="field-error">{{ errors.displayName }}</div>
        </div>
        <div class="field">
          <label>Provider *</label>
          <select v-model="form.provider" class="select">
            <option value="openai">openai</option>
          </select>
          <div class="field-error"></div>
        </div>
        <div class="field">
          <label>模型名称 *</label>
          <input v-model="form.modelName" class="input" maxlength="100" />
          <div class="field-error">{{ errors.modelName }}</div>
        </div>
        <div class="field">
          <label>API Base URL *</label>
          <input v-model="form.apiBaseUrl" class="input" maxlength="255" />
          <div class="field-error">{{ errors.apiBaseUrl }}</div>
        </div>
        <div class="field">
          <label>真实 API Key</label>
          <input v-model="form.apiKeySecret" class="input" maxlength="255" type="password" autocomplete="off" />
          <div class="field-error">{{ errors.apiKeySecret }}</div>
        </div>
        <div class="field">
          <label>环境变量名</label>
          <input v-model="form.apiKeyEnv" class="input" maxlength="100" placeholder="OPENAI_API_KEY" />
          <div class="field-error">{{ errors.apiKeyEnv }}</div>
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="form.status" class="select">
            <option value="enabled">enabled</option>
            <option value="disabled">disabled</option>
          </select>
          <div class="field-error"></div>
        </div>
        <div class="field">
          <label>Temperature *</label>
          <input v-model.number="form.temperature" class="input" type="number" min="0" max="2" step="0.1" />
          <div class="field-error">{{ errors.temperature }}</div>
        </div>
        <div class="field">
          <label>Max Output Tokens *</label>
          <input v-model.number="form.maxOutputTokens" class="input" type="number" min="128" max="16000" />
          <div class="field-error">{{ errors.maxOutputTokens }}</div>
        </div>
        <div class="field">
          <label>Timeout Seconds *</label>
          <input v-model.number="form.timeoutSeconds" class="input" type="number" min="5" max="300" />
          <div class="field-error">{{ errors.timeoutSeconds }}</div>
        </div>
        <label class="check-line">
          <input v-model="form.isDefault" type="checkbox" />
          默认模型
        </label>
      </div>
    </section>
    </div>
  </section>
</template>

<style scoped>
.mode-section { margin-bottom: 16px; }
.config-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; }
.model-list { padding: 10px; display: grid; gap: 8px; }
.config-row { width: 100%; border: 1px solid var(--line); background: white; border-radius: 8px; padding: 12px; text-align: left; display: grid; gap: 5px; }
.config-row strong { font-size: 13px; }
.config-row span { color: var(--muted); font-size: 12px; }
.config-row.active { border-color: var(--primary); background: var(--soft-blue); }
.check-line { display: flex; align-items: center; gap: 8px; min-height: 40px; font-weight: 650; }
@media (max-width: 1100px) { .config-layout { grid-template-columns: 1fr; } }
</style>
