import { createRouter, createWebHistory } from 'vue-router';
import AiAbilityConfigView from '../views/AiAbilityConfigView.vue';
import AiModelConfigView from '../views/AiModelConfigView.vue';
import AiTraceView from '../views/AiTraceView.vue';
import ConversationWorkbench from '../views/ConversationWorkbench.vue';
import FeatureLibraryView from '../views/FeatureLibraryView.vue';
import PromptTemplateConfigView from '../views/PromptTemplateConfigView.vue';
import ProductLineConfigView from '../views/ProductLineConfigView.vue';
import RequirementPool from '../views/RequirementPool.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/conversation' },
    { path: '/conversation', component: ConversationWorkbench },
    { path: '/requirements', component: RequirementPool },
    { path: '/feature-library', component: FeatureLibraryView },
    { path: '/product-lines', component: ProductLineConfigView },
    { path: '/ai/models', component: AiModelConfigView },
    { path: '/ai/prompts', component: PromptTemplateConfigView },
    { path: '/ai/abilities', component: AiAbilityConfigView },
    { path: '/ai/traces', component: AiTraceView },
  ],
});

export default router;
