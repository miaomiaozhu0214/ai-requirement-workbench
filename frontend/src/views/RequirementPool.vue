<script setup lang="ts">
import { MessageSquarePlus, Search } from '@lucide/vue';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { requirementStatusLabels, requirementTypeOptions } from '../constants/options';
import { useUiStore } from '../stores/ui';
import { useWorkbenchStore } from '../stores/workbench';

const store = useWorkbenchStore();
const ui = useUiStore();
const router = useRouter();
const keyword = ref('');
const status = ref('');
const loading = ref(false);
const searchError = ref('');

const filteredRequirements = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return store.requirements.filter((requirement) => {
    const matchKeyword = !kw || requirement.title.toLowerCase().includes(kw) || requirement.requirementNo.toLowerCase().includes(kw);
    const matchStatus = !status.value || requirement.status === status.value;
    return matchKeyword && matchStatus;
  });
});

async function load() {
  loading.value = true;
  try {
    await store.refreshRequirements();
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '请检查后端服务', 'error');
  } finally {
    loading.value = false;
  }
}

function validateSearch() {
  const value = keyword.value.trim();
  if (value && (value.length < 2 || /['";\\]/.test(value))) {
    searchError.value = '搜索关键字至少2个字符，且不能包含引号、分号或反斜杠';
    ui.toast('查询失败', searchError.value, 'error');
    return;
  }
  searchError.value = '';
  ui.toast('查询完成', '已按条件刷新需求池列表', 'success');
}

function typeLabel(type: string) {
  return requirementTypeOptions.find((option) => option.value === type)?.label || type;
}

onMounted(load);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>需求池</h1>
      <p>正式需求卡片列表，所有数据均由候选需求确认生成</p>
    </div>
    <button class="btn primary" type="button" @click="router.push('/conversation')">
      <MessageSquarePlus :size="16" />
      对话收集需求
    </button>
  </header>
  <section class="page-body">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>正式需求卡片</h2>
          <p>默认按更新时间倒序展示</p>
        </div>
        <button class="btn" type="button" :disabled="loading" @click="load">刷新</button>
      </div>
      <div class="panel-body">
        <div class="requirement-toolbar">
          <div class="field search-field">
            <label>关键字</label>
            <input v-model="keyword" class="input" maxlength="80" placeholder="搜索标题或需求编号" />
            <div class="field-error">{{ searchError }}</div>
          </div>
          <div class="field status-field">
            <label>状态</label>
            <select v-model="status" class="select">
              <option value="">全部状态</option>
              <option value="confirmed">已确认</option>
              <option value="prd_generated">已生成PRD</option>
              <option value="prototype_generated">已生成原型</option>
              <option value="closed">已关闭</option>
            </select>
          </div>
          <button class="btn" type="button" @click="validateSearch">
            <Search :size="16" />
            查询
          </button>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>需求编号</th>
                <th>标题</th>
                <th>类型</th>
                <th>完整度</th>
                <th>状态</th>
                <th>来源</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="requirement in filteredRequirements" :key="requirement.id">
                <td>{{ requirement.requirementNo }}</td>
                <td>{{ requirement.title }}</td>
                <td>{{ typeLabel(requirement.requirementType) }}</td>
                <td>
                  <div class="score-cell">
                    <div class="progress">
                      <div class="bar success" :style="{ width: `${Number(requirement.completenessScore)}%` }" />
                    </div>
                    <span>{{ Number(requirement.completenessScore).toFixed(0) }}%</span>
                  </div>
                </td>
                <td><span class="tag green">{{ requirementStatusLabels[requirement.status] || requirement.status }}</span></td>
                <td>会话 {{ requirement.sourceSessionId }} / 候选 {{ requirement.sourceCandidateId }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="filteredRequirements.length === 0" class="empty">暂无正式需求。请先在需求对话中生成需求卡片。</div>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.requirement-toolbar { display: grid; grid-template-columns: minmax(220px, 320px) 180px 96px; gap: 12px; align-items: start; margin-bottom: 14px; }
.search-field, .status-field { min-width: 0; }
.table-wrap { overflow: auto; border: 1px solid var(--line); border-radius: 8px; }
.score-cell { min-width: 150px; display: grid; grid-template-columns: minmax(0, 1fr) 42px; gap: 8px; align-items: center; }
@media (max-width: 760px) {
  .requirement-toolbar { grid-template-columns: 1fr; }
}
</style>
