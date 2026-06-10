import { http, unwrap } from './http';
import type { AiAbilityConfig, AiConfigStatus, AiModelConfig, PromptTemplate } from '../types';

export const aiConfigApi = {
  status: () => unwrap<AiConfigStatus>(http.get('/ai-config/status')),
  models: () => unwrap<AiModelConfig[]>(http.get('/ai-config/models')),
  saveModel: (payload: AiModelConfig) => unwrap<AiModelConfig>(http.post('/ai-config/models', payload)),
  prompts: () => unwrap<PromptTemplate[]>(http.get('/ai-config/prompts')),
  savePrompt: (payload: PromptTemplate) => unwrap<PromptTemplate>(http.post('/ai-config/prompts', payload)),
  abilities: () => unwrap<AiAbilityConfig[]>(http.get('/ai-config/abilities')),
  saveAbility: (payload: AiAbilityConfig) => unwrap<AiAbilityConfig>(http.post('/ai-config/abilities', payload)),
};
