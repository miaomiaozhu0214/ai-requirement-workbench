<script setup lang="ts">
import { RefreshCw, Save } from '@lucide/vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { aiConfigApi } from '../api/aiConfigApi';
import AiModeBanner from '../components/ai/AiModeBanner.vue';
import { useUiStore } from '../stores/ui';
import type { AiAbilityConfig, AiConfigStatus, AiModelConfig, PromptTemplate } from '../types';

const ui = useUiStore();
const abilities = ref<AiAbilityConfig[]>([]);
const models = ref<AiModelConfig[]>([]);
const prompts = ref<PromptTemplate[]>([]);
const configStatus = ref<AiConfigStatus | null>(null);
const loading = ref(false);
const saving = ref(false);
const errors = reactive<Record<string, string>>({});
const form = reactive<AiAbilityConfig>(blankAbility());

const promptOptions = computed(() => prompts.value.filter((prompt) => prompt.abilityType === form.abilityType));

function blankAbility(): AiAbilityConfig {
  return {
    abilityType: 'requirement_extract',
    abilityName: '需求抽取',
    enabled: true,
    modelConfigId: null,
    promptTemplateId: null,
    fallbackToMock: false,
    executionOrder: 100,
    status: 'enabled',
  };
}

async function load() {
  loading.value = true;
  try {
    const [status, abilityList, modelList, promptList] = await Promise.all([
      aiConfigApi.status(),
      aiConfigApi.abilities(),
      aiConfigApi.models(),
      aiConfigApi.prompts(),
    ]);
    configStatus.value = status;
    abilities.value = abilityList;
    models.value = modelList;
    prompts.value = promptList;
    if (!form.id && abilities.value.length > 0) {
      edit(abilities.value[0]);
    }
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

function edit(ability: AiAbilityConfig) {
  Object.assign(form, { ...ability });
  clearErrors();
}

function clearErrors() {
  Object.keys(errors).forEach((key) => delete errors[key]);
}

function validate() {
  clearErrors();
  if (!form.abilityType.trim()) errors.abilityType = '能力类型不能为空';
  if (!form.abilityName.trim()) errors.abilityName = '能力名称不能为空';
  if (!form.modelConfigId) errors.modelConfigId = '请选择模型配置';
  if (!form.promptTemplateId) errors.promptTemplateId = '请选择 Prompt 模板';
  if (form.executionOrder == null || form.executionOrder < 0 || form.executionOrder > 9999) errors.executionOrder = '执行顺序需为0-9999';
  return Object.keys(errors).length === 0;
}

async function save() {
  if (!validate()) {
    ui.toast('校验失败', '请补充必填配置后再保存', 'error');
    return;
  }
  saving.value = true;
  try {
    const saved = await aiConfigApi.saveAbility({ ...form });
    ui.toast('AI能力配置已保存', saved.abilityName, 'success');
    await load();
    edit(saved);
  } catch (error) {
    ui.toast('保存失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    saving.value = false;
  }
}

function modelName(id: number | null) {
  return models.value.find((model) => model.id === id)?.displayName || '未绑定模型';
}

function promptName(id: number | null) {
  const prompt = prompts.value.find((item) => item.id === id);
  return prompt ? `${prompt.templateName} ${prompt.version}` : '未绑定Prompt';
}

onMounted(load);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>AI能力配置</h1>
      <p>为需求识别、抽取、完整度检查、回复生成绑定模型与 Prompt</p>
    </div>
    <button class="btn" type="button" :disabled="loading" @click="load">
      <RefreshCw :size="16" />
      刷新
    </button>
  </header>

  <section class="page-body">
    <AiModeBanner :status="configStatus" :loading="loading" class="mode-section" />
    <div class="ability-layout">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>能力列表</h2>
          <p>{{ abilities.length }} 个能力</p>
        </div>
      </div>
      <div class="ability-list">
        <button
          v-for="ability in abilities"
          :key="ability.id"
          class="config-row"
          :class="{ active: ability.id === form.id }"
          type="button"
          @click="edit(ability)"
        >
          <strong>{{ ability.abilityName }}</strong>
          <span>{{ ability.abilityType }}</span>
          <span>顺序 {{ ability.executionOrder ?? 100 }} · {{ ability.enabled ? '启用' : '停用' }} · {{ modelName(ability.modelConfigId) }}</span>
        </button>
        <div v-if="abilities.length === 0" class="empty">暂无能力配置。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>编辑能力</h2>
          <p>{{ promptName(form.promptTemplateId) }}</p>
        </div>
        <button class="btn primary" type="button" :disabled="saving" @click="save">
          <Save :size="16" />
          {{ saving ? '保存中' : '保存' }}
        </button>
      </div>
      <div class="panel-body grid-form">
        <div class="form-tip">
          数值越小越先执行；顺序相同时，按照 AI Router 返回的原始顺序执行。
        </div>
        <div class="field">
          <label>能力类型 *</label>
          <input v-model="form.abilityType" class="input" maxlength="64" />
          <div class="field-error">{{ errors.abilityType }}</div>
        </div>
        <div class="field">
          <label>能力名称 *</label>
          <input v-model="form.abilityName" class="input" maxlength="120" />
          <div class="field-error">{{ errors.abilityName }}</div>
        </div>
        <div class="field">
          <label>模型配置 *</label>
          <select v-model.number="form.modelConfigId" class="select">
            <option :value="null">请选择</option>
            <option v-for="model in models" :key="model.id" :value="model.id">
              {{ model.displayName }} · {{ model.modelName }}
            </option>
          </select>
          <div class="field-error">{{ errors.modelConfigId }}</div>
        </div>
        <div class="field">
          <label>Prompt 模板 *</label>
          <select v-model.number="form.promptTemplateId" class="select">
            <option :value="null">请选择</option>
            <option v-for="prompt in promptOptions" :key="prompt.id" :value="prompt.id">
              {{ prompt.templateName }} · {{ prompt.version }}
            </option>
          </select>
          <div class="field-error">{{ errors.promptTemplateId }}</div>
        </div>
        <div class="field">
          <label>执行顺序 *</label>
          <input v-model.number="form.executionOrder" class="input" type="number" min="0" max="9999" />
          <div class="field-error">{{ errors.executionOrder }}</div>
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="form.status" class="select">
            <option value="enabled">enabled</option>
            <option value="disabled">disabled</option>
          </select>
          <div class="field-error"></div>
        </div>
        <label class="check-line">
          <input v-model="form.enabled" type="checkbox" />
          启用能力
        </label>
        <label class="check-line">
          <input v-model="form.fallbackToMock" type="checkbox" />
          OpenAI失败时允许回退Mock
        </label>
      </div>
    </section>
    </div>
  </section>
</template>

<style scoped>
.mode-section { margin-bottom: 16px; }
.ability-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; }
.ability-list { padding: 10px; display: grid; gap: 8px; }
.config-row { width: 100%; border: 1px solid var(--line); background: white; border-radius: 8px; padding: 12px; text-align: left; display: grid; gap: 5px; }
.config-row strong { font-size: 13px; }
.config-row span { color: var(--muted); font-size: 12px; }
.config-row.active { border-color: var(--primary); background: var(--soft-blue); }
.form-tip { grid-column: 1 / -1; border: 1px solid var(--line); border-radius: 8px; background: var(--soft-blue); color: #1d4ed8; padding: 10px 12px; font-size: 13px; line-height: 1.5; }
.check-line { display: flex; align-items: center; gap: 8px; min-height: 40px; font-weight: 650; }
@media (max-width: 1100px) { .ability-layout { grid-template-columns: 1fr; } }
</style>
