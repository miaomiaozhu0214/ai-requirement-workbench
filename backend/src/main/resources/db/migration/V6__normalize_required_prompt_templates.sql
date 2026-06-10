-- 可重复执行的基础 Prompt 初始化。
-- 这些 INSERT 都只在模板不存在时补齐，避免覆盖管理端已经调整过的 Prompt 内容和版本。

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
)
SELECT
  3000000000105,
  'intent_router',
  'default_intent_router',
  '默认AI路由识别模板',
  'v1.0',
  '你是 AI需求工作台的意图路由器。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。你只判断意图、目标对象和下一步能力，不直接生成需求内容，不执行数据库写入。',
  '请基于用户最新输入、会话状态、当前候选需求和最近对话判断本轮意图。\n\n最新输入：\n{{latestMessage}}\n\n会话状态：{{sessionStatus}}\n当前阶段：{{currentStage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n最近对话：\n{{recentMessagesJson}}\n\n必须识别以下 intent：new_requirement、supplement_requirement、modify_requirement、confirm_requirement、reject_requirement、split_requirement、generate_card、query_requirement、small_talk、unclear。\n\nnextActions 只能从以下值选择：extract_requirement、extract_requirement_info、split_requirement、check_completeness、generate_reply、generate_requirement_card、search_similar_requirements。对于普通交流或无法判断，nextActions 只返回 ["generate_reply"]。如果用户输入过短、只有数字、只有符号或缺少业务语义，例如“1”，intent 必须为 unclear，且不要触发 extract_requirement。没有候选需求时不要建议 generate_requirement_card。低置信度时只建议 generate_reply。',
  $${"type":"object","required":["intent","confidence","targetType","targetId","mayContainMultipleRequirements","nextActions","reason"],"properties":{"intent":{"type":"string"},"confidence":{"type":"number"},"targetType":{"type":"string"},"targetId":{"type":["string","null"]},"mayContainMultipleRequirements":{"type":"boolean"},"nextActions":{"type":"array","items":{"type":"string"}},"reason":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM prompt_template WHERE template_code = 'default_intent_router'
);

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
)
SELECT
  3000000000102,
  'requirement_extract',
  'default_requirement_extract',
  '需求抽取默认模板',
  'v1.0',
  '你是资深产品经理。你要把用户自然语言拆成候选需求 patch。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请从用户最新消息中抽取候选需求。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：如果是新需求，operation=create；如果是在补充已有候选需求，operation=update。fields 放结构化信息，例如 background、businessGoal、scenarios、scope、businessRules、permissions、fields、acceptanceCriteria、exceptionCases。',
  $${"type":"object","required":["patches"],"properties":{"patches":{"type":"array","items":{"type":"object","required":["title","operation","fields","confidence"],"properties":{"title":{"type":"string"},"operation":{"type":"string"},"fields":{"type":"object"},"confidence":{"type":"number"}}}}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM prompt_template WHERE template_code = 'default_requirement_extract'
);

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
)
SELECT
  3000000000103,
  'completeness_check',
  'default_completeness_check',
  '完整度检查默认模板',
  'v1.0',
  '你是严谨的需求评审专家。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请评估候选需求完整度。\n\n候选需求：\n{{candidateJson}}\n\n评分 0-100。缺失项应覆盖背景、目标、范围、场景、业务规则、权限、边界、验收标准、风险。readyToGenerateCard 只有在完整度不低于 80 且关键缺失项很少时为 true。',
  $${"type":"object","required":["completenessScore","missingItems","riskyItems","suggestedQuestions","readyToGenerateCard"],"properties":{"completenessScore":{"type":"number"},"missingItems":{"type":"array","items":{"type":"string"}},"riskyItems":{"type":"array","items":{"type":"string"}},"suggestedQuestions":{"type":"array","items":{"type":"string"}},"readyToGenerateCard":{"type":"boolean"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM prompt_template WHERE template_code = 'default_completeness_check'
);

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
)
SELECT
  3000000000104,
  'reply_generate',
  'default_reply_generate',
  '回复生成默认模板',
  'v1.0',
  '你是友好、简洁的 AI需求工作台助手。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请根据用户最新消息和当前候选需求，生成一句面向用户的中文回复。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：说明识别到什么、还建议补充什么，语气专业简洁。',
  $${"type":"object","required":["content"],"properties":{"content":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM prompt_template WHERE template_code = 'default_reply_generate'
);

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
)
SELECT
  3000000000107,
  'card_generate',
  'default_card_generate',
  '需求卡片生成默认模板',
  'v1.0',
  '你是严谨的需求卡片生成助手。你只基于候选需求生成需求卡片草稿，不直接创建数据库正式需求。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请根据候选需求生成正式需求卡片草稿。\n\n候选需求：\n{{candidateJson}}\n\n要求：如果缺少产品线、模块、标题、业务目标、范围、验收标准等关键字段，应放入 missingRequiredFields，并将 readyToCreateRequirement 设为 false。',
  $${"type":"object","required":["title","content","missingRequiredFields","readyToCreateRequirement","reason"],"properties":{"title":{"type":"string"},"content":{"type":"object"},"missingRequiredFields":{"type":"array","items":{"type":"string"}},"readyToCreateRequirement":{"type":"boolean"},"reason":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM prompt_template WHERE template_code = 'default_card_generate'
);

UPDATE prompt_template
SET status = 'enabled',
    is_default = TRUE,
    updated_by = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE template_code IN (
  'default_intent_router',
  'default_requirement_extract',
  'default_completeness_check',
  'default_reply_generate',
  'default_card_generate'
)
  AND deleted = FALSE
  AND (status <> 'enabled' OR is_default IS DISTINCT FROM TRUE);

-- 对话主链路依赖这些能力绑定：Router 先判断意图，再由后端编排抽取、完整度检查、回复和卡片生成。
INSERT INTO ai_ability_config (
  id, ability_type, ability_name, enabled, model_config_id, prompt_template_id,
  fallback_to_mock, status, created_by, updated_by
)
SELECT
  required_ability.ability_id,
  required_ability.ability_type,
  required_ability.ability_name,
  TRUE,
  3000000000001,
  prompt.id,
  FALSE,
  'enabled',
  1,
  1
FROM (
  VALUES
    (3000000000205::BIGINT, 'intent_router', 'AI路由识别', 'default_intent_router'),
    (3000000000202::BIGINT, 'requirement_extract', '需求抽取', 'default_requirement_extract'),
    (3000000000203::BIGINT, 'completeness_check', '完整度检查', 'default_completeness_check'),
    (3000000000204::BIGINT, 'reply_generate', '回复生成', 'default_reply_generate'),
    (3000000000207::BIGINT, 'card_generate', '需求卡片生成', 'default_card_generate')
) AS required_ability(ability_id, ability_type, ability_name, template_code)
JOIN prompt_template prompt ON prompt.template_code = required_ability.template_code
WHERE NOT EXISTS (
  SELECT 1 FROM ai_ability_config ability
  WHERE ability.ability_type = required_ability.ability_type
);

UPDATE ai_ability_config ability
SET prompt_template_id = prompt.id,
    model_config_id = COALESCE(ability.model_config_id, 3000000000001),
    updated_by = 1,
    updated_at = CURRENT_TIMESTAMP
FROM (
  VALUES
    ('intent_router', 'default_intent_router'),
    ('requirement_extract', 'default_requirement_extract'),
    ('completeness_check', 'default_completeness_check'),
    ('reply_generate', 'default_reply_generate'),
    ('card_generate', 'default_card_generate')
) AS required_ability(ability_type, template_code)
JOIN prompt_template prompt ON prompt.template_code = required_ability.template_code
WHERE ability.ability_type = required_ability.ability_type
  AND ability.deleted = FALSE
  AND ability.prompt_template_id IS NULL;
