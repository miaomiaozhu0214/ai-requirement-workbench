<script setup lang="ts">
import { CheckCircle2, XCircle } from '@lucide/vue';
import { computed } from 'vue';
import { candidateStatusLabels } from '../../constants/options';
import type { Candidate } from '../../types';

const props = defineProps<{
  candidates: Candidate[];
}>();

defineEmits<{
  generate: [candidate: Candidate];
  close: [candidate: Candidate];
}>();

const orderedCandidates = computed(() => props.candidates);

function scoreClass(score: number) {
  if (score >= 80) return 'success';
  if (score >= 55) return 'warn';
  return '';
}

function identified(candidate: Candidate) {
  return Object.entries(candidate.contentJson || {})
    .filter(([key]) => !['title'].includes(key))
    .slice(0, 5)
    .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join('、') : String(value)}`);
}
</script>

<template>
  <section class="panel candidate-panel">
    <div class="panel-head">
      <div>
        <h2>候选需求卡片</h2>
        <p>每轮对话以 patch 增量更新</p>
      </div>
      <span class="tag blue">{{ candidates.length }} 个</span>
    </div>
    <div class="candidate-list">
      <article v-for="candidate in orderedCandidates" :key="candidate.id" class="candidate-card">
        <header>
          <h3>{{ candidate.title }}</h3>
          <span class="tag" :class="{ green: candidate.status === 'ready_to_card' || candidate.status === 'converted', orange: candidate.status === 'refining', red: candidate.status === 'closed' }">
            {{ candidateStatusLabels[candidate.status] || candidate.status }}
          </span>
        </header>
        <div class="score-row">
          <span>完整度</span>
          <div class="progress">
            <div class="bar" :class="scoreClass(Number(candidate.completenessScore))" :style="{ width: `${Number(candidate.completenessScore)}%` }" />
          </div>
          <strong>{{ Number(candidate.completenessScore).toFixed(0) }}%</strong>
        </div>
        <div class="info-block">
          <strong>已识别信息</strong>
          <p v-if="identified(candidate).length === 0">暂无结构化字段</p>
          <ul v-else>
            <li v-for="item in identified(candidate)" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="info-block">
          <strong>缺失信息</strong>
          <p v-if="!candidate.missingItemsJson?.length">暂无明显缺失项</p>
          <ul v-else class="missing-list">
            <li v-for="item in candidate.missingItemsJson" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="info-block">
          <strong>建议问题</strong>
          <p v-if="!candidate.suggestedQuestionsJson?.length">无需追问</p>
          <ul v-else>
            <li v-for="item in candidate.suggestedQuestionsJson" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="toolbar">
          <button class="btn small primary" type="button" :disabled="candidate.status === 'converted' || candidate.status === 'closed'" @click="$emit('generate', candidate)">
            <CheckCircle2 :size="14" />
            生成需求卡片
          </button>
          <button class="btn small danger" type="button" :disabled="candidate.status === 'converted' || candidate.status === 'closed'" @click="$emit('close', candidate)">
            <XCircle :size="14" />
            关闭
          </button>
        </div>
      </article>
      <div v-if="candidates.length === 0" class="empty">暂无候选需求。发送一条需求描述后会在这里生成卡片。</div>
    </div>
  </section>
</template>

<style scoped>
.candidate-panel { min-height: 0; display: flex; flex-direction: column; }
.candidate-list { min-height: 0; overflow: auto; padding: 12px; }
.candidate-card { border: 1px solid var(--line); border-radius: 8px; background: white; padding: 14px; margin-bottom: 12px; }
.candidate-card header { display: flex; justify-content: space-between; align-items: start; gap: 10px; }
.candidate-card h3 { margin: 0; font-size: 15px; }
.score-row { display: grid; grid-template-columns: 54px minmax(0, 1fr) 42px; gap: 8px; align-items: center; margin: 12px 0; font-size: 12px; color: var(--muted); }
.info-block { border-top: 1px solid var(--line); padding-top: 10px; margin-top: 10px; font-size: 13px; }
.info-block strong { display: block; margin-bottom: 6px; }
.info-block p { margin: 0; color: var(--muted); }
.info-block ul { margin: 0; padding-left: 18px; color: #344054; }
.missing-list { color: #92400e !important; }
.toolbar { margin-top: 12px; }
</style>
