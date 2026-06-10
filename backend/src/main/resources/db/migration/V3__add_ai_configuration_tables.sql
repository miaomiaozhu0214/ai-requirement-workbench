CREATE TABLE ai_model_config (
  id BIGINT PRIMARY KEY,
  provider VARCHAR(32) NOT NULL DEFAULT 'openai',
  model_name VARCHAR(100) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  api_base_url VARCHAR(255) NOT NULL DEFAULT 'https://api.openai.com/v1',
  api_key_env VARCHAR(100) NOT NULL DEFAULT 'OPENAI_API_KEY',
  temperature NUMERIC(4,2) NOT NULL DEFAULT 0.20,
  max_output_tokens INTEGER NOT NULL DEFAULT 1600,
  timeout_seconds INTEGER NOT NULL DEFAULT 60,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ai_model_provider ON ai_model_config(provider);
CREATE INDEX idx_ai_model_status ON ai_model_config(status);
CREATE INDEX idx_ai_model_default ON ai_model_config(is_default);

CREATE TABLE prompt_template (
  id BIGINT PRIMARY KEY,
  ability_type VARCHAR(64) NOT NULL,
  template_name VARCHAR(120) NOT NULL,
  version VARCHAR(32) NOT NULL,
  system_prompt TEXT NOT NULL,
  user_prompt TEXT NOT NULL,
  json_schema JSONB NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_prompt_template_version UNIQUE(ability_type, version)
);

CREATE INDEX idx_prompt_ability ON prompt_template(ability_type);
CREATE INDEX idx_prompt_status ON prompt_template(status);
CREATE INDEX idx_prompt_default ON prompt_template(is_default);

CREATE TABLE ai_ability_config (
  id BIGINT PRIMARY KEY,
  ability_type VARCHAR(64) NOT NULL UNIQUE,
  ability_name VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  model_config_id BIGINT,
  prompt_template_id BIGINT,
  fallback_to_mock BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ai_ability_status ON ai_ability_config(status);
CREATE INDEX idx_ai_ability_model ON ai_ability_config(model_config_id);
CREATE INDEX idx_ai_ability_prompt ON ai_ability_config(prompt_template_id);

INSERT INTO ai_model_config (
  id, provider, model_name, display_name, api_base_url, api_key_env,
  temperature, max_output_tokens, timeout_seconds, status, is_default, created_by, updated_by
) VALUES (
  3000000000001, 'openai', 'gpt-4.1-mini', 'OpenAI GPT-4.1 Mini',
  'https://api.openai.com/v1', 'OPENAI_API_KEY', 0.20, 1600, 60, 'enabled', TRUE, 1, 1
);

INSERT INTO prompt_template (
  id, ability_type, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
) VALUES
(
  3000000000101,
  'intent_recognition',
  '需求意图识别默认模板',
  'v1.0',
  '你是 AI需求工作台的需求分析助手。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请识别用户最新消息的需求意图。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：intent 可选 new_requirement、update_requirement、generate_card、chitchat；nextActions 只能包含 extract_requirement、check_completeness、generate_reply。',
  $${"type":"object","required":["intent","confidence","mayContainMultipleRequirements","nextActions","reason"],"properties":{"intent":{"type":"string"},"confidence":{"type":"number"},"mayContainMultipleRequirements":{"type":"boolean"},"nextActions":{"type":"array","items":{"type":"string"}},"reason":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
),
(
  3000000000102,
  'requirement_extract',
  '需求抽取默认模板',
  'v1.0',
  '你是资深产品经理。你要把用户自然语言拆成候选需求 patch。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请从用户最新消息中抽取候选需求。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：如果是新需求，operation=create；如果是在补充已有候选需求，operation=update。fields 放结构化信息，例如 background、businessGoal、scenarios、scope、businessRules、permissions、fields、acceptanceCriteria、exceptionCases。',
  $${"type":"object","required":["patches"],"properties":{"patches":{"type":"array","items":{"type":"object","required":["title","operation","fields","confidence"],"properties":{"title":{"type":"string"},"operation":{"type":"string"},"fields":{"type":"object"},"confidence":{"type":"number"}}}}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
),
(
  3000000000103,
  'completeness_check',
  '完整度检查默认模板',
  'v1.0',
  '你是严谨的需求评审专家。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请评估候选需求完整度。\n\n候选需求：\n{{candidateJson}}\n\n评分 0-100。缺失项应覆盖背景、目标、范围、场景、业务规则、权限、边界、验收标准、风险。readyToGenerateCard 只有在完整度不低于 80 且关键缺失项很少时为 true。',
  $${"type":"object","required":["completenessScore","missingItems","riskyItems","suggestedQuestions","readyToGenerateCard"],"properties":{"completenessScore":{"type":"number"},"missingItems":{"type":"array","items":{"type":"string"}},"riskyItems":{"type":"array","items":{"type":"string"}},"suggestedQuestions":{"type":"array","items":{"type":"string"}},"readyToGenerateCard":{"type":"boolean"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
),
(
  3000000000104,
  'reply_generate',
  '回复生成默认模板',
  'v1.0',
  '你是友好、简洁的 AI需求工作台助手。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请根据用户最新消息和当前候选需求，生成一句面向用户的中文回复。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：说明识别到什么、还建议补充什么，语气专业简洁。',
  $${"type":"object","required":["content"],"properties":{"content":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
);

INSERT INTO ai_ability_config (
  id, ability_type, ability_name, enabled, model_config_id, prompt_template_id,
  fallback_to_mock, status, created_by, updated_by
) VALUES
  (3000000000201, 'intent_recognition', '需求识别', TRUE, 3000000000001, 3000000000101, FALSE, 'enabled', 1, 1),
  (3000000000202, 'requirement_extract', '需求抽取', TRUE, 3000000000001, 3000000000102, FALSE, 'enabled', 1, 1),
  (3000000000203, 'completeness_check', '完整度检查', TRUE, 3000000000001, 3000000000103, FALSE, 'enabled', 1, 1),
  (3000000000204, 'reply_generate', '回复生成', TRUE, 3000000000001, 3000000000104, FALSE, 'enabled', 1, 1);
