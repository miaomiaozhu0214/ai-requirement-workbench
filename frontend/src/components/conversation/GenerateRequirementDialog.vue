<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { candidateApi } from '../../api/candidateApi';
import { masterDataApi } from '../../api/masterDataApi';
import { priorityOptions, requirementTypeOptions } from '../../constants/options';
import { useUiStore } from '../../stores/ui';
import type { Candidate, GenerateCardRequest, ProductLine, ProductModule, Requirement } from '../../types';
import BaseModal from '../common/BaseModal.vue';

const props = defineProps<{
  open: boolean;
  candidate: Candidate | null;
}>();

const emit = defineEmits<{
  close: [];
  generated: [requirement: Requirement];
}>();

const ui = useUiStore();
const productLines = ref<ProductLine[]>([]);
const modules = ref<ProductModule[]>([]);
const submitting = ref(false);
const errors = reactive<Record<string, string>>({});
const form = reactive<GenerateCardRequest>({
  title: '',
  productLineId: null,
  moduleId: null,
  requirementType: 'optimization',
  priority: 'medium',
});

const contentSummary = computed(() => {
  if (!props.candidate) return '';
  return Object.entries(props.candidate.contentJson || {})
    .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join('、') : String(value)}`)
    .join('\n');
});

watch(
  () => props.open,
  async (open) => {
    if (!open) return;
    form.title = props.candidate?.title || '';
    form.productLineId = null;
    form.moduleId = null;
    form.requirementType = 'optimization';
    form.priority = 'medium';
    Object.keys(errors).forEach((key) => delete errors[key]);
    productLines.value = await masterDataApi.productLines();
  },
);

watch(
  () => form.productLineId,
  async (lineId) => {
    form.moduleId = null;
    modules.value = lineId ? await masterDataApi.modules(lineId) : [];
  },
);

function validate() {
  Object.keys(errors).forEach((key) => delete errors[key]);
  if (!form.title.trim()) errors.title = '需求标题不能为空';
  if (form.title.trim().length > 120 || form.title.trim().length < 2) errors.title = '需求标题需为2-120字符';
  if (!form.productLineId) errors.productLineId = '请选择产品线';
  if (!form.moduleId) errors.moduleId = '请选择模块';
  if (!form.requirementType) errors.requirementType = '请选择需求类型';
  return Object.keys(errors).length === 0;
}

async function submit() {
  if (!props.candidate) return;
  if (!validate()) {
    ui.toast('校验失败', '请补充必填字段后再提交', 'error');
    return;
  }
  submitting.value = true;
  try {
    // 这里是“用户确认生成正式需求”的前端边界；后端会保留来源候选和来源会话。
    const requirement = await candidateApi.generateCard(props.candidate.id, { ...form, title: form.title.trim() });
    ui.toast('需求卡片已生成', `${requirement.requirementNo} 已保存到需求池`, 'success');
    emit('generated', requirement);
  } catch (error) {
    ui.toast('生成失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <BaseModal :open="open" title="生成正式需求卡片" @close="$emit('close')">
    <div class="grid-form">
      <div class="field">
        <label>需求标题 *</label>
        <input v-model="form.title" class="input" maxlength="120" />
        <div class="field-error">{{ errors.title }}</div>
      </div>
      <div class="field">
        <label>产品线 *</label>
        <select v-model.number="form.productLineId" class="select">
          <option :value="null">请选择</option>
          <option v-for="line in productLines" :key="line.id" :value="line.id">{{ line.lineName }}</option>
        </select>
        <div class="field-error">{{ errors.productLineId }}</div>
      </div>
      <div class="field">
        <label>模块 *</label>
        <select v-model.number="form.moduleId" class="select">
          <option :value="null">请选择</option>
          <option v-for="module in modules" :key="module.id" :value="module.id">{{ module.moduleName }}</option>
        </select>
        <div class="field-error">{{ errors.moduleId }}</div>
      </div>
      <div class="field">
        <label>需求类型 *</label>
        <select v-model="form.requirementType" class="select">
          <option v-for="option in requirementTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <div class="field-error">{{ errors.requirementType }}</div>
      </div>
      <div class="field">
        <label>优先级</label>
        <select v-model="form.priority" class="select">
          <option v-for="option in priorityOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </div>
    </div>
    <div class="field summary-field">
      <label>结构化描述</label>
      <textarea class="textarea" readonly :value="contentSummary" />
    </div>
    <template #footer>
      <button class="btn" type="button" @click="$emit('close')">取消</button>
      <button class="btn primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '生成中' : '确认生成' }}</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.summary-field { margin-top: 14px; }
.summary-field .textarea { min-height: 150px; color: #344054; }
</style>
